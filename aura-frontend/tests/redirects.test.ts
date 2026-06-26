import test from 'node:test'
import assert from 'node:assert/strict'
import {
  getAuthPageRedirectPath,
  getRootRedirectPath,
} from '../src/app/redirects.js'

test('getRootRedirectPath sends guests to login and users to dashboard', () => {
  assert.equal(getRootRedirectPath(false), '/login')
  assert.equal(getRootRedirectPath(true), '/dashboard')
})

test('getAuthPageRedirectPath redirects only authenticated users', () => {
  assert.equal(getAuthPageRedirectPath(true), '/dashboard')
  assert.equal(getAuthPageRedirectPath(false), null)
})
