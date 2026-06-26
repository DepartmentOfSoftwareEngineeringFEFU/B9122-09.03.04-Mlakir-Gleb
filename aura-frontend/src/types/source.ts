import type { OrganizationShortResponseDto } from './organization'

export type SourceType = 'MANUAL_IMPORT' | 'TABITURIENT' | 'OTZOVIK' | 'VUZOPEDIA'

export type CollectionMode = 'MANUAL' | 'SCHEDULED'

export interface SourceResponseDto {
  id: number
  organization: OrganizationShortResponseDto
  name: string
  type: SourceType
  baseUrl: string
  collectionMode: CollectionMode
  scheduleEnabled: boolean
  scheduleIntervalMinutes?: number | null
  lastCollectedAt?: string | null
  nextCollectionAt?: string | null
  description?: string | null
  createdAt: string
  updatedAt: string
  isActive: boolean
}

export interface CreateSourceRequestDto {
  organizationId: number
  name: string
  type: SourceType
  baseUrl: string
  collectionMode?: CollectionMode
  scheduleEnabled?: boolean
  scheduleIntervalMinutes?: number | null
  description?: string
}

export interface UpdateSourceRequestDto {
  organizationId?: number
  name?: string
  baseUrl?: string
  isActive?: boolean
  collectionMode?: CollectionMode
  scheduleEnabled?: boolean
  scheduleIntervalMinutes?: number | null
  description?: string
}

export interface ManualImportResponseDto {
  sourceId: number
  fileName: string
  totalRows: number
  importedCount: number
  duplicateCount: number
  invalidCount: number
}

export interface SourceFilters {
  organizationId?: number
  name?: string
  type?: SourceType
  isActive?: boolean
  scheduleEnabled?: boolean
}
