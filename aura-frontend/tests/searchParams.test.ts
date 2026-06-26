import test from 'node:test'
import assert from 'node:assert/strict'
import {
  getBooleanSearchParam,
  getEnumSearchParam,
  getPositiveNumberSearchParam,
  getTrimmedSearchParam,
  updateSearchParams,
} from '../src/lib/searchParams.js'

test('getTrimmedSearchParam returns undefined for blank values', () => {
  const params = new URLSearchParams({ name: '   ' })

  assert.equal(getTrimmedSearchParam(params, 'name'), undefined)
})

test('getPositiveNumberSearchParam keeps only positive finite numbers', () => {
  const params = new URLSearchParams({ organizationId: '7', sourceId: '0' })

  assert.equal(getPositiveNumberSearchParam(params, 'organizationId'), 7)
  assert.equal(getPositiveNumberSearchParam(params, 'sourceId'), undefined)
})

test('getBooleanSearchParam maps string booleans', () => {
  const params = new URLSearchParams({ enabled: 'true', disabled: 'false' })

  assert.equal(getBooleanSearchParam(params, 'enabled'), true)
  assert.equal(getBooleanSearchParam(params, 'disabled'), false)
})

test('getEnumSearchParam rejects unsupported values', () => {
  const params = new URLSearchParams({ sentiment: 'POSITIVE', topic: 'INVALID' })

  assert.equal(
    getEnumSearchParam(params, 'sentiment', ['POSITIVE', 'NEGATIVE'] as const),
    'POSITIVE',
  )
  assert.equal(
    getEnumSearchParam(params, 'topic', ['EDUCATION', 'OTHER'] as const),
    undefined,
  )
})

test('updateSearchParams updates and removes keys in one pass', () => {
  const next = updateSearchParams(new URLSearchParams('page=2&keyword=test'), {
    page: '1',
    keyword: '',
    sentiment: 'POSITIVE',
  })

  assert.equal(next.toString(), 'page=1&sentiment=POSITIVE')
})
