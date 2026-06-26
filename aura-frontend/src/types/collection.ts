export type CollectionJobStatus = 'RUNNING' | 'SUCCESS' | 'FAILED'

export interface CollectionJobResponseDto {
  id: number
  sourceId: number
  sourceName: string
  status: CollectionJobStatus
  startedAt: string
  finishedAt?: string | null
  collectedCount?: number | null
  errorMessage?: string | null
  triggeredBy?: string | null
}
