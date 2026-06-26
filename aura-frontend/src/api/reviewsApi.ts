import { coreClient } from './http'
import { IS_MOCK_DATA_MODE } from '../config/appMode'
import { mockReviewsApi } from '../mocks/mockApi'
import type {
  KeywordStatDto,
  PageResponseDto,
  ReanalyzeReviewsParams,
  ReanalyzeReviewsResponseDto,
  ReviewFilters,
  ReviewListItemDto,
  ReviewResponseDto,
  ReviewSummaryResponseDto,
} from '../types/review'

const realReviewsApi = {
  getReviews: async (filters: ReviewFilters) => {
    const { data } = await coreClient.get<PageResponseDto<ReviewListItemDto>>(
      '/api/reviews',
      {
        params: {
          organizationId: filters.organizationId,
          sourceId: filters.sourceId,
          sentiment: filters.sentiment,
          topic: filters.topic,
          keyword: filters.keyword,
          dateFrom: filters.dateFrom,
          dateTo: filters.dateTo,
          page: filters.page ?? 0,
          size: filters.size ?? 10,
          sort: filters.sort ?? 'publishedAt,desc',
        },
      },
    )
    return data
  },
  getPopularKeywords: async (params?: { organizationId?: number; limit?: number }) => {
    const { data } = await coreClient.get<KeywordStatDto[]>('/api/reviews/keywords/popular', {
      params: {
        organizationId: params?.organizationId,
        limit: params?.limit,
      },
    })
    return data
  },
  getReviewById: async (id: number) => {
    const { data } = await coreClient.get<ReviewResponseDto>(`/api/reviews/${id}`)
    return data
  },
  getReviewSummary: async (reviewId: number, force?: boolean) => {
    const { data } = await coreClient.post<ReviewSummaryResponseDto>(
      `/api/reviews/${reviewId}/summary`,
      undefined,
      {
        params: {
          force: force || undefined,
        },
      },
    )
    return data
  },
  reanalyzeReviews: async (params?: ReanalyzeReviewsParams) => {
    const { data } = await coreClient.post<ReanalyzeReviewsResponseDto>(
      '/api/reviews/reanalyze',
      undefined,
      {
        params: {
          organizationId: params?.organizationId,
          sourceId: params?.sourceId,
          limit: params?.limit,
          force: params?.force,
        },
      },
    )
    return data
  },
}

export const reviewsApi = IS_MOCK_DATA_MODE ? mockReviewsApi : realReviewsApi
