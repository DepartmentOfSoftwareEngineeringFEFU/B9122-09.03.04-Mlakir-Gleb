import { Link } from 'react-router-dom'
import { Badge } from '../../../components/ui/Badge'
import { Card } from '../../../components/ui/Card'
import { formatDateTime, formatPercent, formatSentiment, formatTopic } from '../../../lib/format'
import type { ReviewResponseDto } from '../../../types/review'

export function ReviewAnalysisCard({
  buildKeywordSearchLink,
  review,
}: {
  buildKeywordSearchLink: (keyword: string) => string
  review: ReviewResponseDto
}) {
  return (
    <Card className="p-6">
      <h2 className="text-lg font-semibold text-slate-950">Результат анализа</h2>
      <dl className="mt-5 grid gap-4">
        <MetaRow label="Тональность" value={formatSentiment(review.analysis?.sentiment)} />
        <MetaRow label="Тема" value={formatTopic(review.analysis?.topic)} />
        <MetaRow
          label="Уверенность модели"
          value={formatPercent(review.analysis?.confidence)}
        />
        <MetaRow label="Версия модели" value={review.analysis?.modelVersion || '—'} />
        <MetaRow
          label="Дата анализа"
          value={formatDateTime(review.analysis?.analyzedAt)}
        />
        <MetaRow
          label="Ключевые слова"
          value={
            review.analysis?.keywords?.length ? (
              <div className="flex flex-wrap gap-2">
                {review.analysis.keywords.map((keyword) => (
                  <Link key={keyword} to={buildKeywordSearchLink(keyword)}>
                    <Badge variant="default">{keyword}</Badge>
                  </Link>
                ))}
              </div>
            ) : (
              '—'
            )
          }
        />
      </dl>
    </Card>
  )
}

function MetaRow({
  label,
  value,
}: {
  label: string
  value: React.ReactNode
}) {
  return (
    <div className="grid gap-1 border-b border-slate-100 pb-4 last:border-b-0 last:pb-0">
      <dt className="text-xs uppercase tracking-[0.18em] text-slate-400">{label}</dt>
      <dd className="text-sm font-medium text-slate-700">{value}</dd>
    </div>
  )
}
