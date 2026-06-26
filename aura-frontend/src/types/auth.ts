export type UserRole = 'ROLE_ADMIN' | 'ROLE_USER'

export interface LoginRequestDto {
  login: string
  password: string
}

export interface RegisterRequestDto {
  login: string
  password: string
}

export interface TokenRequestDto {
  token: string
}

export interface AuthResponseDto {
  accessToken: string
  refreshToken?: string | null
}
