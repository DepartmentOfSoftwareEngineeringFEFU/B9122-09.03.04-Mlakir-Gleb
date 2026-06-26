import { useEffect, useRef } from 'react'
import { Input } from '../ui/Input'

interface DebouncedInputProps {
  className?: string
  label?: string
  placeholder?: string
  value: string
  onCommit: (value: string) => void
}

export function DebouncedInput({
  className,
  label,
  placeholder,
  value,
  onCommit,
}: DebouncedInputProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const timeoutRef = useRef<number | null>(null)

  useEffect(() => {
    if (!inputRef.current || inputRef.current.value === value) return
    inputRef.current.value = value
  }, [value])

  useEffect(() => {
    return () => {
      if (timeoutRef.current !== null) {
        window.clearTimeout(timeoutRef.current)
      }
    }
  }, [])

  return (
    <Input
      ref={inputRef}
      className={className}
      label={label}
      defaultValue={value}
      placeholder={placeholder}
      onChange={(event) => {
        const nextValue = event.target.value.trim()
        if (timeoutRef.current !== null) {
          window.clearTimeout(timeoutRef.current)
        }
        timeoutRef.current = window.setTimeout(() => {
          onCommit(nextValue)
        }, 400)
      }}
    />
  )
}
