import type { SourceResponseDto } from '../../types/source'

export const DEFAULT_SCHEDULE_INTERVAL_MINUTES = 1440

export const scheduleIntervalOptions = [
  { label: '15 минут', value: 15 },
  { label: '30 минут', value: 30 },
  { label: '1 час', value: 60 },
  { label: '6 часов', value: 360 },
  { label: '12 часов', value: 720 },
  { label: '1 день', value: 1440 },
  { label: '1 неделя', value: 10080 },
] as const

const scheduleIntervalLabelMap: Record<number, string> = Object.fromEntries(
  scheduleIntervalOptions.map((item) => [item.value, item.label]),
)

export function formatScheduleIntervalMinutes(value?: number | null) {
  if (value == null) return '—'
  return scheduleIntervalLabelMap[value] ?? `${value} мин`
}

export function getSourceScheduleStatusLabel(scheduleEnabled: boolean) {
  return scheduleEnabled ? 'Автосбор включён' : 'Автосбор выключен'
}

export function getSourceScheduleFormHint(scheduleEnabled: boolean) {
  return scheduleEnabled
    ? 'Источник будет автоматически собирать новые отзывы по расписанию. Ручной запуск также останется доступен.'
    : 'Источник можно запускать вручную в любой момент.'
}

export function getDefaultScheduleInterval(
  source?: Pick<SourceResponseDto, 'scheduleIntervalMinutes'> | null,
) {
  return source?.scheduleIntervalMinutes ?? DEFAULT_SCHEDULE_INTERVAL_MINUTES
}
