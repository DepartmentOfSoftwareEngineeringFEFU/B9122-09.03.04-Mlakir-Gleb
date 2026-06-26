import type { OrganizationFilters } from '../../types/organization'
import {
  getBooleanSearchParam,
  getTrimmedSearchParam,
} from '../../lib/searchParams'

export function getOrganizationFilters(
  searchParams: URLSearchParams,
): OrganizationFilters {
  return {
    name: getTrimmedSearchParam(searchParams, 'name'),
    isActive: getBooleanSearchParam(searchParams, 'isActive'),
  }
}
