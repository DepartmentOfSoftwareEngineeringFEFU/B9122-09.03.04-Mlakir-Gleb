import test from 'node:test'
import assert from 'node:assert/strict'
import { AxiosError } from 'axios'
import {
  buildOrganizationInsightsErrorMessage,
  buildOrganizationInsightsMeta,
} from '../src/features/organizations/insights.js'

test('buildOrganizationInsightsErrorMessage maps 403 to permission error', () => {
  const error = new AxiosError('Forbidden', undefined, undefined, undefined, {
    data: {},
    status: 403,
    statusText: 'Forbidden',
    headers: {},
    config: {} as never,
  })

  assert.equal(buildOrganizationInsightsErrorMessage(error), 'Недостаточно прав')
})

test('buildOrganizationInsightsErrorMessage maps insufficient-data responses', () => {
  const error = new AxiosError('Bad Request', undefined, undefined, undefined, {
    data: {
      detail: 'Недостаточно проанализированных отзывов',
    },
    status: 400,
    statusText: 'Bad Request',
    headers: {},
    config: {} as never,
  })

  assert.equal(
    buildOrganizationInsightsErrorMessage(error),
    'Недостаточно проанализированных отзывов для отчёта',
  )
})

test('buildOrganizationInsightsMeta exposes cache label and reviews count', () => {
  assert.deepEqual(
    buildOrganizationInsightsMeta({
      organizationId: 1,
      organizationName: 'ДВФУ',
      summary: 'summary',
      strengths: ['s'],
      weaknesses: ['w'],
      recommendations: ['r'],
      generatedAt: '2026-04-28T12:00:00Z',
      modelVersion: 'gemini-1.5-flash',
      cached: true,
      reviewsUsed: 50,
    }),
    {
      cachedLabel: 'Из кэша',
      generatedAt: '2026-04-28T12:00:00Z',
      modelVersion: 'gemini-1.5-flash',
      reviewsUsedLabel: 'Использовано отзывов: 50',
    },
  )
})
