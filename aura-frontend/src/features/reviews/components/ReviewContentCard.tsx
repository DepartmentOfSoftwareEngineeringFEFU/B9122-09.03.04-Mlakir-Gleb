import { Badge } from '../../../components/ui/Badge'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { formatDateTime, formatSentiment, formatTopic } from '../../../lib/format'
import type { ReviewResponseDto } from '../../../types/review'

interface ReviewContentCardProps {
  isAdmin: boolean
  isReviewSummaryPending: boolean
  onForceSummary: () => void
  onSummaryClick: () => void
  review: ReviewResponseDto
  summaryData: { summary: string } | null
  summaryError: string | null
  summaryMeta: {
    generatedAt: string
    modelVersion: string
    cachedLabel?: string
  } | null
  summaryOpen: boolean
}

export function ReviewContentCard({
  isAdmin,
  isReviewSummaryPending,
  onForceSummary,
  onSummaryClick,
  review,
  summaryData,
  summaryError,
  summaryMeta,
  summaryOpen,
}: ReviewContentCardProps) {
  return (
    <Card className="p-6">
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <Badge variant="default">{review.sourceName}</Badge>
        <Badge
          variant={
            review.analysis?.sentiment === 'POSITIVE'
              ? 'positive'
              : review.analysis?.sentiment === 'NEGATIVE'
                ? 'negative'
                : 'neutral'
          }
        >
          {formatSentiment(review.analysis?.sentiment)}
        </Badge>
        <Badge variant="default">{formatTopic(review.analysis?.topic)}</Badge>
      </div>
      <div className="space-y-4">
        <div className="flex flex-wrap gap-3">
          <Button
            type="button"
            variant="secondary"
            onClick={onSummaryClick}
            isLoading={isReviewSummaryPending}
          >
            {isReviewSummaryPending ? 'Генерируем...' : 'Конспект от ИИ'}
          </Button>
          {isAdmin && summaryData && (
            <Button
              type="button"
              variant="ghost"
              onClick={onForceSummary}
              disabled={isReviewSummaryPending}
            >
              Сгенерировать заново
            </Button>
          )}
        </div>

        {summaryError && <p className="text-sm text-rose-600">{summaryError}</p>}

        {summaryOpen && (
          <Card className="bg-slate-50 p-5">
            {isReviewSummaryPending && !summaryData ? (
              <div className="space-y-3">
                <div className="h-4 w-40 animate-pulse rounded-full bg-slate-200" />
                <div className="h-4 w-full animate-pulse rounded-full bg-slate-200" />
                <div className="h-4 w-5/6 animate-pulse rounded-full bg-slate-200" />
              </div>
            ) : summaryData ? (
              <div className="space-y-4">
                <div className="space-y-2">
                  <p className="text-xs uppercase tracking-[0.18em] text-slate-400">
                    Конспект
                  </p>
                  <p className="whitespace-pre-wrap text-sm leading-7 text-slate-700">
                    {summaryData.summary}
                  </p>
                </div>
                {summaryMeta && (
                  <div className="flex flex-wrap gap-x-5 gap-y-2 text-xs text-slate-500">
                    <span>Сгенерировано: {formatDateTime(summaryMeta.generatedAt)}</span>
                    <span>Модель: {summaryMeta.modelVersion}</span>
                    {summaryMeta.cachedLabel && <span>{summaryMeta.cachedLabel}</span>}
                  </div>
                )}
              </div>
            ) : null}
          </Card>
        )}
      </div>

      <div className="mt-6">
        <h2 className="text-lg font-semibold text-slate-950">Текст отзыва</h2>
        <p className="mt-4 whitespace-pre-wrap text-sm leading-7 text-slate-700">
          {review.text}
        </p>
      </div>
    </Card>
  )
}
