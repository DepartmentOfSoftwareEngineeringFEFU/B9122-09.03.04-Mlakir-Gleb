import { forwardRef } from 'react'
import type { InputHTMLAttributes, ReactNode } from 'react'
import { cn } from '../../lib/cn'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  description?: string
  endAdornment?: ReactNode
  error?: string
  label?: string
}

export const Input = forwardRef<HTMLInputElement, InputProps>(function Input(
  { className, description, endAdornment, error, label, ...props },
  ref,
) {
  return (
    <label className="flex w-full flex-col gap-2">
      {label && <span className="text-sm font-medium text-slate-700">{label}</span>}
      <div className="relative">
        <input
          ref={ref}
          className={cn(
            'field-base',
            endAdornment && 'pr-12',
            error && 'border-red-300 focus:ring-red-100',
            className,
          )}
          {...props}
        />
        {endAdornment && (
          <div className="absolute inset-y-0 right-3 flex items-center">
            {endAdornment}
          </div>
        )}
      </div>
      {description && <span className="text-xs text-slate-500">{description}</span>}
      {error && <span className="text-xs text-red-600">{error}</span>}
    </label>
  )
})
