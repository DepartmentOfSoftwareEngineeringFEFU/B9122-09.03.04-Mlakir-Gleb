import { updateSearchParams } from '../../lib/searchParams.js'

export function getSourceFilterSearchParams(
  searchParams: URLSearchParams,
  key: string,
  value?: string,
) {
  return updateSearchParams(searchParams, {
    [key]: value,
  })
}

export function getResetSourceFiltersSearchParams() {
  return new URLSearchParams()
}

export function getSourcesEmptyStateActionPath(hasActiveFilters: boolean) {
  return hasActiveFilters ? null : '/sources/new'
}
