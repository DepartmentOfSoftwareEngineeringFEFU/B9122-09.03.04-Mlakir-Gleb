import type { ReviewFilters, ReviewSentiment, ReviewTopic } from '../../types/review'
import {
  getEnumSearchParam,
  getPositiveNumberSearchParam,
  getTrimmedSearchParam,
} from '../../lib/searchParams'

const reviewSentimentValues = ['POSITIVE', 'NEUTRAL', 'NEGATIVE'] as const
const reviewTopicValues = [
  'EDUCATION',
  'TEACHERS',
  'INFRASTRUCTURE',
  'DORMITORY',
  'ADMINISTRATION',
  'STUDENT_LIFE',
  'OTHER',
] as const

export const REVIEW_PAGE_SIZE = 10

export function getReviewsFilters(searchParams: URLSearchParams): ReviewFilters {
  const page = Number(searchParams.get('page') ?? '1')

  return {
    page: Math.max(page - 1, 0),
    size: REVIEW_PAGE_SIZE,
    organizationId: getPositiveNumberSearchParam(searchParams, 'organizationId'),
    sourceId: getPositiveNumberSearchParam(searchParams, 'sourceId'),
    sentiment: getEnumSearchParam(
      searchParams,
      'sentiment',
      reviewSentimentValues,
    ) as ReviewSentiment | undefined,
    topic: getEnumSearchParam(searchParams, 'topic', reviewTopicValues) as
      | ReviewTopic
      | undefined,
    keyword: getTrimmedSearchParam(searchParams, 'keyword'),
    dateFrom: getTrimmedSearchParam(searchParams, 'dateFrom'),
    dateTo: getTrimmedSearchParam(searchParams, 'dateTo'),
    sort: getTrimmedSearchParam(searchParams, 'sort') ?? 'publishedAt,desc',
  }
}
