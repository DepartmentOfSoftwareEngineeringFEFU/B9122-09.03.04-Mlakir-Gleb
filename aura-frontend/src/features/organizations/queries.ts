import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { organizationsApi } from '../../api/organizationsApi'
import type {
  CreateOrganizationRequestDto,
  OrganizationFilters,
  UpdateOrganizationRequestDto,
} from '../../types/organization'

export function useOrganizationsQuery(filters?: OrganizationFilters) {
  return useQuery({
    queryKey: ['organizations', filters],
    queryFn: () => organizationsApi.getOrganizations(filters),
    placeholderData: (previousData) => previousData,
  })
}

export function useCreateOrganizationMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: CreateOrganizationRequestDto) =>
      organizationsApi.createOrganization(payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['organizations'] })
    },
  })
}

export function useUpdateOrganizationMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: { id: number; data: UpdateOrganizationRequestDto }) =>
      organizationsApi.updateOrganization(payload.id, payload.data),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['organizations'] })
    },
  })
}

export function useOrganizationInsightsMutation() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (payload: {
      organizationId: number
      params?: { force?: boolean; limit?: number; from?: string; to?: string }
    }) => organizationsApi.generateOrganizationInsights(payload.organizationId, payload.params),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['dashboard'] })
    },
  })
}
