import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { reviewsApi } from '../../api/reviewsApi'
import type { ReanalyzeReviewsParams, ReviewFilters } from '../../types/review'

interface UseReviewsQueryOptions {
  refetchInterval?: number | false
}

export function useReviewsQuery(
  filters: ReviewFilters,
  options?: UseReviewsQueryOptions,
) {
  return useQuery({
    queryKey: ['reviews', filters],
    queryFn: () => reviewsApi.getReviews(filters),
    placeholderData: (previousData) => previousData,
    refetchInterval: options?.refetchInterval,
  })
}

export function useReviewDetailsQuery(id: number) {
  return useQuery({
    queryKey: ['reviews', id],
    queryFn: () => reviewsApi.getReviewById(id),
    enabled: Number.isFinite(id),
  })
}

export function usePopularKeywordsQuery(
  params?: { organizationId?: number; limit?: number },
  options?: UseReviewsQueryOptions,
) {
  return useQuery({
    queryKey: ['reviews', 'popular-keywords', params?.organizationId, params?.limit ?? 12],
    queryFn: () => reviewsApi.getPopularKeywords(params),
    placeholderData: (previousData) => previousData,
    refetchInterval: options?.refetchInterval,
  })
}

export function useReviewSummaryMutation() {
  return useMutation({
    mutationFn: ({ reviewId, force = false }: { reviewId: number; force?: boolean }) =>
      reviewsApi.getReviewSummary(reviewId, force),
  })
}

export function useReanalyzeReviewsMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (params?: ReanalyzeReviewsParams) => reviewsApi.reanalyzeReviews(params),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['reviews'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
        queryClient.invalidateQueries({ queryKey: ['sources'] }),
        queryClient.invalidateQueries({ queryKey: ['collection', 'jobs'] }),
      ])
    },
  })
}
