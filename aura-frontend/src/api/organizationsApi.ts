import { coreClient } from './http'
import { IS_MOCK_DATA_MODE } from '../config/appMode'
import { mockOrganizationsApi } from '../mocks/mockApi'
import type {
  CreateOrganizationRequestDto,
  OrganizationFilters,
  OrganizationInsightsResponseDto,
  OrganizationResponseDto,
  UpdateOrganizationRequestDto,
} from '../types/organization'

const realOrganizationsApi = {
  getOrganizations: async (filters?: OrganizationFilters) => {
    const { data } = await coreClient.get<OrganizationResponseDto[]>('/api/organizations', {
      params: {
        name: filters?.name,
        isActive: filters?.isActive,
      },
    })
    return data
  },
  getOrganizationById: async (id: number) => {
    const { data } = await coreClient.get<OrganizationResponseDto>(`/api/organizations/${id}`)
    return data
  },
  generateOrganizationInsights: async (
    organizationId: number,
    params?: { force?: boolean; limit?: number; from?: string; to?: string },
  ) => {
    const { data } = await coreClient.post<OrganizationInsightsResponseDto>(
      `/api/organizations/${organizationId}/insights`,
      undefined,
      {
        params: {
          force: params?.force || undefined,
          limit: params?.limit,
          from: params?.from,
          to: params?.to,
        },
      },
    )
    return data
  },
  createOrganization: async (payload: CreateOrganizationRequestDto) => {
    const { data } = await coreClient.post<OrganizationResponseDto>(
      '/api/organizations',
      payload,
    )
    return data
  },
  updateOrganization: async (id: number, payload: UpdateOrganizationRequestDto) => {
    const { data } = await coreClient.patch<OrganizationResponseDto>(
      `/api/organizations/${id}`,
      payload,
    )
    return data
  },
  deleteOrganization: async (id: number) => {
    await coreClient.delete(`/api/organizations/${id}`)
  },
}

export const organizationsApi = IS_MOCK_DATA_MODE
  ? mockOrganizationsApi
  : realOrganizationsApi
