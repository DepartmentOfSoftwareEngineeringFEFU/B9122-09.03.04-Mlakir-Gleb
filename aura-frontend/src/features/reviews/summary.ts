import { hasApiErrorStatus, isNetworkApiError } from '../../lib/apiError.js'
import type { ReviewSummaryResponseDto } from '../../types/review'

export function buildReviewSummaryErrorMessage(error: unknown) {
  if (hasApiErrorStatus(error, 401, 403)) {
    return 'Недостаточно прав'
  }

  if (hasApiErrorStatus(error, 503) || isNetworkApiError(error)) {
    return 'Не удалось сгенерировать конспект. Попробуйте позже.'
  }

  return 'Не удалось сгенерировать конспект. Попробуйте позже.'
}

export function buildReviewSummaryMeta(summary: ReviewSummaryResponseDto) {
  return {
    cachedLabel: summary.cached ? 'Из кэша' : undefined,
    generatedAt: summary.generatedAt,
    modelVersion: summary.modelVersion,
  }
}
