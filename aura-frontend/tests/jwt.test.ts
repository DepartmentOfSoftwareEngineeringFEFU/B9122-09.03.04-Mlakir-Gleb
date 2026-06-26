import test from 'node:test'
import assert from 'node:assert/strict'
import { getRolesFromAccessToken } from '../src/lib/jwt.js'

function createToken(payload: object) {
  const encoded = Buffer.from(JSON.stringify(payload), 'utf8').toString('base64url')
  return `header.${encoded}.signature`
}

test('getRolesFromAccessToken extracts a known role from roles array', () => {
  const token = createToken({ roles: ['ROLE_ADMIN'] })

  assert.deepEqual(getRolesFromAccessToken(token), ['ROLE_ADMIN'])
})

test('getRolesFromAccessToken falls back to ROLE_USER for malformed payload', () => {
  assert.deepEqual(getRolesFromAccessToken('broken-token'), ['ROLE_USER'])
})

test('getRolesFromAccessToken supports scope-like strings', () => {
  const token = createToken({ scope: 'openid ROLE_USER profile' })

  assert.deepEqual(getRolesFromAccessToken(token), ['ROLE_USER'])
})
