export function getRootRedirectPath(isAuthenticated: boolean) {
  return isAuthenticated ? '/dashboard' : '/login'
}

export function getAuthPageRedirectPath(isAuthenticated: boolean) {
  return isAuthenticated ? '/dashboard' : null
}
