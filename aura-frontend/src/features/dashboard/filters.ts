import type { DashboardFilters } from '../../types/dashboard'
import type { ReviewSentiment } from '../../types/review'
import {
  getPositiveNumberSearchParam,
  getSearchParam,
} from '../../lib/searchParams'

const dashboardSentimentValues = ['POSITIVE', 'NEUTRAL', 'NEGATIVE'] as const

export interface DashboardFilterState {
  organizationId?: number
  dateFrom: string
  dateTo: string
  sourceId: string
  sentiment: string
}

export function getDashboardFilterState(searchParams: URLSearchParams): DashboardFilterState {
  return {
    organizationId: getPositiveNumberSearchParam(searchParams, 'organizationId'),
    dateFrom: getSearchParam(searchParams, 'from'),
    dateTo: getSearchParam(searchParams, 'to'),
    sourceId: getSearchParam(searchParams, 'sourceId'),
    sentiment: getSearchParam(searchParams, 'sentiment'),
  }
}

export function mapDashboardFilterStateToQuery(
  state: DashboardFilterState,
): DashboardFilters {
  return {
    organizationId: state.organizationId ?? 0,
    from: state.dateFrom || undefined,
    to: state.dateTo || undefined,
    sourceId: state.sourceId ? Number(state.sourceId) : undefined,
    sentiment: getDashboardSentiment(state.sentiment),
  }
}

function getDashboardSentiment(value: string): ReviewSentiment | undefined {
  return dashboardSentimentValues.includes(value as ReviewSentiment)
    ? (value as ReviewSentiment)
    : undefined
}
