import { AxiosError } from 'axios'

interface ProblemDetailLike {
  detail?: string
  message?: string
  title?: string
  errors?: Record<string, string[] | string>
}

function getProblemDetailMessage(data: ProblemDetailLike | string | undefined) {
  if (typeof data === 'string' && data.trim()) {
    return data
  }

  if (data && typeof data === 'object') {
    if (data.detail?.trim()) return data.detail
    if (data.message?.trim()) return data.message
    if (data.title?.trim()) return data.title

    const firstValidationError = data.errors
      ? Object.values(data.errors)
          .flatMap((value) => (Array.isArray(value) ? value : [value]))
          .find((value) => typeof value === 'string' && value.trim())
      : undefined

    if (firstValidationError) {
      return firstValidationError
    }
  }

  return undefined
}

export function hasApiErrorStatus(error: unknown, ...statuses: number[]) {
  return (
    error instanceof AxiosError &&
    Boolean(error.response?.status && statuses.includes(error.response.status))
  )
}

export function isNetworkApiError(error: unknown) {
  return error instanceof AxiosError && !error.response
}

export function extractApiErrorMessage(error: unknown) {
  if (error instanceof AxiosError) {
    const data = error.response?.data as ProblemDetailLike | string | undefined
    return getProblemDetailMessage(data)
  }

  return undefined
}

export function getApiErrorMessage(error: unknown, fallback: string) {
  return extractApiErrorMessage(error) ?? fallback
}
