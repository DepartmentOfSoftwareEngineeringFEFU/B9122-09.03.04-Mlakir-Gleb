import type { ButtonHTMLAttributes, PropsWithChildren } from 'react'
import { cn } from '../../lib/cn'

type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger'
type ButtonSize = 'md' | 'sm'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: ButtonSize
  isLoading?: boolean
}

const variantClassMap: Record<ButtonVariant, string> = {
  primary:
    'bg-slate-950 text-white hover:bg-slate-800 focus-visible:ring-slate-300',
  secondary:
    'bg-blue-50 text-blue-700 hover:bg-blue-100 focus-visible:ring-blue-200',
  ghost:
    'bg-white text-slate-700 hover:bg-slate-100 focus-visible:ring-slate-200 border border-slate-200',
  danger:
    'bg-red-50 text-red-700 hover:bg-red-100 focus-visible:ring-red-200',
}

const sizeClassMap: Record<ButtonSize, string> = {
  md: 'px-4 py-3 text-sm',
  sm: 'px-3 py-2 text-xs',
}

export function Button({
  children,
  className,
  variant = 'primary',
  size = 'md',
  isLoading = false,
  disabled,
  ...props
}: PropsWithChildren<ButtonProps>) {
  return (
    <button
      className={cn(
        'ui-button inline-flex items-center justify-center gap-2 rounded-2xl font-semibold transition focus-visible:outline-none focus-visible:ring-4 disabled:cursor-not-allowed disabled:opacity-60',
        variantClassMap[variant],
        sizeClassMap[size],
        className,
      )}
      disabled={disabled || isLoading}
      {...props}
    >
      {isLoading && (
        <span className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
      )}
      {children}
    </button>
  )
}
