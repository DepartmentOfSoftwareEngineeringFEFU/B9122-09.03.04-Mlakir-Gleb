import test from 'node:test'
import assert from 'node:assert/strict'
import {
  getAuthRoles,
  selectIsAuthenticated,
} from '../src/features/auth/access.js'
import { hasRequiredRole } from '../src/features/auth/roles.js'

test('hasRequiredRole allows access when no role restriction is passed', () => {
  assert.equal(hasRequiredRole(['ROLE_USER'], undefined), true)
})

test('hasRequiredRole checks intersection with allowed roles', () => {
  assert.equal(hasRequiredRole(['ROLE_USER'], ['ROLE_ADMIN']), false)
  assert.equal(hasRequiredRole(['ROLE_USER', 'ROLE_ADMIN'], ['ROLE_ADMIN']), true)
})

test('selectIsAuthenticated checks whether session has tokens', () => {
  assert.equal(
    selectIsAuthenticated({ accessToken: 'token', refreshToken: null }),
    true,
  )
  assert.equal(
    selectIsAuthenticated({ accessToken: null, refreshToken: 'refresh' }),
    true,
  )
  assert.equal(
    selectIsAuthenticated({ accessToken: null, refreshToken: null }),
    false,
  )
})

test('getAuthRoles derives roles from access token', () => {
  const token = `header.${Buffer.from(JSON.stringify({ roles: ['ROLE_ADMIN'] }), 'utf8').toString('base64url')}.signature`

  assert.deepEqual(getAuthRoles(token), ['ROLE_ADMIN'])
})
