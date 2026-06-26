import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Select } from '../../../components/ui/Select'
import { formatDateTime } from '../../../lib/format'

interface DashboardInsightsCardProps {
  insightsData: {
    summary: string
    strengths: string[]
    weaknesses: string[]
    recommendations: string[]
  } | null
  insightsError: string | null
  insightsLimit: string
  insightsMeta: {
    generatedAt: string
    modelVersion: string
    reviewsUsedLabel: string
    cachedLabel?: string
  } | null
  insightsOpen: boolean
  isAdmin: boolean
  isPending: boolean
  onForce: () => void
  onGenerate: () => void
  onLimitChange: (value: string) => void
}

export function DashboardInsightsCard({
  insightsData,
  insightsError,
  insightsLimit,
  insightsMeta,
  insightsOpen,
  isAdmin,
  isPending,
  onForce,
  onGenerate,
  onLimitChange,
}: DashboardInsightsCardProps) {
  return (
    <Card className="p-6">
      <div className="space-y-4">
        <h2 className="text-lg font-semibold text-slate-950">Аналитический отчёт</h2>
        <div className="flex w-fit flex-wrap items-end gap-3 rounded-3xl border border-slate-200 bg-slate-50 p-3">
          {isAdmin && (
            <div className="w-[180px]">
              <span className="mb-2 block text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">
                Лимит
              </span>
              <Select
                aria-label="Лимит отзывов для аналитического отчёта"
                value={insightsLimit}
                onChange={(event) => onLimitChange(event.target.value)}
                options={[
                  { value: '25', label: '25 отзывов' },
                  { value: '50', label: '50 отзывов' },
                  { value: '100', label: '100 отзывов' },
                ]}
              />
            </div>
          )}
          <Button
            type="button"
            variant="secondary"
            onClick={onGenerate}
            isLoading={isPending}
            className="min-w-[220px]"
          >
            {isPending
              ? 'Формируем отчёт...'
              : insightsData
                ? insightsOpen
                  ? 'Скрыть аналитический отчёт'
                  : 'Показать аналитический отчёт'
                : 'Сформировать аналитический отчёт'}
          </Button>
          {isAdmin && insightsData && (
            <Button
              type="button"
              variant="ghost"
              onClick={onForce}
              disabled={isPending}
              className="min-w-[180px]"
            >
              Обновить отчёт
            </Button>
          )}
        </div>
      </div>

      {insightsError && <p className="mt-4 text-sm text-rose-600">{insightsError}</p>}

      {!insightsOpen && !insightsData && !isPending && !insightsError && (
        <div className="mt-5 rounded-3xl border border-dashed border-slate-200 bg-slate-50/70 p-5">
          <p className="text-sm leading-7 text-slate-600">
            Сформируйте аналитический отчёт, чтобы получить краткую сводку по сильным сторонам,
            проблемам и рекомендациям на основе выбранных отзывов.
          </p>
        </div>
      )}

      {insightsOpen && (
        <div className="mt-5">
          {isPending && !insightsData ? (
            <div className="space-y-3">
              <div className="h-4 w-40 animate-pulse rounded-full bg-slate-200" />
              <div className="h-4 w-full animate-pulse rounded-full bg-slate-200" />
              <div className="h-4 w-5/6 animate-pulse rounded-full bg-slate-200" />
              <div className="h-4 w-2/3 animate-pulse rounded-full bg-slate-200" />
            </div>
          ) : insightsData ? (
            <div className="space-y-5 rounded-3xl border border-slate-200 bg-slate-50 p-5">
              <section className="space-y-2">
                <p className="text-xs uppercase tracking-[0.18em] text-slate-400">Сводка</p>
                <p className="whitespace-pre-wrap text-sm leading-7 text-slate-700">
                  {insightsData.summary}
                </p>
              </section>

              <InsightsList title="Сильные стороны" items={insightsData.strengths} />
              <InsightsList title="Проблемы" items={insightsData.weaknesses} />
              <InsightsList title="Рекомендации" items={insightsData.recommendations} />

              {insightsMeta && (
                <div className="flex flex-wrap gap-x-5 gap-y-2 text-xs text-slate-500">
                  <span>Сгенерировано: {formatDateTime(insightsMeta.generatedAt)}</span>
                  <span>Модель: {insightsMeta.modelVersion}</span>
                  <span>{insightsMeta.reviewsUsedLabel}</span>
                  {insightsMeta.cachedLabel && <span>{insightsMeta.cachedLabel}</span>}
                </div>
              )}
            </div>
          ) : null}
        </div>
      )}
    </Card>
  )
}

function InsightsList({ items, title }: { items: string[]; title: string }) {
  if (!items.length) {
    return null
  }

  return (
    <section className="space-y-2">
      <p className="text-xs uppercase tracking-[0.18em] text-slate-400">{title}</p>
      <ul className="space-y-2">
        {items.map((item) => (
          <li
            key={item}
            className="rounded-2xl border border-slate-200 bg-white px-4 py-3 text-sm text-slate-700"
          >
            {item}
          </li>
        ))}
      </ul>
    </section>
  )
}
