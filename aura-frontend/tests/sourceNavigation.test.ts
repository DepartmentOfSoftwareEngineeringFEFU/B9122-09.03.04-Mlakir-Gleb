import test from 'node:test'
import assert from 'node:assert/strict'
import {
  getResetSourceFiltersSearchParams,
  getSourceFilterSearchParams,
  getSourcesEmptyStateActionPath,
} from '../src/features/sources/navigation.js'

test('getSourceFilterSearchParams updates a single source filter', () => {
  const next = getSourceFilterSearchParams(
    new URLSearchParams('organizationId=7&type=OTZOVIK'),
    'scheduleEnabled',
    'true',
  )

  assert.equal(next.toString(), 'organizationId=7&type=OTZOVIK&scheduleEnabled=true')
})

test('getResetSourceFiltersSearchParams clears all source filters', () => {
  assert.equal(getResetSourceFiltersSearchParams().toString(), '')
})

test('getSourcesEmptyStateActionPath returns create path only without active filters', () => {
  assert.equal(getSourcesEmptyStateActionPath(false), '/sources/new')
  assert.equal(getSourcesEmptyStateActionPath(true), null)
})
