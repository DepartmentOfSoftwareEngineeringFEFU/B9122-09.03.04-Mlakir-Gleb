import { coreClient } from './http'
import { IS_MOCK_DATA_MODE } from '../config/appMode'
import { mockCollectionApi } from '../mocks/mockApi'
import type { CollectionJobResponseDto } from '../types/collection'

const realCollectionApi = {
  runCollection: async (sourceId: number) => {
    const { data } = await coreClient.post<CollectionJobResponseDto>(
      `/api/collection/run/${sourceId}`,
    )
    return data
  },
  getJobs: async (limit = 10) => {
    const { data } = await coreClient.get<CollectionJobResponseDto[]>(
      '/api/collection/jobs',
      { params: { limit } },
    )
    return data
  },
}

export const collectionApi = IS_MOCK_DATA_MODE ? mockCollectionApi : realCollectionApi
