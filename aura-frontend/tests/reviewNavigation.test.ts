import test from 'node:test'
import assert from 'node:assert/strict'
import {
  getClearedUnavailableReviewSourceSearchParams,
  getResetReviewFiltersSearchParams,
  getReviewFilterSearchParams,
  getReviewPageSearchParams,
} from '../src/features/reviews/navigation.js'

test('getReviewFilterSearchParams resets page when filter changes', () => {
  const next = getReviewFilterSearchParams(
    new URLSearchParams('page=3&keyword=test'),
    'sentiment',
    'POSITIVE',
  )

  assert.equal(next.toString(), 'page=1&keyword=test&sentiment=POSITIVE')
})

test('getResetReviewFiltersSearchParams preserves only first page', () => {
  assert.equal(getResetReviewFiltersSearchParams().toString(), 'page=1')
})

test('getReviewPageSearchParams updates page without touching other filters', () => {
  const next = getReviewPageSearchParams(
    new URLSearchParams('organizationId=7&page=1'),
    4,
  )

  assert.equal(next.toString(), 'organizationId=7&page=4')
})

test('getClearedUnavailableReviewSourceSearchParams drops source and resets page', () => {
  const next = getClearedUnavailableReviewSourceSearchParams(
    new URLSearchParams('sourceId=9&page=3&organizationId=1'),
  )

  assert.equal(next.toString(), 'page=1&organizationId=1')
})
