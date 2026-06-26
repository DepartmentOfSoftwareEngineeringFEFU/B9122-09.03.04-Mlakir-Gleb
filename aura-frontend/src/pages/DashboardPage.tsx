import { Suspense, lazy, useEffect, useMemo, useState } from 'react'
import toast from 'react-hot-toast'
import { useSearchParams } from 'react-router-dom'
import { ErrorState } from '../components/common/ErrorState'
import { EmptyState } from '../components/common/EmptyState'
import { Loader } from '../components/common/Loader'
import { PageHeader } from '../components/common/PageHeader'
import { Button } from '../components/ui/Button'
import { useHasRole } from '../features/auth/access'
import { DashboardFiltersCard } from '../features/dashboard/components/DashboardFiltersCard'
import { DashboardInsightsCard } from '../features/dashboard/components/DashboardInsightsCard'
import {
  getDashboardFilterState,
  mapDashboardFilterStateToQuery,
} from '../features/dashboard/filters'
import { useDashboardQuery } from '../features/dashboard/queries'
import { useDashboardInsights } from '../features/dashboard/useDashboardInsights'
import { useOrganizationsQuery } from '../features/organizations/queries'
import { ReanalyzeReviewsDialog } from '../features/reviews/components/ReanalyzeReviewsDialog'
import { useReanalyzeReviewsMutation } from '../features/reviews/queries'
import {
  buildReanalyzeSuccessMessage,
  buildReanalyzeWarningMessage,
  getReanalyzeErrorMessage,
} from '../features/reviews/reanalyze'
import {
  ACTIVE_COLLECTION_POLL_INTERVAL,
  useCollectionJobsQuery,
  useSourcesQuery,
} from '../features/sources/queries'
import { updateSearchParams } from '../lib/searchParams'

const DashboardAnalyticsSection = lazy(() =>
  import('../features/dashboard/components/DashboardAnalyticsSection').then((module) => ({
    default: module.DashboardAnalyticsSection,
  })),
)

export function DashboardPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [isReanalyzeOpen, setIsReanalyzeOpen] = useState(false)
  const isAdmin = useHasRole('ROLE_ADMIN')
  const organizationsQuery = useOrganizationsQuery()
  const jobsQuery = useCollectionJobsQuery()
  const hasRunningJobs = jobsQuery.data?.some((job) => job.status === 'RUNNING') ?? false
  const reanalyzeMutation = useReanalyzeReviewsMutation()
  const filterState = useMemo(() => getDashboardFilterState(searchParams), [searchParams])
  const defaultOrganizationId = organizationsQuery.data?.[0]?.id
  const selectedOrganizationId = filterState.organizationId
  const dateFrom = filterState.dateFrom
  const dateTo = filterState.dateTo
  const sourceId = filterState.sourceId
  const sentiment = filterState.sentiment

  useEffect(() => {
    if (!defaultOrganizationId || selectedOrganizationId) return

    setSearchParams(
      updateSearchParams(searchParams, {
        organizationId: String(defaultOrganizationId),
      }),
      { replace: true },
    )
  }, [defaultOrganizationId, searchParams, selectedOrganizationId, setSearchParams])

  const sourceFilters = useMemo(
    () => ({
      organizationId: selectedOrganizationId,
    }),
    [selectedOrganizationId],
  )
  const sourcesQuery = useSourcesQuery(sourceFilters, {
    refetchInterval: hasRunningJobs ? ACTIVE_COLLECTION_POLL_INTERVAL : false,
  })

  const setParam = (key: string, value?: string) => {
    setSearchParams(
      updateSearchParams(searchParams, {
        [key]: value,
      }),
      { replace: true },
    )
  }

  const handleResetFilters = () => {
    setSearchParams(
      selectedOrganizationId
        ? new URLSearchParams({ organizationId: String(selectedOrganizationId) })
        : new URLSearchParams(),
      { replace: true },
    )
  }

  const dashboardQuery = useDashboardQuery(mapDashboardFilterStateToQuery(filterState), {
    refetchInterval: hasRunningJobs ? ACTIVE_COLLECTION_POLL_INTERVAL : false,
  })

  const filteredSources = useMemo(() => sourcesQuery.data ?? [], [sourcesQuery.data])

  useEffect(() => {
    if (!sourceId) return

    const sourceExists = filteredSources.some((item) => String(item.id) === sourceId)
    if (sourceExists) return

    setSearchParams(
      updateSearchParams(searchParams, {
        sourceId: undefined,
      }),
      { replace: true },
    )
  }, [filteredSources, searchParams, setSearchParams, sourceId])

  const insights = useDashboardInsights({
    organizationId: selectedOrganizationId ?? 0,
    dateFrom,
    dateTo,
  })

  if (organizationsQuery.isLoading || sourcesQuery.isLoading) {
    return <Loader label="Загрузка аналитики..." />
  }

  if (organizationsQuery.isError || sourcesQuery.isError) {
    return (
      <ErrorState
        onRetry={() => void Promise.all([organizationsQuery.refetch(), sourcesQuery.refetch()])}
      />
    )
  }

  if (!organizationsQuery.data || organizationsQuery.data.length === 0) {
    return (
      <EmptyState
        title="Нет организаций для аналитики"
        description="Создайте организацию и подключите к ней источники, чтобы открыть дашборд."
      />
    )
  }

  if (!selectedOrganizationId) {
    return <Loader label="Подготовка фильтров дашборда..." />
  }

  if (dashboardQuery.isLoading) {
    return <Loader label="Загрузка аналитики..." />
  }

  if (dashboardQuery.isError || !dashboardQuery.data) {
    return <ErrorState onRetry={() => void dashboardQuery.refetch()} />
  }

  const dashboard = dashboardQuery.data
  const isEmpty =
    dashboard.totalReviews === 0 &&
    dashboard.sourcesCount === 0 &&
    dashboard.timeline.length === 0 &&
    dashboard.topCategories.length === 0

  return (
    <div className="space-y-6">
      <PageHeader
        title="Дашборд"
        actions={
          isAdmin ? (
            <Button variant="secondary" onClick={() => setIsReanalyzeOpen(true)}>
              Повторить анализ
            </Button>
          ) : undefined
        }
      />

      <DashboardFiltersCard
        dateFrom={dateFrom}
        dateTo={dateTo}
        organizations={organizationsQuery.data}
        selectedOrganizationId={selectedOrganizationId}
        sentiment={sentiment}
        sourceId={sourceId}
        sources={filteredSources}
        onOrganizationChange={(value) => {
          setSearchParams(
            updateSearchParams(searchParams, {
              organizationId: value,
              sourceId: undefined,
            }),
            { replace: true },
          )
        }}
        onReset={handleResetFilters}
        onParamChange={setParam}
      />

      <DashboardInsightsCard
        insightsData={insights.insightsData}
        insightsError={insights.insightsError}
        insightsLimit={insights.insightsLimit}
        insightsMeta={insights.insightsMeta}
        insightsOpen={insights.insightsOpen}
        isAdmin={isAdmin}
        isPending={insights.isPending}
        onForce={insights.handleForceInsights}
        onGenerate={insights.handleGenerateInsights}
        onLimitChange={insights.setInsightsLimit}
      />

      {isEmpty ? (
        <EmptyState
          title="Недостаточно данных для аналитики"
          description="Для выбранной организации и текущих фильтров пока нет отзывов."
        />
      ) : (
        <Suspense fallback={<Loader label="Загрузка аналитических виджетов..." />}>
          <DashboardAnalyticsSection dashboard={dashboard} />
        </Suspense>
      )}

      {isAdmin && organizationsQuery.data && sourcesQuery.data && (
        <ReanalyzeReviewsDialog
          open={isReanalyzeOpen}
          organizations={organizationsQuery.data}
          sources={sourcesQuery.data}
          initialOrganizationId={selectedOrganizationId}
          isLoading={reanalyzeMutation.isPending}
          onClose={() => setIsReanalyzeOpen(false)}
          onSubmit={(params) => {
            reanalyzeMutation.mutate(params, {
              onSuccess: (result) => {
                toast.success(buildReanalyzeSuccessMessage(result))
                if (result.errorMessage) {
                  toast(buildReanalyzeWarningMessage(result), {
                    icon: '!',
                  })
                }
                setIsReanalyzeOpen(false)
              },
              onError: (error) => {
                toast.error(getReanalyzeErrorMessage(error))
              },
            })
          }}
        />
      )}
    </div>
  )
}
