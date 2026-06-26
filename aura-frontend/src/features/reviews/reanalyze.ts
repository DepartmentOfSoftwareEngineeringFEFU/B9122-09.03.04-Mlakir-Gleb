import { z } from 'zod'
import {
  getApiErrorMessage,
  hasApiErrorStatus,
  isNetworkApiError,
} from '../../lib/apiError.js'
import type {
  ReanalyzeReviewsParams,
  ReanalyzeReviewsResponseDto,
} from '../../types/review'

export const DEFAULT_REANALYZE_LIMIT = 100
export const MIN_REANALYZE_LIMIT = 1
export const MAX_REANALYZE_LIMIT = 1000

function optionalPositiveNumber(message: string) {
  return z.preprocess(
    (value) => (value === '' || value == null ? undefined : value),
    z.coerce.number().int(message).positive(message).optional(),
  )
}

export const reanalyzeReviewsSchema = z.object({
  organizationId: optionalPositiveNumber('Выберите организацию'),
  sourceId: optionalPositiveNumber('Выберите источник'),
  limit: z.coerce
    .number()
    .int('Введите целое число')
    .min(MIN_REANALYZE_LIMIT, `Минимум ${MIN_REANALYZE_LIMIT}`)
    .max(MAX_REANALYZE_LIMIT, `Максимум ${MAX_REANALYZE_LIMIT}`),
  force: z.boolean(),
})

export type ReanalyzeReviewsFormInput = z.input<typeof reanalyzeReviewsSchema>
export type ReanalyzeReviewsFormValues = z.output<typeof reanalyzeReviewsSchema>

export function getDefaultReanalyzeValues(
  organizationId?: number,
): ReanalyzeReviewsFormInput {
  return {
    organizationId,
    sourceId: undefined,
    limit: DEFAULT_REANALYZE_LIMIT,
    force: false,
  }
}

export function mapReanalyzeFormToParams(
  values: ReanalyzeReviewsFormValues,
): ReanalyzeReviewsParams {
  return {
    organizationId: values.organizationId,
    sourceId: values.sourceId,
    limit: values.limit,
    force: values.force || undefined,
  }
}

export function buildReanalyzeSuccessMessage(
  result: ReanalyzeReviewsResponseDto,
) {
  return `Повторный анализ завершён: успешно ${result.reanalyzedCount}, ошибок ${result.failedCount}, пропущено ${result.skippedCount}`
}

export function buildReanalyzeWarningMessage(
  result: ReanalyzeReviewsResponseDto,
) {
  return `Повторный анализ выполнен частично: ${result.errorMessage}`
}

export function getReanalyzeErrorMessage(error: unknown) {
  if (hasApiErrorStatus(error, 403)) {
    return 'Недостаточно прав'
  }

  if (isNetworkApiError(error)) {
    return 'Не удалось связаться с backend. Проверьте доступность aura-core-service.'
  }

  return getApiErrorMessage(error, 'Не удалось запустить повторный анализ')
}
