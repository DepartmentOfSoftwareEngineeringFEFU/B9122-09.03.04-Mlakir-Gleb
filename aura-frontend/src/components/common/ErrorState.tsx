import { Card } from '../ui/Card'
import { Button } from '../ui/Button'

interface ErrorStateProps {
  title?: string
  description?: string
  onRetry?: () => void
}

export function ErrorState({
  title = 'Не удалось загрузить данные',
  description = 'Проверьте доступность сервисов и повторите попытку.',
  onRetry,
}: ErrorStateProps) {
  return (
    <Card className="flex flex-col gap-5 p-8 lg:p-10">
      <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-rose-50 text-rose-700 ring-1 ring-rose-100">
        !
      </div>
      <div className="space-y-2">
        <h3 className="text-xl font-semibold text-slate-950">{title}</h3>
        <p className="max-w-2xl text-sm leading-6 text-slate-600">{description}</p>
      </div>
      {onRetry && (
        <Button variant="secondary" onClick={onRetry} className="self-start">
          Повторить
        </Button>
      )}
    </Card>
  )
}
