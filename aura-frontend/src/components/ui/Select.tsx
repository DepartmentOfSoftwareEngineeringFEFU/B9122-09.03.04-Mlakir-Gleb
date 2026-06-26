import type { SelectHTMLAttributes } from 'react'
import { cn } from '../../lib/cn'

interface Option {
  value: string
  label: string
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  description?: string
  error?: string
  label?: string
  options: Option[]
  placeholder?: string
}

export function Select({
  className,
  description,
  error,
  label,
  options,
  placeholder,
  ...props
}: SelectProps) {
  return (
    <label className="flex w-full flex-col gap-2">
      {label && <span className="text-sm font-medium text-slate-700">{label}</span>}
      <select
        className={cn('field-base appearance-none', error && 'border-red-300 focus:ring-red-100', className)}
        {...props}
      >
        {placeholder && <option value="">{placeholder}</option>}
        {options.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      {description && <span className="text-xs text-slate-500">{description}</span>}
      {error && <span className="text-xs text-red-600">{error}</span>}
    </label>
  )
}
