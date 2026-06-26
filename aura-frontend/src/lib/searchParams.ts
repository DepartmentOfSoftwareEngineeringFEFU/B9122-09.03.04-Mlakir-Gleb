export function getTrimmedSearchParam(
  searchParams: URLSearchParams,
  key: string,
) {
  const value = searchParams.get(key)?.trim()
  return value ? value : undefined
}

export function getSearchParam(
  searchParams: URLSearchParams,
  key: string,
) {
  return searchParams.get(key) ?? ''
}

export function getBooleanSearchParam(
  searchParams: URLSearchParams,
  key: string,
) {
  const value = searchParams.get(key)
  if (value === 'true') return true
  if (value === 'false') return false
  return undefined
}

export function getNumberSearchParam(
  searchParams: URLSearchParams,
  key: string,
) {
  const value = searchParams.get(key)
  if (!value) return undefined

  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : undefined
}

export function getPositiveNumberSearchParam(
  searchParams: URLSearchParams,
  key: string,
) {
  const parsed = getNumberSearchParam(searchParams, key)
  return parsed && parsed > 0 ? parsed : undefined
}

export function getEnumSearchParam<T extends string>(
  searchParams: URLSearchParams,
  key: string,
  allowedValues: readonly T[],
) {
  const value = getTrimmedSearchParam(searchParams, key)
  return value && allowedValues.includes(value as T) ? (value as T) : undefined
}

export function updateSearchParams(
  searchParams: URLSearchParams,
  updates: Record<string, string | undefined | null>,
) {
  const next = new URLSearchParams(searchParams)

  for (const [key, value] of Object.entries(updates)) {
    if (value == null || value === '') {
      next.delete(key)
    } else {
      next.set(key, value)
    }
  }

  return next
}
