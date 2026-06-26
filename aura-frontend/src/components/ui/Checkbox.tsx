import type { InputHTMLAttributes } from 'react'
import { cn } from '../../lib/cn'

interface CheckboxProps extends InputHTMLAttributes<HTMLInputElement> {
  description?: string
  error?: string
  label?: string
}

export function Checkbox({
  className,
  description,
  error,
  label,
  ...props
}: CheckboxProps) {
  return (
    <label className="flex w-full flex-col gap-2">
      <span className="flex items-start gap-3 rounded-2xl border border-slate-200 bg-white px-4 py-3">
        <input
          type="checkbox"
          className={cn(
            'mt-1 h-4 w-4 rounded border-slate-300 text-blue-600 focus:ring-4 focus:ring-blue-100',
            className,
          )}
          {...props}
        />
        {label && <span className="text-sm font-medium text-slate-700">{label}</span>}
      </span>
      {description && <span className="text-xs text-slate-500">{description}</span>}
      {error && <span className="text-xs text-red-600">{error}</span>}
    </label>
  )
}
