import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { collectionApi } from '../../api/collectionApi'
import { sourcesApi } from '../../api/sourcesApi'
import type { CollectionJobResponseDto } from '../../types/collection'
import type {
  CreateSourceRequestDto,
  SourceFilters,
  UpdateSourceRequestDto,
} from '../../types/source'

export const ACTIVE_COLLECTION_POLL_INTERVAL = 5_000
export const IDLE_COLLECTION_POLL_INTERVAL = 30_000

function getCollectionRefetchInterval(
  data: CollectionJobResponseDto[] | undefined,
): number {
  return data?.some((job) => job.status === 'RUNNING')
    ? ACTIVE_COLLECTION_POLL_INTERVAL
    : IDLE_COLLECTION_POLL_INTERVAL
}

interface UseSourcesQueryOptions {
  refetchInterval?: number | false
}

export function useSourcesQuery(
  filters?: SourceFilters,
  options?: UseSourcesQueryOptions,
) {
  return useQuery({
    queryKey: ['sources', filters],
    queryFn: () => sourcesApi.getSources(filters),
    placeholderData: (previousData) => previousData,
    refetchInterval: options?.refetchInterval,
  })
}

export function useCollectionJobsQuery() {
  return useQuery({
    queryKey: ['collection', 'jobs'],
    queryFn: collectionApi.getJobs,
    refetchInterval: (query) => getCollectionRefetchInterval(query.state.data),
  })
}

export function useCreateSourceMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: CreateSourceRequestDto) => sourcesApi.createSource(payload),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['sources'] }),
        queryClient.invalidateQueries({ queryKey: ['collection', 'jobs'] }),
      ])
    },
  })
}

export function useUpdateSourceMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: { id: number; data: UpdateSourceRequestDto }) =>
      sourcesApi.updateSource(payload.id, payload.data),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['sources'] }),
        queryClient.invalidateQueries({ queryKey: ['collection', 'jobs'] }),
      ])
    },
  })
}

export function useRunCollectionMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (sourceId: number) => collectionApi.runCollection(sourceId),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['sources'] }),
        queryClient.invalidateQueries({ queryKey: ['collection', 'jobs'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
        queryClient.invalidateQueries({ queryKey: ['reviews'] }),
      ])
    },
  })
}

export function useImportReviewsMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: { sourceId: number; file: File }) =>
      sourcesApi.importReviews(payload.sourceId, payload.file),
    onSuccess: async () => {
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: ['sources'] }),
        queryClient.invalidateQueries({ queryKey: ['reviews'] }),
        queryClient.invalidateQueries({ queryKey: ['dashboard'] }),
        queryClient.invalidateQueries({ queryKey: ['collection', 'jobs'] }),
      ])
    },
  })
}
