import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import type { PageResponseDto, ReviewListItemDto } from '../../../types/review'

export function ReviewsPagination({
  pageData,
  onNext,
  onPrevious,
}: {
  pageData: PageResponseDto<ReviewListItemDto>
  onNext: () => void
  onPrevious: () => void
}) {
  return (
    <Card className="flex flex-col gap-4 p-5 sm:flex-row sm:items-center sm:justify-between">
      <div className="text-sm text-slate-600">
        Показано {pageData.content.length} из {pageData.totalElements} отзывов
      </div>
      <div className="flex gap-3">
        <Button variant="ghost" disabled={pageData.page <= 0} onClick={onPrevious}>
          Назад
        </Button>
        <div className="flex items-center rounded-2xl bg-slate-100 px-4 text-sm text-slate-600">
          Страница {pageData.page + 1} из {Math.max(pageData.totalPages, 1)}
        </div>
        <Button variant="ghost" disabled={pageData.last} onClick={onNext}>
          Далее
        </Button>
      </div>
    </Card>
  )
}
