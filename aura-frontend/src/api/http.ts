import axios, {
  AxiosError,
  type InternalAxiosRequestConfig,
} from 'axios'
import toast from 'react-hot-toast'
import type { AuthResponseDto } from '../types/auth'
import { selectIsAuthenticated } from '../features/auth/access'
import { useAuthStore } from '../features/auth/authStore'
import { queryClient } from '../app/queryClient'

const DEFAULT_CORE_URL = 'http://localhost:8081'
const DEFAULT_AUTH_URL = 'http://localhost:8080'

function getBaseUrl(envValue: string | undefined, fallback: string) {
  return envValue && envValue.trim().length > 0 ? envValue : fallback
}

export const CORE_API_BASE_URL = getBaseUrl(
  import.meta.env.VITE_API_BASE_URL,
  DEFAULT_CORE_URL,
)

export const coreClient = axios.create({
  baseURL: CORE_API_BASE_URL,
})

export const authClient = axios.create({
  baseURL: getBaseUrl(import.meta.env.VITE_AUTH_BASE_URL, DEFAULT_AUTH_URL),
})

function attachAuthHeader(config: InternalAxiosRequestConfig) {
  const token = useAuthStore.getState().accessToken
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
}

function shouldAttachAuthHeader(url?: string) {
  return !['/auth/login', '/auth/refresh', '/auth/register'].includes(url ?? '')
}

function handleUnauthorized() {
  const authState = useAuthStore.getState()
  if (!selectIsAuthenticated(authState)) return
  const { logout } = authState
  queryClient.clear()
  logout()
  toast.error('Сессия истекла. Выполните вход снова.')
  if (window.location.pathname !== '/login') {
    window.location.replace('/login')
  }
}

let refreshPromise: Promise<AuthResponseDto | null> | null = null

async function refreshAuthSession() {
  const { refreshToken, updateTokens } = useAuthStore.getState()

  if (!refreshToken) {
    return null
  }

  if (!refreshPromise) {
    refreshPromise = authClient
      .post<AuthResponseDto>('/auth/refresh', { token: refreshToken })
      .then(({ data }) => {
        updateTokens({
          accessToken: data.accessToken,
          refreshToken: data.refreshToken ?? refreshToken,
        })
        return data
      })
      .catch(() => null)
      .finally(() => {
        refreshPromise = null
      })
  }

  return refreshPromise
}

coreClient.interceptors.request.use(attachAuthHeader)
authClient.interceptors.request.use((config) => {
  if (shouldAttachAuthHeader(config.url)) {
    return attachAuthHeader(config)
  }

  return config
})

coreClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const request = error.config as
      | (InternalAxiosRequestConfig & { _retry?: boolean })
      | undefined

    if (error.response?.status === 401 && request && !request._retry) {
      request._retry = true

      const refreshedSession = await refreshAuthSession()

      if (refreshedSession?.accessToken) {
        request.headers.Authorization = `Bearer ${refreshedSession.accessToken}`
        return coreClient(request)
      }
    }

    if (error instanceof AxiosError && error.response?.status === 401) {
      handleUnauthorized()
    }

    return Promise.reject(error)
  },
)
