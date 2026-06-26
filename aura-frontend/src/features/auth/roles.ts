import type { UserRole } from '../../types/auth'

export function hasRequiredRole(
  roles: UserRole[],
  allowedRoles?: UserRole[],
) {
  if (!allowedRoles || allowedRoles.length === 0) return true
  return allowedRoles.some((role) => roles.includes(role))
}
