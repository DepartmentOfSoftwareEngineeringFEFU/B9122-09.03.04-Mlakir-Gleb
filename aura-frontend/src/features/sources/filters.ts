import type { SourceFilters, SourceType } from '../../types/source'
import {
  getBooleanSearchParam,
  getEnumSearchParam,
  getPositiveNumberSearchParam,
  getTrimmedSearchParam,
} from '../../lib/searchParams'

const sourceTypeValues = [
  'MANUAL_IMPORT',
  'TABITURIENT',
  'OTZOVIK',
  'VUZOPEDIA',
] as const

export function getSourceFilters(searchParams: URLSearchParams): SourceFilters {
  return {
    organizationId: getPositiveNumberSearchParam(searchParams, 'organizationId'),
    name: getTrimmedSearchParam(searchParams, 'name'),
    type: getEnumSearchParam(searchParams, 'type', sourceTypeValues) as
      | SourceType
      | undefined,
    isActive: getBooleanSearchParam(searchParams, 'isActive'),
    scheduleEnabled: getBooleanSearchParam(searchParams, 'scheduleEnabled'),
  }
}
