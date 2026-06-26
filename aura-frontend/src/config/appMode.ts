export type AppDataMode = 'real' | 'mock'
export type AppUiMode = 'app' | 'mockup'

function getDataMode(value: string | undefined): AppDataMode {
  return value === 'mock' ? 'mock' : 'real'
}

function getUiMode(value: string | undefined): AppUiMode {
  return value === 'mockup' ? 'mockup' : 'app'
}

export const APP_DATA_MODE = getDataMode(import.meta.env.VITE_DATA_MODE)
export const APP_UI_MODE = getUiMode(import.meta.env.VITE_UI_MODE)

export const IS_MOCK_DATA_MODE = APP_DATA_MODE === 'mock'
export const IS_MOCKUP_UI_MODE = APP_UI_MODE === 'mockup'

export function applyAppModeAttributes() {
  if (typeof document === 'undefined') return

  document.documentElement.dataset.dataMode = APP_DATA_MODE
  document.documentElement.dataset.uiMode = APP_UI_MODE
}
