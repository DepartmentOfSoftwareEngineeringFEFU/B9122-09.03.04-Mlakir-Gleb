import { useMemo } from 'react'
import { useAuthStore } from './authStore.js'
import type { UserRole } from '../../types/auth'
import { hasRequiredRole } from './roles.js'
import { getRolesFromAccessToken } from '../../lib/jwt.js'

export function getAuthRoles(accessToken: string | null): UserRole[] {
  return accessToken ? getRolesFromAccessToken(accessToken) : ['ROLE_USER']
}

export function selectIsAuthenticated(state: {
  accessToken: string | null
  refreshToken: string | null
}) {
  return Boolean(state.accessToken || state.refreshToken)
}

export function useAuthRoles() {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useMemo(() => getAuthRoles(accessToken), [accessToken])
}

export function useHasRole(role: UserRole) {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useMemo(() => getAuthRoles(accessToken).includes(role), [accessToken, role])
}

export function useHasAnyRole(allowedRoles?: UserRole[]) {
  const accessToken = useAuthStore((state) => state.accessToken)
  return useMemo(
    () => hasRequiredRole(getAuthRoles(accessToken), allowedRoles),
    [accessToken, allowedRoles],
  )
}
