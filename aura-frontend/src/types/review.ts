export type ReviewSentiment = 'POSITIVE' | 'NEUTRAL' | 'NEGATIVE'
export type ReviewTopic =
  | 'EDUCATION'
  | 'TEACHERS'
  | 'INFRASTRUCTURE'
  | 'DORMITORY'
  | 'ADMINISTRATION'
  | 'STUDENT_LIFE'
  | 'OTHER'

export type ReviewStatus = 'NEW' | 'ANALYSIS_PENDING' | 'ANALYZED' | 'FAILED_ANALYSIS'

export interface ReviewAnalysisDto {
  sentiment?: ReviewSentiment | null
  topic?: ReviewTopic | null
  keywords?: string[] | null
  confidence?: number | null
  modelVersion?: string | null
  analyzedAt?: string | null
}

export interface ReviewListItemDto {
  id: number
  sourceId: number
  sourceName: string
  externalId?: string | null
  text: string
  authorName?: string | null
  rating?: number | null
  publishedAt?: string | null
  collectedAt?: string | null
  status: ReviewStatus
  analysis?: ReviewAnalysisDto | null
}

export interface ReviewResponseDto extends ReviewListItemDto {
  originalUrl?: string | null
}

export type ReviewSummaryResponseDto = {
  reviewId: number
  summary: string
  generatedAt: string
  modelVersion: string
  cached: boolean
}

export type ReanalyzeReviewsParams = {
  organizationId?: number
  sourceId?: number
  limit?: number
  force?: boolean
}

export type ReanalyzeReviewsResponseDto = {
  requestedCount: number
  reanalyzedCount: number
  failedCount: number
  skippedCount: number
  errorMessage?: string | null
}

export interface KeywordStatDto {
  keyword: string
  count: number
}

export interface PageResponseDto<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}

export interface ReviewFilters {
  organizationId?: number
  sourceId?: number
  sentiment?: ReviewSentiment
  topic?: ReviewTopic
  keyword?: string
  dateFrom?: string
  dateTo?: string
  page?: number
  size?: number
  sort?: string
}
