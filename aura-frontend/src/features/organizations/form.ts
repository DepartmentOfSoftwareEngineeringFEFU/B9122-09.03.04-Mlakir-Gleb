import { z } from 'zod'
import type {
  CreateOrganizationRequestDto,
  OrganizationResponseDto,
  UpdateOrganizationRequestDto,
} from '../../types/organization'

export const createOrganizationSchema = z.object({
  name: z.string().trim().min(1, 'Укажите название организации'),
  shortName: z.string().trim().min(1, 'Укажите краткое название'),
  description: z
    .string()
    .trim()
    .max(2000, 'Описание не должно превышать 2000 символов')
    .optional()
    .or(z.literal('')),
  website: z
    .string()
    .trim()
    .url('Введите корректный URL')
    .optional()
    .or(z.literal('')),
})

const organizationStatusValues = ['true', 'false'] as const

export const updateOrganizationSchema = createOrganizationSchema.extend({
  isActive: z.enum(organizationStatusValues),
})

export type CreateOrganizationFormInput = z.input<typeof createOrganizationSchema>
export type CreateOrganizationFormValues = z.output<typeof createOrganizationSchema>
export type UpdateOrganizationFormInput = z.input<typeof updateOrganizationSchema>
export type UpdateOrganizationFormValues = z.output<typeof updateOrganizationSchema>

export const defaultCreateOrganizationValues: CreateOrganizationFormInput = {
  name: '',
  shortName: '',
  description: '',
  website: '',
}

export function getUpdateOrganizationFormValues(
  organization: OrganizationResponseDto,
): UpdateOrganizationFormInput {
  return {
    name: organization.name,
    shortName: organization.shortName,
    description: organization.description ?? '',
    website: organization.website ?? '',
    isActive: organization.isActive ? 'true' : 'false',
  }
}

function normalizeOptionalText(value?: string) {
  const trimmed = value?.trim()
  return trimmed ? trimmed : undefined
}

export function mapCreateOrganizationFormToDto(
  values: CreateOrganizationFormValues,
): CreateOrganizationRequestDto {
  return {
    name: values.name.trim(),
    shortName: values.shortName.trim(),
    description: normalizeOptionalText(values.description),
    website: normalizeOptionalText(values.website),
  }
}

export function mapUpdateOrganizationFormToDto(
  values: UpdateOrganizationFormValues,
): UpdateOrganizationRequestDto {
  return {
    name: values.name.trim(),
    shortName: values.shortName.trim(),
    description: normalizeOptionalText(values.description),
    website: normalizeOptionalText(values.website),
    isActive: values.isActive === 'true',
  }
}
