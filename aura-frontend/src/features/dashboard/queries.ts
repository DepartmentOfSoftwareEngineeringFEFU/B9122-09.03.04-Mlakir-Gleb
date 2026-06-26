import { useQuery } from '@tanstack/react-query'
import { dashboardApi } from '../../api/dashboardApi'
import type { DashboardFilters } from '../../types/dashboard'

interface UseDashboardQueryOptions {
  refetchInterval?: number | false
}

export function useDashboardQuery(
  filters: DashboardFilters,
  options?: UseDashboardQueryOptions,
) {
  return useQuery({
    queryKey: ['dashboard', filters],
    queryFn: () => dashboardApi.getDashboard(filters),
    enabled: Number.isFinite(filters.organizationId) && filters.organizationId > 0,
    refetchInterval: options?.refetchInterval,
  })
}
