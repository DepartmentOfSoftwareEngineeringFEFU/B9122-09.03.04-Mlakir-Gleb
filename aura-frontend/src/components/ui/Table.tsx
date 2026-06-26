import type { HTMLAttributes, PropsWithChildren, TableHTMLAttributes } from 'react'
import { cn } from '../../lib/cn'

export function TableContainer({
  children,
  className,
}: PropsWithChildren<HTMLAttributes<HTMLDivElement>>) {
  return (
    <div className={cn('table-shell overflow-auto', className)}>
      {children}
    </div>
  )
}

export function Table({
  children,
  className,
  ...props
}: PropsWithChildren<TableHTMLAttributes<HTMLTableElement>>) {
  return (
    <table className={cn('min-w-full divide-y divide-slate-200', className)} {...props}>
      {children}
    </table>
  )
}
