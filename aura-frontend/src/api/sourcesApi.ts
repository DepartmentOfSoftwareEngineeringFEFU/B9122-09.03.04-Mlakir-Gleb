import { coreClient } from './http'
import { IS_MOCK_DATA_MODE } from '../config/appMode'
import { mockSourcesApi } from '../mocks/mockApi'
import type {
  CreateSourceRequestDto,
  SourceFilters,
  SourceResponseDto,
  UpdateSourceRequestDto,
} from '../types/source'

const realSourcesApi = {
  getSources: async (filters?: SourceFilters) => {
    const { data } = await coreClient.get<SourceResponseDto[]>('/api/sources', {
      params: {
        organizationId: filters?.organizationId,
        name: filters?.name,
        type: filters?.type,
        isActive: filters?.isActive,
        scheduleEnabled: filters?.scheduleEnabled,
      },
    })
    return data
  },
  getSourceById: async (id: number) => {
    const { data } = await coreClient.get<SourceResponseDto>(`/api/sources/${id}`)
    return data
  },
  createSource: async (payload: CreateSourceRequestDto) => {
    const { data } = await coreClient.post<SourceResponseDto>('/api/sources', payload)
    return data
  },
  updateSource: async (id: number, payload: UpdateSourceRequestDto) => {
    const { data } = await coreClient.patch<SourceResponseDto>(
      `/api/sources/${id}`,
      payload,
    )
    return data
  },
  deleteSource: async (id: number) => {
    await coreClient.delete(`/api/sources/${id}`)
  },
}

export const sourcesApi = IS_MOCK_DATA_MODE ? mockSourcesApi : realSourcesApi
