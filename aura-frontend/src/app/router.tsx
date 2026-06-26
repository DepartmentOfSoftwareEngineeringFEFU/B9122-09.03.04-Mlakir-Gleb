import { Suspense, lazy, type ReactNode } from 'react'
import { Navigate, RouterProvider, createBrowserRouter } from 'react-router-dom'
import { AppShell } from '../components/layout/AppShell'
import { Loader } from '../components/common/Loader'
import { getAuthPageRedirectPath, getRootRedirectPath } from './redirects'
import { selectIsAuthenticated } from '../features/auth/access'
import { ProtectedRoute } from '../features/auth/ProtectedRoute'
import { useAuthStore } from '../features/auth/authStore'

const DashboardPage = lazy(() =>
  import('../pages/DashboardPage').then((module) => ({
    default: module.DashboardPage,
  })),
)
const CustomSourcesPage = lazy(() =>
  import('../pages/CustomSourcesPage').then((module) => ({
    default: module.CustomSourcesPage,
  })),
)
const NewCustomPlatformPage = lazy(() =>
  import('../pages/NewCustomPlatformPage').then((module) => ({
    default: module.NewCustomPlatformPage,
  })),
)
const LoginPage = lazy(() =>
  import('../pages/LoginPage').then((module) => ({
    default: module.LoginPage,
  })),
)
const NewOrganizationPage = lazy(() =>
  import('../pages/NewOrganizationPage').then((module) => ({
    default: module.NewOrganizationPage,
  })),
)
const NewSourcePage = lazy(() =>
  import('../pages/NewSourcePage').then((module) => ({
    default: module.NewSourcePage,
  })),
)
const NotFoundPage = lazy(() =>
  import('../pages/NotFoundPage').then((module) => ({
    default: module.NotFoundPage,
  })),
)
const OrganizationsPage = lazy(() =>
  import('../pages/OrganizationsPage').then((module) => ({
    default: module.OrganizationsPage,
  })),
)
const RegisterPage = lazy(() =>
  import('../pages/RegisterPage').then((module) => ({
    default: module.RegisterPage,
  })),
)
const ReviewDetailsPage = lazy(() =>
  import('../pages/ReviewDetailsPage').then((module) => ({
    default: module.ReviewDetailsPage,
  })),
)
const ReviewsPage = lazy(() =>
  import('../pages/ReviewsPage').then((module) => ({
    default: module.ReviewsPage,
  })),
)
const SourcesPage = lazy(() =>
  import('../pages/SourcesPage').then((module) => ({
    default: module.SourcesPage,
  })),
)

function RouteFallback() {
  return <Loader label="Загрузка страницы..." />
}

function withSuspense(element: ReactNode) {
  return <Suspense fallback={<RouteFallback />}>{element}</Suspense>
}

function RootRedirect() {
  const isAuthenticated = useAuthStore(selectIsAuthenticated)

  return <Navigate to={getRootRedirectPath(isAuthenticated)} replace />
}

function LoginRedirect() {
  const isAuthenticated = useAuthStore(selectIsAuthenticated)
  const redirectPath = getAuthPageRedirectPath(isAuthenticated)

  return redirectPath ? <Navigate to={redirectPath} replace /> : withSuspense(<LoginPage />)
}

function RegisterRedirect() {
  const isAuthenticated = useAuthStore(selectIsAuthenticated)
  const redirectPath = getAuthPageRedirectPath(isAuthenticated)

  return redirectPath ? <Navigate to={redirectPath} replace /> : withSuspense(<RegisterPage />)
}

const router = createBrowserRouter([
  {
    path: '/',
    element: <RootRedirect />,
    errorElement: withSuspense(<NotFoundPage />),
  },
  {
    path: '/login',
    element: <LoginRedirect />,
  },
  {
    path: '/register',
    element: <RegisterRedirect />,
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppShell />,
        children: [
          { path: '/custom-sources', element: withSuspense(<CustomSourcesPage />) },
          { path: '/dashboard', element: withSuspense(<DashboardPage />) },
          { path: '/organizations', element: withSuspense(<OrganizationsPage />) },
          { path: '/reviews', element: withSuspense(<ReviewsPage />) },
          { path: '/reviews/:id', element: withSuspense(<ReviewDetailsPage />) },
        ],
      },
      {
        element: <ProtectedRoute allowedRoles={['ROLE_ADMIN']} />,
        children: [
          {
            element: <AppShell />,
            children: [
              {
                path: '/organizations/new',
                element: withSuspense(<NewOrganizationPage />),
              },
              {
                path: '/custom-sources/new',
                element: withSuspense(<NewCustomPlatformPage />),
              },
              { path: '/sources', element: withSuspense(<SourcesPage />) },
              { path: '/sources/new', element: withSuspense(<NewSourcePage />) },
            ],
          },
        ],
      },
    ],
  },
  {
    path: '*',
    element: withSuspense(<NotFoundPage />),
  },
])

export function AppRouter() {
  return <RouterProvider router={router} />
}
