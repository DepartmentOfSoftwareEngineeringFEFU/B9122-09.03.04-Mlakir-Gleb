import { authClient } from './http'
import { IS_MOCK_DATA_MODE } from '../config/appMode'
import { mockAuthApi } from '../mocks/mockApi'
import type {
  AuthResponseDto,
  LoginRequestDto,
  RegisterRequestDto,
  TokenRequestDto,
} from '../types/auth'

const realAuthApi = {
  login: async (payload: LoginRequestDto) => {
    const { data } = await authClient.post<AuthResponseDto>('/auth/login', payload)
    return data
  },
  register: async (payload: RegisterRequestDto) => {
    const { data } = await authClient.post<AuthResponseDto>('/auth/register', payload)
    return data
  },
  refresh: async (payload: TokenRequestDto) => {
    const { data } = await authClient.post<AuthResponseDto>('/auth/refresh', payload)
    return data
  },
  logout: async () => {
    await authClient.post('/auth/logout')
  },
}

export const authApi = IS_MOCK_DATA_MODE ? mockAuthApi : realAuthApi
