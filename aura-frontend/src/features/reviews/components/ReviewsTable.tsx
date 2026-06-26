import { Link } from 'react-router-dom'
import { Badge } from '../../../components/ui/Badge'
import { Table, TableContainer } from '../../../components/ui/Table'
import { formatDateTime, formatReviewStatus, formatSentiment, formatTopic, truncateText } from '../../../lib/format'
import type { ReviewListItemDto } from '../../../types/review'

export function ReviewsTable({
  fromSearch,
  reviews,
}: {
  fromSearch: string
  reviews: ReviewListItemDto[]
}) {
  return (
    <TableContainer className="max-h-[calc(100vh-20rem)]">
      <Table>
        <thead className="bg-slate-50 text-left text-xs uppercase tracking-[0.18em] text-slate-400">
          <tr>
            <th className="px-5 py-4 font-semibold">Отзыв</th>
            <th className="px-5 py-4 font-semibold">Источник</th>
            <th className="px-5 py-4 font-semibold">Автор</th>
            <th className="px-5 py-4 font-semibold">Дата</th>
            <th className="px-5 py-4 font-semibold">Тональность</th>
            <th className="px-5 py-4 font-semibold">Тема</th>
            <th className="px-5 py-4 font-semibold">Рейтинг</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {reviews.map((review) => (
            <tr key={review.id} className="transition hover:bg-slate-50/80">
              <td className="px-5 py-4">
                <div className="space-y-2">
                  <Link
                    to={`/reviews/${review.id}`}
                    state={{ fromSearch }}
                    className="font-semibold text-slate-900 hover:text-blue-700"
                  >
                    {truncateText(review.text, 110)}
                  </Link>
                  <p className="text-sm text-slate-500">
                    Статус: {formatReviewStatus(review.status)}
                  </p>
                </div>
              </td>
              <td className="px-5 py-4 text-slate-600">{review.sourceName}</td>
              <td className="px-5 py-4 text-slate-600">{review.authorName || 'Аноним'}</td>
              <td className="px-5 py-4 text-slate-500">{formatDateTime(review.publishedAt)}</td>
              <td className="px-5 py-4">
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
              </td>
              <td className="px-5 py-4">
                <Badge variant="default">{formatTopic(review.analysis?.topic)}</Badge>
              </td>
              <td className="px-5 py-4 text-slate-600">{review.rating ?? '—'}</td>
            </tr>
          ))}
        </tbody>
      </Table>
    </TableContainer>
  )
}
