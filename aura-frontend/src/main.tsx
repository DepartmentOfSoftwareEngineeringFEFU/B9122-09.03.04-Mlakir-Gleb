import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClientProvider } from '@tanstack/react-query'
import { Toaster } from 'react-hot-toast'
import App from './App'
import { queryClient } from './app/queryClient'
import { applyAppModeAttributes } from './config/appMode'
import './index.css'

applyAppModeAttributes()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <App />
      <Toaster
        position="top-right"
        toastOptions={{
          duration: 3500,
          className: 'rounded-2xl border border-slate-200 bg-white text-slate-900 shadow-lg',
        }}
      />
    </QueryClientProvider>
  </StrictMode>,
)
