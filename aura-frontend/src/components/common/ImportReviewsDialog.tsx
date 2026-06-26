import { useId, useState } from 'react'
import { Button } from '../ui/Button'

interface ImportReviewsDialogProps {
  open: boolean
  sourceName: string
  isLoading?: boolean
  onClose: () => void
  onSubmit: (file: File) => void
}

const csvExample = `externalId,text,authorName,publishedAt,originalUrl,rating
1,"Отличные преподаватели и хороший кампус","Иван","2026-04-01T12:00:00Z","",5
2,"В общежитии грязно и неудобно","Мария","2026-04-02T15:30:00Z","",2`

export function ImportReviewsDialog({
  open,
  sourceName,
  isLoading,
  onClose,
  onSubmit,
}: ImportReviewsDialogProps) {
  const inputId = useId()
  const [file, setFile] = useState<File | null>(null)
  const [error, setError] = useState<string | null>(null)

  if (!open) return null

  const handleSubmit = () => {
    if (!file) {
      setError('Выберите CSV-файл для импорта')
      return
    }

    setError(null)
    onSubmit(file)
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 p-4 backdrop-blur-sm">
      <div className="surface-card w-full max-w-2xl p-6">
        <div className="space-y-2">
          <h3 className="text-lg font-semibold text-slate-950">Импортировать отзывы</h3>
          <p className="text-sm text-slate-600">
            Источник: {sourceName}. Загрузите CSV-файл с отзывами для ручного импорта.
          </p>
        </div>

        <div className="mt-6 space-y-5">
          <div className="space-y-2">
            <label htmlFor={inputId} className="text-sm font-medium text-slate-700">
              CSV-файл
            </label>
            <input
              id={inputId}
              type="file"
              accept=".csv,text/csv"
              className="field-base file:mr-4 file:rounded-xl file:border-0 file:bg-slate-950 file:px-3 file:py-2 file:text-sm file:font-semibold file:text-white"
              onChange={(event) => {
                const nextFile = event.target.files?.[0] ?? null
                setFile(nextFile)
                setError(null)
              }}
            />
            <p className="text-xs text-slate-500">
              Обязательные колонки: `externalId`, `text`, `publishedAt`.
            </p>
            <p className="text-xs text-slate-500">
              Дополнительно поддерживаются: `authorName`, `originalUrl`, `rating`.
            </p>
            {file && <p className="text-sm text-slate-600">Выбран файл: {file.name}</p>}
            {error && <p className="text-sm text-red-600">{error}</p>}
          </div>

          <div className="rounded-2xl border border-slate-200 bg-slate-50 p-4">
            <p className="text-sm font-medium text-slate-800">Формат CSV</p>
            <pre className="mt-3 overflow-x-auto whitespace-pre-wrap text-xs leading-6 text-slate-600">
              {csvExample}
            </pre>
          </div>
        </div>

        <div className="mt-6 flex justify-end gap-3">
          <Button variant="ghost" onClick={onClose} disabled={isLoading}>
            Отмена
          </Button>
          <Button onClick={handleSubmit} isLoading={isLoading}>
            Загрузить
          </Button>
        </div>
      </div>
    </div>
  )
}
