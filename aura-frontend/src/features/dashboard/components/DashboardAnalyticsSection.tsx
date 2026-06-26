import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'
import { StatCard } from '../../../components/common/StatCard'
import { Card } from '../../../components/ui/Card'
import { formatSentiment, formatTopic } from '../../../lib/format'
import type { DashboardResponseDto } from '../../../types/dashboard'

const pieColors = ['#2563eb', '#64748b', '#dc2626']

export function DashboardAnalyticsSection({
  dashboard,
}: {
  dashboard: DashboardResponseDto
}) {
  const sentimentChartData = [
    { name: formatSentiment('POSITIVE'), count: dashboard.sentiment.positive },
    { name: formatSentiment('NEUTRAL'), count: dashboard.sentiment.neutral },
    { name: formatSentiment('NEGATIVE'), count: dashboard.sentiment.negative },
  ].filter((item) => item.count > 0)

  return (
    <>
      <section className="grid gap-4 md:grid-cols-2">
        <StatCard title="Всего отзывов" value={dashboard.totalReviews} />
        <StatCard title="Источников в выборке" value={dashboard.sourcesCount} />
      </section>

      <section className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
        <Card className="p-6">
          <div className="mb-6">
            <h2 className="text-lg font-semibold text-slate-950">Тональность</h2>
          </div>
          <div className="h-80">
            {sentimentChartData.length === 0 ? (
              <div className="flex h-full items-center justify-center text-sm text-slate-500">
                Нет данных для диаграммы.
              </div>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <PieChart>
                  <Pie
                    data={sentimentChartData}
                    dataKey="count"
                    nameKey="name"
                    innerRadius={70}
                    outerRadius={110}
                    paddingAngle={3}
                  >
                    {sentimentChartData.map((item, index) => (
                      <Cell key={item.name} fill={pieColors[index % pieColors.length]} />
                    ))}
                  </Pie>
                  <Tooltip />
                </PieChart>
              </ResponsiveContainer>
            )}
          </div>
        </Card>

        <Card className="p-6">
          <div className="mb-6">
            <h2 className="text-lg font-semibold text-slate-950">Динамика</h2>
          </div>
          <div className="h-80">
            {dashboard.timeline.length === 0 ? (
              <div className="flex h-full items-center justify-center text-sm text-slate-500">
                Нет данных для графика.
              </div>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={dashboard.timeline}>
                  <CartesianGrid stroke="#e2e8f0" vertical={false} />
                  <XAxis dataKey="month" tickLine={false} axisLine={false} />
                  <YAxis tickLine={false} axisLine={false} allowDecimals={false} />
                  <Tooltip cursor={{ fill: '#f8fafc' }} />
                  <Bar dataKey="count" fill="#2563eb" radius={[12, 12, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </Card>
      </section>

      <Card className="p-6">
        <div className="mb-6">
          <h2 className="text-lg font-semibold text-slate-950">Топ категорий</h2>
        </div>
        {dashboard.topCategories.length === 0 ? (
          <p className="text-sm text-slate-500">Категории пока не определены.</p>
        ) : (
          <div className="space-y-3">
            {dashboard.topCategories.map((item) => (
              <div
                key={item.category}
                className="flex items-center justify-between rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3"
              >
                <span className="text-sm font-medium text-slate-800">
                  {formatTopic(item.category)}
                </span>
                <span className="text-sm text-slate-500">{item.count}</span>
              </div>
            ))}
          </div>
        )}
      </Card>
    </>
  )
}
