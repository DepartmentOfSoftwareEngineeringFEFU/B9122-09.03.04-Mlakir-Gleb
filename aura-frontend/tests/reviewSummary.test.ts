import test from 'node:test'
import assert from 'node:assert/strict'
import { AxiosError } from 'axios'
import {
  buildReviewSummaryErrorMessage,
  buildReviewSummaryMeta,
} from '../src/features/reviews/summary.js'

test('buildReviewSummaryErrorMessage maps 401 and 403 to permission error', () => {
  const error401 = new AxiosError('Unauthorized', undefined, undefined, undefined, {
    data: {},
    status: 401,
    statusText: 'Unauthorized',
    headers: {},
    config: {} as never,
  })
  const error403 = new AxiosError('Forbidden', undefined, undefined, undefined, {
    data: {},
    status: 403,
    statusText: 'Forbidden',
    headers: {},
    config: {} as never,
  })

  assert.equal(buildReviewSummaryErrorMessage(error401), 'Недостаточно прав')
  assert.equal(buildReviewSummaryErrorMessage(error403), 'Недостаточно прав')
})

test('buildReviewSummaryErrorMessage maps 503 to summary unavailable message', () => {
  const error = new AxiosError('Unavailable', undefined, undefined, undefined, {
    data: {},
    status: 503,
    statusText: 'Service Unavailable',
    headers: {},
    config: {} as never,
  })

  assert.equal(
    buildReviewSummaryErrorMessage(error),
    'Не удалось сгенерировать конспект. Попробуйте позже.',
  )
})

test('buildReviewSummaryMeta exposes cache label only for cached summaries', () => {
  assert.deepEqual(
    buildReviewSummaryMeta({
      reviewId: 1,
      summary: 'Summary',
      generatedAt: '2026-04-27T12:00:00Z',
      modelVersion: 'deepseek-openrouter-0.1.0',
      cached: true,
    }),
    {
      cachedLabel: 'Из кэша',
      generatedAt: '2026-04-27T12:00:00Z',
      modelVersion: 'deepseek-openrouter-0.1.0',
    },
  )
})
