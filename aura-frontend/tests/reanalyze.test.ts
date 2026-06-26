import test from 'node:test'
import assert from 'node:assert/strict'
import { AxiosError } from 'axios'
import {
  buildReanalyzeSuccessMessage,
  buildReanalyzeWarningMessage,
  getReanalyzeErrorMessage,
  mapReanalyzeFormToParams,
} from '../src/features/reviews/reanalyze.js'

test('mapReanalyzeFormToParams keeps query params compact', () => {
  assert.deepEqual(
    mapReanalyzeFormToParams({
      organizationId: 7,
      sourceId: undefined,
      limit: 100,
      force: false,
    }),
    {
      organizationId: 7,
      sourceId: undefined,
      limit: 100,
      force: undefined,
    },
  )
})

test('buildReanalyzeSuccessMessage renders summary numbers', () => {
  assert.equal(
    buildReanalyzeSuccessMessage({
      requestedCount: 100,
      reanalyzedCount: 85,
      failedCount: 15,
      skippedCount: 0,
      errorMessage: null,
    }),
    'Повторный анализ завершён: успешно 85, ошибок 15, пропущено 0',
  )
})

test('buildReanalyzeWarningMessage renders backend warning text', () => {
  assert.equal(
    buildReanalyzeWarningMessage({
      requestedCount: 100,
      reanalyzedCount: 85,
      failedCount: 15,
      skippedCount: 0,
      errorMessage: 'analysis-service unavailable',
    }),
    'Повторный анализ выполнен частично: analysis-service unavailable',
  )
})

test('getReanalyzeErrorMessage maps 403 to permission error', () => {
  const error = new AxiosError('Forbidden', undefined, undefined, undefined, {
    data: {},
    status: 403,
    statusText: 'Forbidden',
    headers: {},
    config: {} as never,
  })

  assert.equal(getReanalyzeErrorMessage(error), 'Недостаточно прав')
})
