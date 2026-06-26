import type { ReviewSentiment, ReviewTopic } from './review'

export interface DashboardSentimentDto {
  positive: number
  neutral: number
  negative: number
}

export interface DashboardTimelinePointDto {
  month: string
  count: number
}

export interface DashboardCategoryStatDto {
  category: ReviewTopic
  count: number
}

export interface DashboardResponseDto {
  totalReviews: number
  sourcesCount: number
  sentiment: DashboardSentimentDto
  topCategories: DashboardCategoryStatDto[]
  timeline: DashboardTimelinePointDto[]
}

export interface DashboardFilters {
  organizationId: number
  from?: string
  to?: string
  sourceId?: number
  sentiment?: ReviewSentiment
}
