import {
  extractApiErrorMessage,
  hasApiErrorStatus,
  isNetworkApiError,
} from '../../lib/apiError.js'
import type { OrganizationInsightsResponseDto } from '../../types/organization'

export function buildOrganizationInsightsErrorMessage(error: unknown) {
  if (hasApiErrorStatus(error, 403)) {
    return 'Недостаточно прав'
  }

  const message = extractApiErrorMessage(error)?.toLowerCase() ?? ''
  if (
    hasApiErrorStatus(error, 400, 404) ||
    message.includes('недостаточно') ||
    message.includes('not enough')
  ) {
    return 'Недостаточно проанализированных отзывов для отчёта'
  }

  if (hasApiErrorStatus(error, 503) || isNetworkApiError(error)) {
    return 'ИИ-отчёт временно недоступен'
  }

  return 'ИИ-отчёт временно недоступен'
}

export function buildOrganizationInsightsMeta(
  insights: OrganizationInsightsResponseDto,
) {
  return {
    cachedLabel: insights.cached ? 'Из кэша' : undefined,
    generatedAt: insights.generatedAt,
    modelVersion: insights.modelVersion,
    reviewsUsedLabel: `Использовано отзывов: ${insights.reviewsUsed}`,
  }
}
