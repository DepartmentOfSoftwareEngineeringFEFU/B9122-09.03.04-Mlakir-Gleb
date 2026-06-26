import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuthStore } from './authStore'
import { selectIsAuthenticated, useHasAnyRole } from './access'
import type { UserRole } from '../../types/auth'

interface ProtectedRouteProps {
  allowedRoles?: UserRole[]
}

export function ProtectedRoute({ allowedRoles }: ProtectedRouteProps) {
  const isAuthenticated = useAuthStore(selectIsAuthenticated)
  const hasAccess = useHasAnyRole(allowedRoles)
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location }} />
  }

  if (!hasAccess) {
    return <Navigate to="/dashboard" replace />
  }

  return <Outlet />
}
