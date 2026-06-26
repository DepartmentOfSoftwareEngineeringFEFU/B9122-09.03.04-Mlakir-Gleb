import { Link } from 'react-router-dom'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'

export function NotFoundPage() {
  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <Card className="max-w-lg p-8 text-center">
        <p className="text-xs font-semibold uppercase tracking-[0.22em] text-blue-600">
          404
        </p>
        <h1 className="mt-2 text-3xl font-semibold text-slate-950">Страница не найдена</h1>
        <Link to="/dashboard" className="mt-6 inline-flex">
          <Button>Перейти в приложение</Button>
        </Link>
      </Card>
    </div>
  )
}
