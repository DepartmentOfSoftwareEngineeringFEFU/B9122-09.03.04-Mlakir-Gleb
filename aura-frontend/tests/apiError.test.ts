import test from 'node:test'
import assert from 'node:assert/strict'
import { AxiosError } from 'axios'
import { getApiErrorMessage } from '../src/lib/apiError.js'
import {
  getRunCollectionErrorMessage,
  getRunCollectionToastMessage,
  normalizeCollectionJobErrorMessage,
} from '../src/features/sources/errors.js'

test('getApiErrorMessage extracts validation details from problem payloads', () => {
  const error = new AxiosError('Request failed', undefined, undefined, undefined, {
    data: {
      errors: {
        login: ['Логин уже занят'],
      },
    },
    status: 400,
    statusText: 'Bad Request',
    headers: {},
    config: {} as never,
  })

  assert.equal(getApiErrorMessage(error, 'fallback'), 'Логин уже занят')
})

test('getApiErrorMessage falls back for unknown errors', () => {
  assert.equal(getApiErrorMessage(new Error('boom'), 'fallback'), 'fallback')
})

test('normalizeCollectionJobErrorMessage maps analysis unavailable error', () => {
  assert.equal(
    normalizeCollectionJobErrorMessage('Batch analyze request to analysis-service failed: analysis-service is unavailable'),
    'Сервис анализа временно недоступен. Проверьте доступность aura-analysis.',
  )
})

test('getRunCollectionErrorMessage maps network errors', () => {
  const error = new AxiosError('Network Error')
  assert.equal(
    getRunCollectionErrorMessage(error),
    'Не удалось связаться с aura-core-service. Проверьте, что backend запущен.',
  )
})

test('getRunCollectionToastMessage returns error tone for failed job', () => {
  const result = getRunCollectionToastMessage(
    {
      id: 1,
      sourceId: 2,
      sourceName: 'Otzovik',
      status: 'FAILED',
      startedAt: '2026-01-01T00:00:00Z',
      collectedCount: 0,
      errorMessage: 'analysis-service is unavailable',
      triggeredBy: 'demo-admin',
    },
    'Otzovik',
  )

  assert.equal(result.tone, 'error')
  assert.match(result.message, /Сервис анализа временно недоступен/)
})
