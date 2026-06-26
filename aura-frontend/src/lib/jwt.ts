import type { UserRole } from '../types/auth'

interface JwtPayloadLike {
  role?: unknown
  roles?: unknown
  authority?: unknown
  authorities?: unknown
  scope?: unknown
  scp?: unknown
}

const KNOWN_ROLES: UserRole[] = ['ROLE_ADMIN', 'ROLE_USER']

function decodeJwtPayload(token: string): JwtPayloadLike | null {
  const parts = token.split('.')
  if (parts.length < 2) return null

  try {
    const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=')
    const json = atob(padded)
    return JSON.parse(json) as JwtPayloadLike
  } catch {
    return null
  }
}

function extractRoleValues(value: unknown): string[] {
  if (Array.isArray(value)) {
    return value.flatMap((item) => extractRoleValues(item))
  }

  if (typeof value === 'string') {
    return value
      .split(/[,\s]+/)
      .map((item) => item.trim())
      .filter(Boolean)
  }

  if (value && typeof value === 'object') {
    return Object.values(value).flatMap((item) => extractRoleValues(item))
  }

  return []
}

export function getRolesFromAccessToken(accessToken: string): UserRole[] {
  const payload = decodeJwtPayload(accessToken)
  if (!payload) return ['ROLE_USER']

  const roleCandidates = [
    ...extractRoleValues(payload.role),
    ...extractRoleValues(payload.roles),
    ...extractRoleValues(payload.authority),
    ...extractRoleValues(payload.authorities),
    ...extractRoleValues(payload.scope),
    ...extractRoleValues(payload.scp),
  ]

  const roles = KNOWN_ROLES.filter((role) => roleCandidates.includes(role))
  return roles.length > 0 ? roles : ['ROLE_USER']
}
