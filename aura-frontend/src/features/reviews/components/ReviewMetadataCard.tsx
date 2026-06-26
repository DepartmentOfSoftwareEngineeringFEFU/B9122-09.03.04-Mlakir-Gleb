import { Card } from '../../../components/ui/Card'
import { formatDateTime, formatReviewStatus } from '../../../lib/format'
import type { ReviewResponseDto } from '../../../types/review'

export function ReviewMetadataCard({ review }: { review: ReviewResponseDto }) {
  return (
    <Card className="p-6">
      <h2 className="text-lg font-semibold text-slate-950">Метаданные</h2>
      <dl className="mt-5 grid gap-4">
        <MetaRow label="Автор" value={review.authorName || 'Аноним'} />
        <MetaRow label="Дата публикации" value={formatDateTime(review.publishedAt)} />
        <MetaRow label="Дата сбора" value={formatDateTime(review.collectedAt)} />
        <MetaRow label="Рейтинг" value={review.rating?.toString() ?? '—'} />
        <MetaRow label="Статус" value={formatReviewStatus(review.status)} />
        <MetaRow label="Внешний ID" value={review.externalId || '—'} />
        <MetaRow
          label="Исходная ссылка"
          value={
            review.originalUrl ? (
              <a
                href={review.originalUrl}
                target="_blank"
                rel="noreferrer"
                className="text-blue-700 hover:text-blue-800"
              >
                Открыть ссылку
              </a>
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
