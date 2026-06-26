import { coreClient } from './http'
import { IS_MOCK_DATA_MODE } from '../config/appMode'
import { mockDashboardApi } from '../mocks/mockApi'
import type {
  DashboardFilters,
  DashboardResponseDto,
} from '../types/dashboard'

const realDashboardApi = {
  getDashboard: async (filters: DashboardFilters) => {
    const { data } = await coreClient.get<DashboardResponseDto>('/api/dashboard', {
      params: {
        organizationId: filters.organizationId,
        from: filters.from,
        to: filters.to,
        sourceId: filters.sourceId,
        sentiment: filters.sentiment,
      },
    })
    return data
  },
}

export const dashboardApi = IS_MOCK_DATA_MODE ? mockDashboardApi : realDashboardApi
