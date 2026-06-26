import {
  extractApiErrorMessage,
  getApiErrorMessage,
  hasApiErrorStatus,
  isNetworkApiError,
} from '../../lib/apiError'
import type { CollectionJobResponseDto } from '../../types/collection'
import type { SourceType } from '../../types/source'

const ANALYSIS_UNAVAILABLE_MESSAGE =
  'Сервис анализа временно недоступен. Проверьте доступность aura-analysis.'

export function normalizeCollectionJobErrorMessage(message?: string | null) {
  const normalized = message?.trim()
  if (!normalized) {
    return 'Сбор завершился с ошибкой. Подробности не были переданы.'
  }

  const lower = normalized.toLowerCase()

  if (
    lower.includes('analysis-service is unavailable') ||
    lower.includes('connection refused') ||
    lower.includes('failed to establish a new connection')
  ) {
    return ANALYSIS_UNAVAILABLE_MESSAGE
  }

  if (
    lower.includes('timeout') ||
    lower.includes('timed out') ||
    lower.includes('read timed out')
  ) {
    return 'Сервис анализа не ответил вовремя. Попробуйте повторить запуск позже.'
  }

  if (
    lower.includes('parsing failed') ||
    lower.includes('invalid response') ||
    lower.includes('incomplete response') ||
    lower.includes('empty response')
  ) {
    return 'Сервис анализа вернул некорректный или неполный ответ.'
  }

  const httpMatch = normalized.match(/HTTP\s+(\d{3})/i)
  if (httpMatch) {
    return `Сервис анализа вернул ошибку (HTTP ${httpMatch[1]}).`
  }

  return normalized
}

export function getRunCollectionToastMessage(
  result: CollectionJobResponseDto,
  sourceName: string,
) {
  if (result.status === 'SUCCESS') {
    return {
      tone: 'success' as const,
      message: `Сбор для источника «${sourceName}» завершён: добавлено ${result.collectedCount ?? 0} отзывов`,
    }
  }

  if (result.status === 'FAILED') {
    return {
      tone: 'error' as const,
      message: `Сбор для источника «${sourceName}» завершился ошибкой: ${normalizeCollectionJobErrorMessage(result.errorMessage)}`,
    }
  }

  return {
    tone: 'success' as const,
    message: `Сбор для источника «${sourceName}» запущен`,
  }
}

export function getRunCollectionErrorMessage(error: unknown) {
  if (hasApiErrorStatus(error, 403)) {
    return 'Недостаточно прав для запуска сбора.'
  }

  if (isNetworkApiError(error)) {
    return 'Не удалось связаться с aura-core-service. Проверьте, что backend запущен.'
  }

  const apiMessage = extractApiErrorMessage(error)
  if (apiMessage) {
    return normalizeCollectionJobErrorMessage(apiMessage)
  }

  return 'Не удалось запустить сбор.'
}

export function getSourceMutationErrorMessage(
  error: unknown,
  options: {
    action: 'create' | 'update' | 'import'
    sourceType?: SourceType
  },
) {
  if (options.action === 'import') {
    return getApiErrorMessage(error, 'Не удалось импортировать отзывы')
  }

  const isUrlValidationError =
    (options.sourceType === 'OTZOVIK' || options.sourceType === 'VUZOPEDIA') &&
    hasApiErrorStatus(error, 400)

  if (isUrlValidationError) {
    return options.sourceType === 'VUZOPEDIA'
      ? 'Некорректная ссылка на Vuzopedia.'
      : 'Некорректная ссылка на Otzovik.'
  }

  return getApiErrorMessage(
    error,
    options.action === 'create' ? 'Не удалось создать источник' : 'Не удалось обновить источник',
  )
}
