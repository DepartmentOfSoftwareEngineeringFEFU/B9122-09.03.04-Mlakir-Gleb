import type { ReactNode } from 'react'
import { Button } from '../ui/Button'
import { Card } from '../ui/Card'

interface EmptyStateProps {
  title: string
  description: string
  actionLabel?: string
  onAction?: () => void
  icon?: ReactNode
}

export function EmptyState({
  title,
  description,
  actionLabel,
  onAction,
  icon,
}: EmptyStateProps) {
  return (
    <Card className="flex flex-col items-start gap-5 p-8 lg:p-10">
      <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-50 text-blue-700 ring-1 ring-blue-100">
        {icon ?? <span className="text-lg">◎</span>}
      </div>
      <div className="space-y-2">
        <h3 className="text-xl font-semibold text-slate-950">{title}</h3>
        <p className="max-w-2xl text-sm leading-6 text-slate-600">{description}</p>
      </div>
      {actionLabel && onAction && (
        <Button variant="secondary" onClick={onAction}>
          {actionLabel}
        </Button>
      )}
    </Card>
  )
}
