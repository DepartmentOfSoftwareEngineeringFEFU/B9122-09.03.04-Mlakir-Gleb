import { Badge } from '../../../components/ui/Badge'
import { Card } from '../../../components/ui/Card'
import { formatCollectionJobStatus, formatDateTime } from '../../../lib/format'
import type { CollectionJobResponseDto } from '../../../types/collection'

interface CollectionJobsCardProps {
  jobs: CollectionJobResponseDto[]
  isError: boolean
  isLoading: boolean
}

export function CollectionJobsCard({
  jobs,
  isError,
  isLoading,
}: CollectionJobsCardProps) {
  return (
    <Card className="p-6">
      <div className="mb-4 flex items-center justify-between gap-4">
        <div>
          <h2 className="text-lg font-semibold text-slate-950">Последние задания сбора</h2>
          <p className="mt-1 text-sm text-slate-600">
            История последних запусков автоматического сбора отзывов.
          </p>
        </div>
      </div>

      {isLoading ? (
        <p className="text-sm text-slate-500">Загрузка заданий...</p>
      ) : isError ? (
        <p className="text-sm text-rose-600">Не удалось загрузить задания сбора.</p>
      ) : jobs.length === 0 ? (
        <p className="text-sm text-slate-500">Запусков сбора пока не было.</p>
      ) : (
        <div className="max-h-80 space-y-3 overflow-y-auto pr-1">
          {jobs.map((job) => (
            <div
              key={job.id}
              className="flex flex-col gap-3 rounded-2xl border border-slate-200 bg-slate-50 p-4 lg:flex-row lg:items-center lg:justify-between"
            >
              <div>
                <p className="font-semibold text-slate-900">{job.sourceName}</p>
                <p className="text-sm text-slate-500">Запуск: {formatDateTime(job.startedAt)}</p>
              </div>
              <div className="flex items-center gap-3">
                <Badge
                  variant={
                    job.status === 'SUCCESS'
                      ? 'positive'
                      : job.status === 'FAILED'
                        ? 'negative'
                        : 'warning'
                  }
                >
                  {formatCollectionJobStatus(job.status)}
                </Badge>
                <span className="text-sm text-slate-500">{job.collectedCount ?? 0} отзывов</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </Card>
  )
}
