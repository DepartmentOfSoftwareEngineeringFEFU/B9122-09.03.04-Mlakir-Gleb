import { getApiErrorMessage } from '../../lib/apiError'

export function getOrganizationMutationErrorMessage(
  error: unknown,
  action: 'create' | 'update',
) {
  return getApiErrorMessage(
    error,
    action === 'create'
      ? 'Не удалось создать организацию'
      : 'Не удалось обновить организацию',
  )
}
