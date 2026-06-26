import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { DebouncedInput } from '../components/common/DebouncedInput'
import { PageHeader } from '../components/common/PageHeader'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { Select } from '../components/ui/Select'
import { Table, TableContainer } from '../components/ui/Table'
import { IS_MOCKUP_UI_MODE } from '../config/appMode'
import { useHasRole } from '../features/auth/access'
import { PlatformForm } from '../features/customPlatforms/PlatformForm'
import {
  connectionModeOptions,
  getConnectionModeLabel,
  getPlatformEndpointLabel,
  getPlatformFormState,
  loadPlatformsFromStorage,
  savePlatformsToStorage,
  statusOptions,
  type CustomPlatform,
  type ConnectionMode,
  type PlatformFormState,
} from '../features/customPlatforms/model'
import { formatDateTime } from '../lib/format'

export function CustomSourcesPage() {
  const isAdmin = useHasRole('ROLE_ADMIN')
  const [platforms, setPlatforms] = useState<CustomPlatform[]>(() => loadPlatformsFromStorage())
  const [editingPlatform, setEditingPlatform] = useState<CustomPlatform | null>(null)
  const [nameFilter, setNameFilter] = useState('')
  const [connectionModeFilter, setConnectionModeFilter] = useState('')
  const [statusFilter, setStatusFilter] = useState('')

  useEffect(() => {
    savePlatformsToStorage(platforms)
  }, [platforms])

  const sortedPlatforms = useMemo(
    () => platforms.slice().sort((a, b) => b.updatedAt.localeCompare(a.updatedAt)),
    [platforms],
  )
  const filteredPlatforms = useMemo(
    () =>
      sortedPlatforms.filter((platform) => {
        if (nameFilter) {
          const query = nameFilter.toLowerCase()
          const haystack = `${platform.name} ${platform.description}`.toLowerCase()
          if (!haystack.includes(query)) return false
        }

        if (
          connectionModeFilter &&
          platform.connectionMode !== (connectionModeFilter as ConnectionMode)
        ) {
          return false
        }

        if (statusFilter) {
          const expected = statusFilter === 'true'
          if (platform.isActive !== expected) return false
        }

        return true
      }),
    [connectionModeFilter, nameFilter, sortedPlatforms, statusFilter],
  )

  const handleEditSubmit = (values: PlatformFormState) => {
    if (!editingPlatform) return

    setPlatforms((current) =>
      current.map((platform) =>
        platform.id === editingPlatform.id
          ? {
              ...platform,
              ...values,
              htmlConfig: { ...values.htmlConfig },
              apiConfig: { ...values.apiConfig },
              updatedAt: new Date().toISOString(),
            }
          : platform,
      ),
    )
    setEditingPlatform(null)
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Пользовательские платформы отзывов"
        actions={
          isAdmin ? (
            <Link to="/custom-sources/new">
              <Button>Новая платформа</Button>
            </Link>
          ) : IS_MOCKUP_UI_MODE ? undefined : (
            <Badge variant="warning">В разработке</Badge>
          )
        }
      />

      {!IS_MOCKUP_UI_MODE && (
        <Card className="space-y-3 border-amber-200 bg-amber-50/70">
          <h2 className="text-xl font-semibold text-slate-950">Гибкое подключение платформ</h2>
          <p className="text-sm leading-7 text-slate-700">
            Раздел позволяет хранить конфигурации пользовательских платформ, чтобы подключать новые
            сайты отзывов через HTML-скрапинг или API-интеграцию без изменения структуры интерфейса.
          </p>
        </Card>
      )}

      <Card className="space-y-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <h2 className="text-xl font-semibold text-slate-950">Фильтры</h2>
          <Button
            size="sm"
            type="button"
            variant="ghost"
            onClick={() => {
              setNameFilter('')
              setConnectionModeFilter('')
              setStatusFilter('')
            }}
          >
            Сбросить
          </Button>
        </div>
        <div className="grid gap-4 md:grid-cols-3">
          <DebouncedInput
            label="Название платформы"
            value={nameFilter}
            placeholder="Например, Example Reviews"
            onCommit={setNameFilter}
          />
          <Select
            label="Способ подключения"
            options={connectionModeOptions}
            placeholder="Все способы"
            value={connectionModeFilter}
            onChange={(event) => setConnectionModeFilter(event.target.value)}
          />
          <Select
            label="Статус"
            options={statusOptions}
            placeholder="Все статусы"
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
          />
        </div>
      </Card>

      <Card className="space-y-4">
        <div className="flex flex-wrap items-start justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold text-slate-950">Созданные платформы</h2>
            <p className="text-sm leading-6 text-slate-600">
              Список подключённых конфигураций для сбора отзывов.
            </p>
          </div>
          <Badge variant="default">
            {filteredPlatforms.length}
            {filteredPlatforms.length !== sortedPlatforms.length
              ? ` из ${sortedPlatforms.length}`
              : ''}
          </Badge>
        </div>

        <TableContainer>
          <Table>
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-[0.18em] text-slate-400">
              <tr>
                <th className="px-5 py-4 font-semibold">Платформа</th>
                <th className="px-5 py-4 font-semibold">Подключение</th>
                <th className="px-5 py-4 font-semibold">Точка входа</th>
                <th className="px-5 py-4 font-semibold">Статус</th>
                <th className="px-5 py-4 font-semibold">Обновлена</th>
                {isAdmin && <th className="px-5 py-4 font-semibold text-right">Действия</th>}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {filteredPlatforms.map((platform) => (
                <tr key={platform.id} className="transition hover:bg-slate-50/80">
                  <td className="px-5 py-4">
                    <div className="space-y-1">
                      <p className="font-semibold text-slate-900">{platform.name}</p>
                      <p className="text-sm text-slate-500">
                        {platform.description || 'Описание не указано'}
                      </p>
                    </div>
                  </td>
                  <td className="px-5 py-4 text-slate-600">
                    {getConnectionModeLabel(platform.connectionMode)}
                  </td>
                  <td className="px-5 py-4 text-sm text-slate-600">
                    {getPlatformEndpointLabel(platform)}
                  </td>
                  <td className="px-5 py-4">
                    <Badge variant={platform.isActive ? 'active' : 'inactive'}>
                      {platform.isActive ? 'Активна' : 'Отключена'}
                    </Badge>
                  </td>
                  <td className="px-5 py-4 text-slate-500">
                    {formatDateTime(platform.updatedAt)}
                  </td>
                  {isAdmin && (
                    <td className="px-5 py-4 text-right">
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => setEditingPlatform(platform)}
                      >
                        Редактировать
                      </Button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </Table>
        </TableContainer>
        {filteredPlatforms.length === 0 && (
          <p className="text-sm text-slate-500">По текущим фильтрам платформы не найдены.</p>
        )}
      </Card>

      {editingPlatform && (
        <PlatformEditDialog
          key={editingPlatform.id}
          platform={editingPlatform}
          onClose={() => setEditingPlatform(null)}
          onSubmit={handleEditSubmit}
        />
      )}
    </div>
  )
}

function PlatformEditDialog({
  platform,
  onClose,
  onSubmit,
}: {
  platform: CustomPlatform
  onClose: () => void
  onSubmit: (values: PlatformFormState) => void
}) {
  const [value, setValue] = useState<PlatformFormState>(() =>
    getPlatformFormState(platform),
  )

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto bg-slate-950/30 p-4 backdrop-blur-sm">
      <div className="flex min-h-full items-start justify-center py-4">
        <Card className="max-h-[calc(100vh-2rem)] w-full max-w-5xl overflow-y-auto p-6 lg:p-8">
          <div className="flex items-start justify-between gap-4">
            <div className="space-y-2">
              <h2 className="text-xl font-semibold text-slate-950">Редактирование платформы</h2>
              <p className="text-sm leading-6 text-slate-600">
                Обновите конфигурацию подключения и сохраните изменения.
              </p>
            </div>
            <Button type="button" variant="ghost" size="sm" onClick={onClose}>
              Закрыть
            </Button>
          </div>

          <div className="mt-6">
            <PlatformForm
              submitLabel="Сохранить изменения"
              cancelLabel="Отмена"
              value={value}
              onChange={setValue}
              onCancel={onClose}
              onSubmit={() => onSubmit(value)}
            />
          </div>
        </Card>
      </div>
    </div>
  )
}
