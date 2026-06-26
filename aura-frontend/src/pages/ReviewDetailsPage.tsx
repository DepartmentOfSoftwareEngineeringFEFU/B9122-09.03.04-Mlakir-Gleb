import { Link, useLocation, useParams } from 'react-router-dom'
import { ErrorState } from '../components/common/ErrorState'
import { Loader } from '../components/common/Loader'
import { PageHeader } from '../components/common/PageHeader'
import { Button } from '../components/ui/Button'
import { useHasRole } from '../features/auth/access'
import { ReviewAnalysisCard } from '../features/reviews/components/ReviewAnalysisCard'
import { ReviewContentCard } from '../features/reviews/components/ReviewContentCard'
import { ReviewMetadataCard } from '../features/reviews/components/ReviewMetadataCard'
import { useReviewDetailsQuery } from '../features/reviews/queries'
import { useReviewSummaryState } from '../features/reviews/useReviewSummaryState'

export function ReviewDetailsPage() {
  const params = useParams()
  const reviewId = Number(params.id)

  return <ReviewDetailsContent key={reviewId} reviewId={reviewId} />
}

function ReviewDetailsContent({ reviewId }: { reviewId: number }) {
  const location = useLocation()
  const isAdmin = useHasRole('ROLE_ADMIN')
  const reviewQuery = useReviewDetailsQuery(reviewId)
  const summaryState = useReviewSummaryState(reviewId)

  if (reviewQuery.isLoading) {
    return <Loader label="Загрузка деталей отзыва..." />
  }

  if (reviewQuery.isError || !reviewQuery.data) {
    return <ErrorState onRetry={() => void reviewQuery.refetch()} />
  }

  const review = reviewQuery.data
  const backSearch = (location.state as { fromSearch?: string } | null)?.fromSearch
  const backToReviews = backSearch ? `/reviews?${backSearch}` : '/reviews'

  const buildKeywordSearchLink = (keyword: string) => {
    const next = new URLSearchParams(backSearch ?? '')
    next.set('keyword', keyword)
    next.set('page', '1')
    return `/reviews?${next.toString()}`
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title={`Отзыв #${review.id}`}
        description="Детальная карточка отзыва, его метаданных и результата автоматического анализа."
        actions={
          <Link to={backToReviews}>
            <Button variant="ghost">Назад к списку</Button>
          </Link>
        }
      />

      <div className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <ReviewContentCard
          isAdmin={isAdmin}
          isReviewSummaryPending={summaryState.isReviewSummaryPending}
          onForceSummary={summaryState.handleForceSummaryClick}
          onSummaryClick={summaryState.handleSummaryClick}
          review={review}
          summaryData={summaryState.summaryData}
          summaryError={summaryState.summaryError}
          summaryMeta={summaryState.summaryMeta}
          summaryOpen={summaryState.summaryOpen}
        />

        <div className="space-y-6">
          <ReviewMetadataCard review={review} />
          <ReviewAnalysisCard
            buildKeywordSearchLink={buildKeywordSearchLink}
            review={review}
          />
        </div>
      </div>
    </div>
  )
}
