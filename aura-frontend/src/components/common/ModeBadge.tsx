import { IS_MOCKUP_UI_MODE } from '../../config/appMode'

export function ModeBadge() {
  if (!IS_MOCKUP_UI_MODE) return null

  return (
    <div className="pointer-events-none fixed right-3 top-3 z-50">
      <span className="rounded border border-slate-400 bg-white px-2 py-1 text-[11px] font-medium uppercase tracking-[0.12em] text-slate-700">
        Макет
      </span>
    </div>
  )
}
