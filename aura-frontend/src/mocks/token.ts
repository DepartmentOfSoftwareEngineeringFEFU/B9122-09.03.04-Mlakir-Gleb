import type { UserRole } from '../types/auth'

function toBase64Url(value: string) {
  if (typeof globalThis.btoa === 'function') {
    return globalThis
      .btoa(value)
      .replace(/\+/g, '-')
      .replace(/\//g, '_')
      .replace(/=+$/g, '')
  }

  const buffer = (
    globalThis as typeof globalThis & {
      Buffer?: {
        from: (input: string, encoding: string) => { toString: (format: string) => string }
      }
    }
  ).Buffer

  return buffer
    ?.from(value, 'utf8')
    .toString('base64')
    .replace(/\+/g, '-')
    .replace(/\//g, '_')
    .replace(/=+$/g, '')
    ?? value
}

export function createMockAccessToken(login: string, role: UserRole) {
  return [
    toBase64Url(JSON.stringify({ alg: 'none', typ: 'JWT' })),
    toBase64Url(JSON.stringify({ sub: login, roles: [role] })),
    'mock-signature',
  ].join('.')
}
