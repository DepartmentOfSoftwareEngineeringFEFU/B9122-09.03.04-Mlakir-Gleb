import { useMemo, useState } from 'react'
import toast from 'react-hot-toast'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { DebouncedInput } from '../components/common/DebouncedInput'
import { EmptyState } from '../components/common/EmptyState'
import { ErrorState } from '../components/common/ErrorState'
import { Loader } from '../components/common/Loader'
import { PageHeader } from '../components/common/PageHeader'
import { Badge } from '../components/ui/Badge'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { Select } from '../components/ui/Select'
import { Table, TableContainer } from '../components/ui/Table'
import { useHasRole } from '../features/auth/access'
import { mapUpdateOrganizationFormToDto, type UpdateOrganizationFormValues } from '../features/organizations/form'
import { OrganizationEditDialog } from '../features/organizations/components/OrganizationEditDialog'
import { getOrganizationMutationErrorMessage } from '../features/organizations/errors'
import {
  useOrganizationsQuery,
  useUpdateOrganizationMutation,
} from '../features/organizations/queries'
import { getOrganizationFilters } from '../features/organizations/filters'
import { formatDateTime } from '../lib/format'
import { updateSearchParams } from '../lib/searchParams'
import type {
  OrganizationFilters,
  OrganizationResponseDto,
} from '../types/organization'

export function OrganizationsPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const isAdmin = useHasRole('ROLE_ADMIN')
  const organizationFilters = useMemo<OrganizationFilters>(
    () => getOrganizationFilters(searchParams),
    [searchParams],
  )
  const organizationsQuery = useOrganizationsQuery(organizationFilters)
  const updateOrganizationMutation = useUpdateOrganizationMutation()
  const [editingOrganization, setEditingOrganization] =
    useState<OrganizationResponseDto | null>(null)

  const filterStatusOptions = [
    { value: 'true', label: 'Активные' },
    { value: 'false', label: 'Отключенные' },
  ]
  const hasActiveFilters = Object.values(organizationFilters).some(
    (value) => value !== undefined,
  )

  const setParam = (key: string, value?: string) => {
    setSearchParams(
      updateSearchParams(searchParams, {
        [key]: value,
      }),
      { replace: true },
    )
  }

  const handleResetFilters = () => {
    setSearchParams(new URLSearchParams(), { replace: true })
  }

  const handleEditSubmit = (values: UpdateOrganizationFormValues) => {
    if (!editingOrganization) return

    updateOrganizationMutation.mutate(
      {
        id: editingOrganization.id,
        data: mapUpdateOrganizationFormToDto(values),
      },
      {
        onSuccess: () => {
          toast.success('Организация обновлена')
          setEditingOrganization(null)
        },
        onError: (error) => {
          toast.error(getOrganizationMutationErrorMessage(error, 'update'))
        },
      },
    )
  }

  if (organizationsQuery.isLoading) {
    return <Loader label="Загрузка организаций..." />
  }

  if (organizationsQuery.isError || !organizationsQuery.data) {
    return <ErrorState onRetry={() => void organizationsQuery.refetch()} />
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Организации"
        actions={
          isAdmin ? (
            <Link to="/organizations/new">
              <Button>Новая организация</Button>
            </Link>
          ) : undefined
        }
      />

      <Card className="p-5">
        <div className="mb-4">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-950">Фильтры</h2>
            </div>
            <Button size="sm" type="button" variant="ghost" onClick={handleResetFilters}>
              Сбросить
            </Button>
          </div>
        </div>
        <div className="grid gap-4 md:grid-cols-2">
          <DebouncedInput
            label="Название организации"
            value={searchParams.get('name') ?? ''}
            placeholder="Например, ДВФУ"
            onCommit={(value) => setParam('name', value || undefined)}
          />
          <Select
            label="Статус"
            value={searchParams.get('isActive') ?? ''}
            onChange={(event) => setParam('isActive', event.target.value || undefined)}
            options={filterStatusOptions}
            placeholder="Все статусы"
          />
        </div>
      </Card>

      {organizationsQuery.data.length === 0 ? (
        <EmptyState
          title={hasActiveFilters ? 'Организации не найдены' : 'Организации пока не созданы'}
          description={
            hasActiveFilters
              ? 'Попробуйте изменить параметры фильтрации.'
              : 'Создайте первую организацию, чтобы затем подключить к ней источники и начать сбор отзывов.'
          }
          actionLabel={isAdmin && !hasActiveFilters ? 'Создать организацию' : undefined}
          onAction={
            isAdmin && !hasActiveFilters ? () => navigate('/organizations/new') : undefined
          }
        />
      ) : (
        <TableContainer>
          <Table>
            <thead className="bg-slate-50 text-left text-xs uppercase tracking-[0.18em] text-slate-400">
              <tr>
                <th className="px-5 py-4 font-semibold">Организация</th>
                <th className="px-5 py-4 font-semibold">Сайт</th>
                <th className="px-5 py-4 font-semibold">Статус</th>
                <th className="px-5 py-4 font-semibold">Обновлена</th>
                {isAdmin && (
                  <th className="px-5 py-4 font-semibold text-right">Действия</th>
                )}
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {organizationsQuery.data.map((organization) => (
                <tr key={organization.id} className="transition hover:bg-slate-50/80">
                  <td className="px-5 py-4">
                    <div className="space-y-1">
                      <p className="font-semibold text-slate-900">{organization.name}</p>
                      <p className="text-sm text-slate-500">{organization.shortName}</p>
                      <p className="text-sm text-slate-500">
                        {organization.description || 'Описание не указано'}
                      </p>
                    </div>
                  </td>
                  <td className="px-5 py-4 text-slate-600">
                    {organization.website ? (
                      <a
                        href={organization.website}
                        target="_blank"
                        rel="noreferrer"
                        className="hover:text-slate-950"
                      >
                        {organization.website}
                      </a>
                    ) : (
                      '—'
                    )}
                  </td>
                  <td className="px-5 py-4">
                    <Badge variant={organization.isActive ? 'active' : 'inactive'}>
                      {organization.isActive ? 'Активна' : 'Отключена'}
                    </Badge>
                  </td>
                  <td className="px-5 py-4 text-slate-500">
                    {formatDateTime(organization.updatedAt)}
                  </td>
                  {isAdmin && (
                    <td className="px-5 py-4 text-right">
                      <Button
                        size="sm"
                        variant="ghost"
                        onClick={() => setEditingOrganization(organization)}
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
      )}

      {editingOrganization && (
        <OrganizationEditDialog
          isLoading={updateOrganizationMutation.isPending}
          onClose={() => setEditingOrganization(null)}
          onSubmit={handleEditSubmit}
          organization={editingOrganization}
        />
      )}
    </div>
  )
}
