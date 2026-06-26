import type { TextareaHTMLAttributes } from 'react'
import { cn } from '../../lib/cn'

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  description?: string
  error?: string
  label?: string
}

export function Textarea({ className, description, error, label, ...props }: TextareaProps) {
  return (
    <label className="flex w-full flex-col gap-2">
      {label && <span className="text-sm font-medium text-slate-700">{label}</span>}
      <textarea
        className={cn(
          'field-base min-h-32 resize-y',
          error && 'border-red-300 focus:ring-red-100',
          className,
        )}
        {...props}
      />
      {description && <span className="text-xs text-slate-500">{description}</span>}
      {error && <span className="text-xs text-red-600">{error}</span>}
    </label>
  )
}
