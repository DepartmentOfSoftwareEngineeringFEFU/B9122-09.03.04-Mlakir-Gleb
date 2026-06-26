import { create } from 'zustand'
import { createJSONStorage, persist } from 'zustand/middleware'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  login: string | null
  setSession: (payload: {
    accessToken: string
    refreshToken?: string | null
    login: string
  }) => void
  updateTokens: (payload: {
    accessToken: string
    refreshToken?: string | null
  }) => void
  logout: () => void
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      accessToken: null,
      refreshToken: null,
      login: null,
      setSession: ({ accessToken, refreshToken = null, login }) =>
        set({
          accessToken,
          refreshToken,
          login,
        }),
      updateTokens: ({ accessToken, refreshToken = null }) =>
        set((state) => ({
          ...state,
          accessToken,
          refreshToken,
        })),
      logout: () =>
        set({
          accessToken: null,
          refreshToken: null,
          login: null,
        }),
    }),
    {
      name: 'aura-auth',
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({
        accessToken: state.accessToken,
        refreshToken: state.refreshToken,
        login: state.login,
      }),
    },
  ),
)
