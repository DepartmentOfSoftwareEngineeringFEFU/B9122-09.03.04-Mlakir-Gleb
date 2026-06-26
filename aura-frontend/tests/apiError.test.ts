import test from 'node:test'
import assert from 'node:assert/strict'
import { AxiosError } from 'axios'
import { getApiErrorMessage } from '../src/lib/apiError.js'

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
