import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { Button } from '../ui/Button'
import { authApi } from '../../api/authApi'
import { queryClient } from '../../app/queryClient'
import { IS_MOCKUP_UI_MODE } from '../../config/appMode'
import { useAuthStore } from '../../features/auth/authStore'
import { useAuthRoles, useHasRole } from '../../features/auth/access'
import { cn } from '../../lib/cn'
import { formatUserRole } from '../../lib/format'

const navItems = [
  { to: '/dashboard', label: 'Дашборд', adminOnly: false },
  { to: '/organizations', label: 'Организации', adminOnly: false },
  {
    to: '/custom-sources',
    label: 'Платформы',
    adminOnly: false,
    badge: IS_MOCKUP_UI_MODE ? undefined : 'Beta',
  },
  { to: '/sources', label: 'Источники', adminOnly: true },
  { to: '/reviews', label: 'Отзывы', adminOnly: false },
]

export function AppShell() {
  const navigate = useNavigate()
  const login = useAuthStore((state) => state.login)
  const roles = useAuthRoles()
  const refreshToken = useAuthStore((state) => state.refreshToken)
  const logout = useAuthStore((state) => state.logout)
  const isAdmin = useHasRole('ROLE_ADMIN')

  const handleLogout = async () => {
    if (refreshToken) {
      try {
        await authApi.logout()
      } catch {
        // Local logout should still complete even if the backend is unavailable.
      }
    }

    queryClient.clear()
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <div className="min-h-screen lg:grid lg:h-screen lg:grid-cols-[272px_1fr] lg:overflow-hidden">
      <aside className="app-shell-panel border-b border-slate-200 bg-white px-5 py-5 lg:h-screen lg:overflow-y-auto lg:border-b-0 lg:border-r">
        <div className="surface-card flex h-full flex-col gap-8 p-5">
          <div className="space-y-3">
            <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-slate-950 text-sm font-semibold text-white">
              <svg viewBox="0 0 24 24" fill="none" className="h-5 w-5" aria-hidden="true">
                <path
                  d="M7 8.75h10M7 12h6.5M9.5 16H7.75c-.97 0-1.75-.78-1.75-1.75v-7.5C6 5.78 6.78 5 7.75 5h8.5C17.22 5 18 5.78 18 6.75v7.5c0 .97-.78 1.75-1.75 1.75H14l-3.5 3V16Z"
                  stroke="currentColor"
                  strokeWidth="1.8"
                  strokeLinecap="round"
                  strokeLinejoin="round"
                />
              </svg>
            </div>
            <div className="space-y-1">
              <p className="text-sm font-semibold text-slate-950">Aura</p>
              <p className="text-xs leading-5 text-slate-500">
                Аналитика отзывов и управление источниками
              </p>
            </div>
          </div>

          <nav className="grid gap-2">
            {navItems
              .filter((item) => !item.adminOnly || isAdmin)
              .map((item) => (
              <NavLink
                key={item.to}
                to={item.to}
                className={({ isActive }) =>
                  cn(
                    'rounded-2xl px-4 py-3 text-sm font-medium transition',
                    isActive
                      ? 'bg-slate-950 text-white shadow-lg'
                      : 'text-slate-600 hover:bg-slate-100 hover:text-slate-950',
                  )
                }
              >
                <span className="flex items-center justify-between gap-3">
                  <span>{item.label}</span>
                  {item.badge ? (
                    <span
                      className={cn(
                        'rounded-full px-2 py-0.5 text-[10px] font-semibold uppercase tracking-[0.12em]',
                        'bg-amber-100 text-amber-800',
                        item.to === '/dashboard' ? '' : '',
                      )}
                    >
                      {item.badge}
                    </span>
                  ) : null}
                </span>
              </NavLink>
            ))}
          </nav>

          <div className="mt-auto rounded-2xl bg-slate-50 p-4">
            <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Аккаунт</p>
            <p className="mt-2 text-sm font-semibold text-slate-950">{login ?? 'Администратор'}</p>
            <p className="mt-1 text-xs text-slate-500">
              {formatUserRole(roles.includes('ROLE_ADMIN') ? 'ROLE_ADMIN' : 'ROLE_USER')}
            </p>
            <Button variant="ghost" className="mt-4 w-full" onClick={handleLogout}>
              Выйти
            </Button>
          </div>
        </div>
      </aside>

      <div className="min-w-0 lg:flex lg:h-screen lg:flex-col lg:overflow-hidden">
        <main className="flex w-full min-w-0 flex-col gap-5 px-4 py-5 lg:min-h-0 lg:flex-1 lg:overflow-y-auto lg:px-8 lg:py-6">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
