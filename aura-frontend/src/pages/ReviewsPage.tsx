import { useEffect, useMemo, useState } from 'react'
import toast from 'react-hot-toast'
import { useSearchParams } from 'react-router-dom'
import { EmptyState } from '../components/common/EmptyState'
import { ErrorState } from '../components/common/ErrorState'
import { Loader } from '../components/common/Loader'
import { PageHeader } from '../components/common/PageHeader'
import { Button } from '../components/ui/Button'
import { useHasRole } from '../features/auth/access'
import { useOrganizationsQuery } from '../features/organizations/queries'
import { ReanalyzeReviewsDialog } from '../features/reviews/components/ReanalyzeReviewsDialog'
import { ReviewsFiltersCard } from '../features/reviews/components/ReviewsFiltersCard'
import { ReviewsPagination } from '../features/reviews/components/ReviewsPagination'
import { ReviewsTable } from '../features/reviews/components/ReviewsTable'
import { getReviewsFilters } from '../features/reviews/filters'
import {
  getClearedUnavailableReviewSourceSearchParams,
  getResetReviewFiltersSearchParams,
  getReviewFilterSearchParams,
  getReviewPageSearchParams,
} from '../features/reviews/navigation'
import {
  usePopularKeywordsQuery,
  useReanalyzeReviewsMutation,
  useReviewsQuery,
} from '../features/reviews/queries'
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

export function ReviewsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const [isReanalyzeOpen, setIsReanalyzeOpen] = useState(false)
  const keywordParam = searchParams.get('keyword') ?? ''
  const isAdmin = useHasRole('ROLE_ADMIN')
  const organizationsQuery = useOrganizationsQuery()
  const jobsQuery = useCollectionJobsQuery()
  const hasRunningJobs = jobsQuery.data?.some((job) => job.status === 'RUNNING') ?? false
  const reanalyzeMutation = useReanalyzeReviewsMutation()
  const filters = useMemo(() => getReviewsFilters(searchParams), [searchParams])

  const reviewsQuery = useReviewsQuery(filters, {
    refetchInterval: hasRunningJobs ? ACTIVE_COLLECTION_POLL_INTERVAL : false,
  })
  const popularKeywordsQuery = usePopularKeywordsQuery(
    {
      organizationId: filters.organizationId,
      limit: 12,
    },
    {
      refetchInterval: hasRunningJobs ? ACTIVE_COLLECTION_POLL_INTERVAL : false,
    },
  )
  const sourceFilters = useMemo(
    () => ({
      organizationId: filters.organizationId,
    }),
    [filters.organizationId],
  )
  const sourcesQuery = useSourcesQuery(sourceFilters, {
    refetchInterval: hasRunningJobs ? ACTIVE_COLLECTION_POLL_INTERVAL : false,
  })

  const filteredSources = useMemo(() => sourcesQuery.data ?? [], [sourcesQuery.data])

  const setParam = (key: string, value?: string) =>
    setSearchParams(getReviewFilterSearchParams(searchParams, key, value))

  useEffect(() => {
    if (!filters.sourceId) return

    const isSourceAvailable = filteredSources.some((source) => source.id === filters.sourceId)
    if (isSourceAvailable) return

    setSearchParams(
      getClearedUnavailableReviewSourceSearchParams(searchParams),
      { replace: true },
    )
  }, [filteredSources, filters.sourceId, searchParams, setSearchParams])

  const handleResetFilters = () => {
    setSearchParams(getResetReviewFiltersSearchParams(), { replace: true })
  }

  if (reviewsQuery.isLoading || sourcesQuery.isLoading || organizationsQuery.isLoading) {
    return <Loader label="Загрузка отзывов..." />
  }

  if (reviewsQuery.isError || !reviewsQuery.data) {
    return <ErrorState onRetry={() => void reviewsQuery.refetch()} />
  }

  const pageData = reviewsQuery.data

  return (
    <div className="space-y-6">
      <PageHeader
        title="Отзывы"
        actions={
          isAdmin ? (
            <Button variant="secondary" onClick={() => setIsReanalyzeOpen(true)}>
              Повторить анализ
            </Button>
          ) : undefined
        }
      />

      <ReviewsFiltersCard
        dateFrom={searchParams.get('dateFrom') ?? ''}
        dateTo={searchParams.get('dateTo') ?? ''}
        keyword={keywordParam}
        organizations={organizationsQuery.data ?? []}
        popularKeywords={popularKeywordsQuery.data ?? []}
        selectedOrganizationId={searchParams.get('organizationId') ?? ''}
        selectedSentiment={searchParams.get('sentiment') ?? ''}
        selectedSort={searchParams.get('sort') ?? 'publishedAt,desc'}
        selectedSourceId={searchParams.get('sourceId') ?? ''}
        selectedTopic={searchParams.get('topic') ?? ''}
        sources={filteredSources}
        onParamChange={setParam}
        onReset={handleResetFilters}
      />

      {pageData.content.length === 0 ? (
        <EmptyState
          title={filters.keyword ? 'По этому ключевому слову отзывы не найдены.' : 'Отзывы не найдены'}
          description={
            filters.keyword
              ? 'Попробуйте изменить ключевое слово или сбросить фильтры.'
              : 'Попробуйте изменить фильтры.'
          }
        />
      ) : (
        <ReviewsTable
          fromSearch={searchParams.toString()}
          reviews={pageData.content}
        />
      )}

      <ReviewsPagination
        pageData={pageData}
        onPrevious={() => {
          setSearchParams(getReviewPageSearchParams(searchParams, pageData.page))
        }}
        onNext={() => {
          setSearchParams(getReviewPageSearchParams(searchParams, pageData.page + 2))
        }}
      />

      {isAdmin && organizationsQuery.data && sourcesQuery.data && (
        <ReanalyzeReviewsDialog
          open={isReanalyzeOpen}
          organizations={organizationsQuery.data}
          sources={sourcesQuery.data}
          initialOrganizationId={filters.organizationId}
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
