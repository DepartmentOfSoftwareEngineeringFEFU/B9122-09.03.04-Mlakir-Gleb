import type { HTMLAttributes, PropsWithChildren } from 'react'
import { cn } from '../../lib/cn'

export function Card({
  children,
  className,
  ...props
}: PropsWithChildren<HTMLAttributes<HTMLDivElement>>) {
  return (
    <div className={cn('surface-card p-6', className)} {...props}>
      {children}
    </div>
  )
}
