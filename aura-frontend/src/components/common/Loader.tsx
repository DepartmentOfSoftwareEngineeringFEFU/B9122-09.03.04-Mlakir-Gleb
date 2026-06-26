export function Loader({ label = 'Загрузка данных...' }: { label?: string }) {
  return (
    <div className="surface-card flex min-h-56 flex-col items-center justify-center gap-4 p-10 text-center">
      <div className="h-10 w-10 animate-spin rounded-full border-4 border-blue-100 border-t-blue-600" />
      <div className="space-y-1">
        <p className="text-sm font-medium text-slate-900">Подготавливаем данные</p>
        <p className="text-sm text-slate-600">{label}</p>
      </div>
    </div>
  )
}
