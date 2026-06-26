export interface OrganizationShortResponseDto {
  id: number
  name: string
  shortName: string
}

export interface OrganizationResponseDto extends OrganizationShortResponseDto {
  description?: string | null
  website?: string | null
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface CreateOrganizationRequestDto {
  name: string
  shortName: string
  description?: string
  website?: string
}

export interface UpdateOrganizationRequestDto {
  name?: string
  shortName?: string
  description?: string
  website?: string
  isActive?: boolean
}

export interface OrganizationFilters {
  name?: string
  isActive?: boolean
}

export type OrganizationInsightsResponseDto = {
  organizationId: number
  organizationName: string
  summary: string
  strengths: string[]
  weaknesses: string[]
  recommendations: string[]
  generatedAt: string
  modelVersion: string
  cached: boolean
  reviewsUsed: number
}
