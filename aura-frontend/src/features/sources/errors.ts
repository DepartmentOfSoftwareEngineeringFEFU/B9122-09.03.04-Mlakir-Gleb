import { getApiErrorMessage, hasApiErrorStatus } from '../../lib/apiError'
import type { SourceType } from '../../types/source'

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
