import { z } from 'zod'
import type {
  CollectionMode,
  CreateSourceRequestDto,
  SourceResponseDto,
  SourceType,
  UpdateSourceRequestDto,
} from '../../types/source'
import { DEFAULT_SCHEDULE_INTERVAL_MINUTES } from './schedule.js'
const TABITURIENT_URL_PREFIXES = [
  'https://tabiturient.ru/vuzu/',
  'http://tabiturient.ru/vuzu/',
] as const
const OTZOVIK_URL_PREFIXES = [
  'https://otzovik.com/reviews/',
  'http://otzovik.com/reviews/',
] as const
const VUZOPEDIA_URL_PREFIXES = [
  'https://vuzopedia.ru/vuz/',
  'http://vuzopedia.ru/vuz/',
] as const

const sourceTypeValues = ['TABITURIENT', 'OTZOVIK', 'VUZOPEDIA'] as const satisfies readonly SourceType[]

const collectionModeValues = ['MANUAL', 'SCHEDULED'] as const satisfies readonly CollectionMode[]
const scheduleIntervalMin = 15
const scheduleIntervalMax = 43_200
const scheduleIntervalSchema = z.preprocess(
  (value) => {
    if (value === '' || value == null) return null
    return Number(value)
  },
  z.number().int().nullable().optional(),
)

export const createSourceSchema = z
  .object({
    organizationId: z.coerce
      .number({ error: 'Выберите организацию' })
      .int('Выберите организацию')
      .positive('Выберите организацию'),
    name: z.string().trim().min(1, 'Укажите название источника'),
    type: z.enum(sourceTypeValues),
    baseUrl: z.string().trim().min(1, 'Укажите base URL'),
    collectionMode: z.enum(collectionModeValues).optional(),
    scheduleEnabled: z.boolean(),
    scheduleIntervalMinutes: scheduleIntervalSchema,
    description: z
      .string()
      .trim()
      .max(2000, 'Описание не должно превышать 2000 символов')
      .optional()
      .or(z.literal('')),
  })
  .superRefine((values, context) => {
    if (values.scheduleEnabled) {
      if (values.scheduleIntervalMinutes == null || Number.isNaN(values.scheduleIntervalMinutes)) {
        context.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['scheduleIntervalMinutes'],
          message: 'Укажите интервал сбора',
        })
      } else if (values.scheduleIntervalMinutes < scheduleIntervalMin) {
        context.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['scheduleIntervalMinutes'],
          message: 'Минимальный интервал сбора — 15 минут',
        })
      } else if (values.scheduleIntervalMinutes > scheduleIntervalMax) {
        context.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['scheduleIntervalMinutes'],
          message: 'Максимальный интервал сбора — 43200 минут',
        })
      }
    }

    const baseUrl = values.baseUrl.trim()

    if (values.type === 'TABITURIENT') {
      const isValidPrefix = TABITURIENT_URL_PREFIXES.some((prefix) =>
        baseUrl.startsWith(prefix),
      )

      if (!isValidPrefix) {
        context.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['baseUrl'],
          message:
            'Для Tabiturient укажите ссылку вида https://tabiturient.ru/vuzu/dvfu/',
        })
        return
      }

      const urlResult = z.string().url().safeParse(baseUrl)
      if (!urlResult.success) {
        context.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['baseUrl'],
          message: 'Введите корректный URL страницы вуза на Tabiturient',
        })
      }

      return
    }

    if (values.type === 'OTZOVIK') {
      const normalizedBaseUrl = normalizeSourceBaseUrl(values.type, baseUrl)
      const isValidPrefix = OTZOVIK_URL_PREFIXES.some((prefix) =>
        normalizedBaseUrl.startsWith(prefix),
      )
      const urlResult = z.string().url().safeParse(normalizedBaseUrl)
      const hasReviewsPath = normalizedBaseUrl.includes('/reviews/')

      if (!isValidPrefix || !urlResult.success || !hasReviewsPath) {
        context.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['baseUrl'],
          message: 'Укажите корректную ссылку на страницу отзывов Otzovik.',
        })
      }

      return
    }

    if (values.type === 'VUZOPEDIA') {
      const isValidPrefix = VUZOPEDIA_URL_PREFIXES.some((prefix) =>
        baseUrl.startsWith(prefix),
      )
      const urlResult = z.string().url().safeParse(baseUrl)
      const hasValidPath = /^https?:\/\/vuzopedia\.ru\/vuz\/\d+\/otziv\/?$/.test(baseUrl)

      if (!isValidPrefix || !urlResult.success || !hasValidPath) {
        context.addIssue({
          code: z.ZodIssueCode.custom,
          path: ['baseUrl'],
          message: 'Укажите корректную ссылку на страницу отзывов Vuzopedia.',
        })
      }
    }
  })

const sourceStatusValues = ['true', 'false'] as const

export const updateSourceSchema = createSourceSchema.extend({
  isActive: z.enum(sourceStatusValues),
})

export type CreateSourceFormInput = z.input<typeof createSourceSchema>
export type CreateSourceFormValues = z.output<typeof createSourceSchema>
export type UpdateSourceFormInput = z.input<typeof updateSourceSchema>
export type UpdateSourceFormValues = z.output<typeof updateSourceSchema>

export const defaultCreateSourceValues: CreateSourceFormInput = {
  organizationId: '',
  name: '',
  type: 'TABITURIENT',
  baseUrl: '',
  collectionMode: 'MANUAL',
  scheduleEnabled: false,
  scheduleIntervalMinutes: null,
  description: '',
}

export function getUpdateSourceFormValues(
  source: SourceResponseDto,
): UpdateSourceFormInput {
  return {
    organizationId: source.organization.id,
    name: source.name,
    type: source.type,
    baseUrl: source.baseUrl,
    collectionMode: source.collectionMode,
    scheduleEnabled: source.scheduleEnabled,
    scheduleIntervalMinutes: source.scheduleEnabled
      ? source.scheduleIntervalMinutes ?? DEFAULT_SCHEDULE_INTERVAL_MINUTES
      : null,
    description: source.description ?? '',
    isActive: source.isActive ? 'true' : 'false',
  }
}

function normalizeOptionalText(value?: string) {
  const trimmed = value?.trim()
  return trimmed ? trimmed : undefined
}

function normalizeSourceBaseUrl(type: SourceType, value: string) {
  const trimmed = value.trim()

  if (type === 'OTZOVIK' && trimmed.startsWith('http://otzovik.com/')) {
    return `https://${trimmed.slice('http://'.length)}`
  }

  return trimmed
}

export function getSourceUrlHelpText(type: SourceType) {
  switch (type) {
    case 'TABITURIENT':
      return 'Укажите URL страницы вуза на Tabiturient, например https://tabiturient.ru/vuzu/dvfu/'
    case 'OTZOVIK':
      return 'Укажите ссылку на страницу отзывов организации на otzovik.com. Например: https://otzovik.com/reviews/dalnevostochniy_federalniy_universitet_dvfu/'
    case 'VUZOPEDIA':
      return 'Укажите ссылку на страницу отзывов вуза на vuzopedia.ru. Например: https://vuzopedia.ru/vuz/3281/otziv'
    default:
      return ''
  }
}

export function mapCreateSourceFormToDto(
  values: CreateSourceFormValues,
): CreateSourceRequestDto {
  return {
    organizationId: values.organizationId,
    name: values.name.trim(),
    type: values.type,
    baseUrl: normalizeSourceBaseUrl(values.type, values.baseUrl),
    collectionMode: values.collectionMode ?? 'MANUAL',
    scheduleEnabled: values.scheduleEnabled,
    scheduleIntervalMinutes: values.scheduleEnabled
      ? values.scheduleIntervalMinutes ?? DEFAULT_SCHEDULE_INTERVAL_MINUTES
      : null,
    description: normalizeOptionalText(values.description),
  }
}

export function mapUpdateSourceFormToDto(
  values: UpdateSourceFormValues,
): UpdateSourceRequestDto {
  return {
    organizationId: values.organizationId,
    name: values.name.trim(),
    baseUrl: normalizeSourceBaseUrl(values.type, values.baseUrl),
    isActive: values.isActive === 'true',
    collectionMode: values.collectionMode ?? 'MANUAL',
    scheduleEnabled: values.scheduleEnabled,
    scheduleIntervalMinutes: values.scheduleEnabled
      ? values.scheduleIntervalMinutes ?? DEFAULT_SCHEDULE_INTERVAL_MINUTES
      : null,
    description: normalizeOptionalText(values.description),
  }
}
