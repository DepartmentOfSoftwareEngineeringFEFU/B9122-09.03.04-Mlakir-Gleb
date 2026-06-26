import { updateSearchParams } from '../../lib/searchParams.js'

export function getReviewFilterSearchParams(
  searchParams: URLSearchParams,
  key: string,
  value?: string,
) {
  return updateSearchParams(searchParams, {
    [key]: value,
    page: '1',
  })
}

export function getResetReviewFiltersSearchParams() {
  return new URLSearchParams({ page: '1' })
}

export function getReviewPageSearchParams(
  searchParams: URLSearchParams,
  nextPage: number,
) {
  return updateSearchParams(searchParams, {
    page: String(nextPage),
  })
}

export function getClearedUnavailableReviewSourceSearchParams(
  searchParams: URLSearchParams,
) {
  return updateSearchParams(searchParams, {
    sourceId: undefined,
    page: '1',
  })
}
