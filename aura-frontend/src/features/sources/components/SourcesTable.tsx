import { Badge } from '../../../components/ui/Badge'
import { Button } from '../../../components/ui/Button'
import { Table, TableContainer } from '../../../components/ui/Table'
import { formatDateTime, formatSourceType } from '../../../lib/format'
import { formatScheduleIntervalMinutes, getSourceScheduleStatusLabel } from '../schedule.js'
import type { SourceResponseDto } from '../../../types/source'

interface SourcesTableProps {
  isAdmin: boolean
  onEdit: (source: SourceResponseDto) => void
  onRunCollection: (source: SourceResponseDto) => void
  runCollectionPendingId?: number
  sources: SourceResponseDto[]
}

export function SourcesTable({
  isAdmin,
  onEdit,
  onRunCollection,
  runCollectionPendingId,
  sources,
}: SourcesTableProps) {
  return (
    <TableContainer className="max-h-[calc(100vh-22rem)]">
      <Table>
        <thead className="bg-slate-50 text-left text-xs uppercase tracking-[0.18em] text-slate-400">
          <tr>
            <th className="px-5 py-4 font-semibold">Название</th>
            <th className="px-5 py-4 font-semibold">Организация</th>
            <th className="px-5 py-4 font-semibold">Тип</th>
            <th className="px-5 py-4 font-semibold">URL</th>
            <th className="px-5 py-4 font-semibold">Статус</th>
            <th className="px-5 py-4 font-semibold">Автосбор</th>
            <th className="px-5 py-4 font-semibold">Обновлен</th>
            {isAdmin && <th className="px-5 py-4 font-semibold text-right">Действия</th>}
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-100">
          {sources.map((source) => (
            <tr key={source.id} className="transition hover:bg-slate-50/80">
              <td className="px-5 py-4">
                <div className="space-y-1">
                  <p className="font-semibold text-slate-900">{source.name}</p>
                  <p className="text-sm text-slate-500">
                    {source.description || 'Описание не указано'}
                  </p>
                </div>
              </td>
              <td className="px-5 py-4">
                <div className="space-y-1">
                  <p className="font-medium text-slate-900">{source.organization.shortName}</p>
                  <p className="text-sm text-slate-500">{source.organization.name}</p>
                </div>
              </td>
              <td className="px-5 py-4 text-slate-600">{formatSourceType(source.type)}</td>
              <td className="max-w-xs px-5 py-4 text-slate-500">
                <span className="line-clamp-2 break-all">{source.baseUrl}</span>
              </td>
              <td className="px-5 py-4">
                <Badge variant={source.isActive ? 'active' : 'inactive'}>
                  {source.isActive ? 'Активен' : 'Отключен'}
                </Badge>
              </td>
              <td className="px-5 py-4">
                <div className="space-y-1 text-sm text-slate-600">
                  <p className="font-medium text-slate-900">
                    {getSourceScheduleStatusLabel(source.scheduleEnabled)}
                  </p>
                  {source.scheduleEnabled && (
                    <>
                      <p>
                        Интервал:{' '}
                        {formatScheduleIntervalMinutes(source.scheduleIntervalMinutes)}
                      </p>
                      <p>Последний сбор: {formatDateTime(source.lastCollectedAt)}</p>
                      <p>Следующий сбор: {formatDateTime(source.nextCollectionAt)}</p>
                    </>
                  )}
                </div>
              </td>
              <td className="px-5 py-4 text-slate-500">{formatDateTime(source.updatedAt)}</td>
              {isAdmin && (
                <td className="px-5 py-4">
                  <div className="flex justify-end gap-2">
                    <Button
                      size="sm"
                      variant="secondary"
                      onClick={() => onRunCollection(source)}
                      isLoading={runCollectionPendingId === source.id}
                    >
                      Запустить сбор
                    </Button>
                    <Button size="sm" variant="ghost" onClick={() => onEdit(source)}>
                      Редактировать
                    </Button>
                  </div>
                </td>
              )}
            </tr>
          ))}
        </tbody>
      </Table>
    </TableContainer>
  )
}
