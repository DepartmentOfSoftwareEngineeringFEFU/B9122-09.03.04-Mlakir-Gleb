import type { PropsWithChildren } from 'react'
import { cn } from '../../lib/cn'

type BadgeVariant =
  | 'default'
  | 'positive'
  | 'neutral'
  | 'negative'
  | 'active'
  | 'inactive'
  | 'warning'

const variantClassMap: Record<BadgeVariant, string> = {
  default: 'bg-slate-100 text-slate-700',
  positive: 'bg-emerald-50 text-emerald-700',
  neutral: 'bg-slate-100 text-slate-700',
  negative: 'bg-rose-50 text-rose-700',
  active: 'bg-blue-50 text-blue-700',
  inactive: 'bg-slate-100 text-slate-500',
  warning: 'bg-amber-50 text-amber-700',
}

interface BadgeProps {
  variant?: BadgeVariant
  className?: string
}

export function Badge({
  children,
  variant = 'default',
  className,
}: PropsWithChildren<BadgeProps>) {
  return (
    <span
      className={cn(
        'inline-flex items-center rounded-full px-3 py-1 text-xs font-semibold',
        variantClassMap[variant],
        className,
      )}
    >
      {children}
    </span>
  )
}
