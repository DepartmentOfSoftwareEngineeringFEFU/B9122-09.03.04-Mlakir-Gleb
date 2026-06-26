import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { create } from 'zustand'

vi.mock('./authStore', () => {
  const useAuthStore = create<{
    accessToken: string | null
    refreshToken: string | null
    login: string | null
  }>(() => ({
    accessToken: null,
    refreshToken: null,
    login: null,
  }))

  return { useAuthStore }
})

import { ProtectedRoute } from './ProtectedRoute'
import { useAuthStore } from './authStore'

function createToken(payload: object) {
  const encoded = window.btoa(JSON.stringify(payload)).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/g, '')
  return `header.${encoded}.signature`
}

function LoginPageProbe() {
  const location = useLocation()
  const fromPath =
    typeof location.state === 'object' &&
    location.state !== null &&
    'from' in location.state &&
    typeof location.state.from === 'object' &&
    location.state.from !== null &&
    'pathname' in location.state.from
      ? String(location.state.from.pathname)
      : ''

  return (
    <div>
      <span>Login page</span>
      <span data-testid="from-path">{fromPath}</span>
    </div>
  )
}

afterEach(() => {
  useAuthStore.setState({
    accessToken: null,
    refreshToken: null,
    login: null,
  })
})

describe('ProtectedRoute', () => {
  it('redirects guests to /login and preserves the origin route', () => {
    render(
      <MemoryRouter initialEntries={['/reviews']}>
        <Routes>
          <Route path="/login" element={<LoginPageProbe />} />
          <Route element={<ProtectedRoute />}>
            <Route path="/reviews" element={<div>Reviews page</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByText('Login page')).toBeInTheDocument()
    expect(screen.getByTestId('from-path')).toHaveTextContent('/reviews')
  })

  it('redirects authenticated users without required role to /dashboard', () => {
    useAuthStore.setState({
      accessToken: createToken({ roles: ['ROLE_USER'] }),
      refreshToken: null,
      login: 'user@example.com',
    })

    render(
      <MemoryRouter initialEntries={['/sources']}>
        <Routes>
          <Route path="/dashboard" element={<div>Dashboard page</div>} />
          <Route element={<ProtectedRoute allowedRoles={['ROLE_ADMIN']} />}>
            <Route path="/sources" element={<div>Sources page</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByText('Dashboard page')).toBeInTheDocument()
  })

  it('renders nested content for users with the required role', () => {
    useAuthStore.setState({
      accessToken: createToken({ roles: ['ROLE_ADMIN'] }),
      refreshToken: null,
      login: 'admin@example.com',
    })

    render(
      <MemoryRouter initialEntries={['/sources']}>
        <Routes>
          <Route path="/dashboard" element={<div>Dashboard page</div>} />
          <Route element={<ProtectedRoute allowedRoles={['ROLE_ADMIN']} />}>
            <Route path="/sources" element={<div>Sources page</div>} />
          </Route>
        </Routes>
      </MemoryRouter>,
    )

    expect(screen.getByText('Sources page')).toBeInTheDocument()
  })
})
