import { useMemo, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { CollectionJobsCard } from '../features/sources/components/CollectionJobsCard'
import { ErrorState } from '../components/common/ErrorState'
import { EmptyState } from '../components/common/EmptyState'
import { Loader } from '../components/common/Loader'
import { PageHeader } from '../components/common/PageHeader'
import { Button } from '../components/ui/Button'
import { useOrganizationsQuery } from '../features/organizations/queries'
import { SourcesFiltersCard } from '../features/sources/components/SourcesFiltersCard'
import { getSourceFilters } from '../features/sources/filters'
import {
  ACTIVE_COLLECTION_POLL_INTERVAL,
  useCollectionJobsQuery,
  useSourcesQuery,
} from '../features/sources/queries'
import { SourceEditDialog } from '../features/sources/components/SourceEditDialog'
import { SourcesTable } from '../features/sources/components/SourcesTable'
import {
  getResetSourceFiltersSearchParams,
  getSourceFilterSearchParams,
  getSourcesEmptyStateActionPath,
} from '../features/sources/navigation'
import { useSourceActions } from '../features/sources/useSourceActions'
import type { SourceFilters, SourceResponseDto } from '../types/source'
import { useHasRole } from '../features/auth/access'

export function SourcesPage() {
  const navigate = useNavigate()
  const [searchParams, setSearchParams] = useSearchParams()
  const [editingSource, setEditingSource] = useState<SourceResponseDto | null>(null)
  const isAdmin = useHasRole('ROLE_ADMIN')
  const organizationsQuery = useOrganizationsQuery()
  const jobsQuery = useCollectionJobsQuery()
  const hasRunningJobs = jobsQuery.data?.some((job) => job.status === 'RUNNING') ?? false
  const sourceFilters = useMemo<SourceFilters>(
    () => getSourceFilters(searchParams),
    [searchParams],
  )
  const sourcesQuery = useSourcesQuery(sourceFilters, {
    refetchInterval: hasRunningJobs ? ACTIVE_COLLECTION_POLL_INTERVAL : false,
  })

  const latestJobs = useMemo(
    () => jobsQuery.data?.slice(0, 5) ?? [],
    [jobsQuery.data],
  )

  const organizationOptions =
    organizationsQuery.data?.map((organization) => ({
      value: String(organization.id),
      label: `${organization.shortName} — ${organization.name}`,
    })) ?? []
  const hasActiveFilters = Object.values(sourceFilters).some((value) => value !== undefined)
  const {
    handleEditSubmit,
    handleRunCollection,
    runCollectionMutation,
    updateSourceMutation,
  } = useSourceActions({
    editingSource,
    onEditClose: () => setEditingSource(null),
  })

  const setParam = (key: string, value?: string) =>
    setSearchParams(getSourceFilterSearchParams(searchParams, key, value), {
      replace: true,
    })

  const handleResetFilters = () => {
    setSearchParams(getResetSourceFiltersSearchParams(), { replace: true })
  }

  if (sourcesQuery.isLoading) {
    return <Loader label="Загрузка источников..." />
  }

  if (sourcesQuery.isError || !sourcesQuery.data) {
    return <ErrorState onRetry={() => void sourcesQuery.refetch()} />
  }

  const emptyStateActionPath = getSourcesEmptyStateActionPath(hasActiveFilters)

  return (
    <div className="space-y-6">
      <PageHeader
        title="Источники"
        actions={
          isAdmin ? (
            <Link to="/sources/new">
              <Button>Новый источник</Button>
            </Link>
          ) : undefined
        }
      />

      <SourcesFiltersCard
        organizationOptions={organizationOptions}
        selectedName={searchParams.get('name') ?? ''}
        selectedOrganizationId={searchParams.get('organizationId') ?? ''}
        selectedScheduleEnabled={searchParams.get('scheduleEnabled') ?? ''}
        selectedStatus={searchParams.get('isActive') ?? ''}
        selectedType={searchParams.get('type') ?? ''}
        onParamChange={setParam}
        onReset={handleResetFilters}
      />

      {sourcesQuery.data.length === 0 ? (
        <EmptyState
          title={hasActiveFilters ? 'Источники не найдены' : 'Источники пока не созданы'}
          description={
            hasActiveFilters
              ? 'Попробуйте изменить параметры фильтрации.'
              : 'Создайте источник Tabiturient, Otzovik или Vuzopedia для автоматического сбора отзывов.'
          }
          actionLabel={hasActiveFilters ? undefined : 'Создать источник'}
          onAction={emptyStateActionPath ? () => navigate(emptyStateActionPath) : undefined}
        />
      ) : (
        <SourcesTable
          isAdmin={isAdmin}
          onEdit={setEditingSource}
          onRunCollection={handleRunCollection}
          runCollectionPendingId={
            runCollectionMutation.isPending ? runCollectionMutation.variables : undefined
          }
          sources={sourcesQuery.data}
        />
      )}

      <CollectionJobsCard
        jobs={latestJobs}
        isLoading={jobsQuery.isLoading}
        isError={jobsQuery.isError}
      />

      {editingSource && (
        <SourceEditDialog
          source={editingSource}
          organizationOptions={organizationOptions}
          isLoading={updateSourceMutation.isPending}
          onClose={() => setEditingSource(null)}
          onSubmit={handleEditSubmit}
        />
      )}
    </div>
  )
}
