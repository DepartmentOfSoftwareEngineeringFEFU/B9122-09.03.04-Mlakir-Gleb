import {
  collectionModeLabelMap,
  sentimentLabelMap,
  sourceTypeLabelMap,
  topicLabelMap,
} from './constants'
import type { UserRole } from '../types/auth'
import type { CollectionMode, SourceType } from '../types/source'
import type { CollectionJobStatus } from '../types/collection'
import type { ReviewSentiment, ReviewStatus, ReviewTopic } from '../types/review'

const dateTimeFormatter = new Intl.DateTimeFormat('ru-RU', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

const dateFormatter = new Intl.DateTimeFormat('ru-RU', {
  dateStyle: 'medium',
})

export function formatDateTime(value?: string | null) {
  if (!value) return '—'
  return dateTimeFormatter.format(new Date(value))
}

export function formatDate(value?: string | null) {
  if (!value) return '—'
  return dateFormatter.format(new Date(value))
}

export function formatSourceType(value: SourceType) {
  return sourceTypeLabelMap[value] ?? value
}

export function formatCollectionMode(value: CollectionMode) {
  return collectionModeLabelMap[value] ?? value
}

export function formatScheduleIntervalMinutes(value?: number | null) {
  if (value == null) return '—'

  switch (value) {
    case 15:
      return '15 минут'
    case 30:
      return '30 минут'
    case 60:
      return '1 час'
    case 360:
      return '6 часов'
    case 720:
      return '12 часов'
    case 1440:
      return '1 день'
    case 10080:
      return '1 неделя'
    default:
      return `${value} мин`
  }
}

export function formatCollectionJobStatus(value: CollectionJobStatus) {
  switch (value) {
    case 'RUNNING':
      return 'Выполняется'
    case 'SUCCESS':
      return 'Завершено'
    case 'FAILED':
      return 'Ошибка'
    default:
      return value
  }
}

export function formatUserRole(value?: UserRole | null) {
  if (!value) return 'Пользователь'

  switch (value) {
    case 'ROLE_ADMIN':
      return 'Администратор'
    case 'ROLE_USER':
      return 'Пользователь'
    default:
      return value
  }
}

export function formatSentiment(value?: ReviewSentiment | null) {
  if (!value) return 'Не определено'
  return sentimentLabelMap[value] ?? value
}

export function formatTopic(value?: ReviewTopic | null) {
  if (!value) return 'Не определена'
  return topicLabelMap[value] ?? value
}

export function formatReviewStatus(value: ReviewStatus) {
  switch (value) {
    case 'NEW':
      return 'Новый'
    case 'ANALYSIS_PENDING':
      return 'Ожидает анализа'
    case 'ANALYZED':
      return 'Проанализирован'
    case 'FAILED_ANALYSIS':
      return 'Ошибка анализа'
    default:
      return value
  }
}

export function formatPercent(value?: number | null) {
  if (value == null) return '—'
  return `${Math.round(value * 100)}%`
}

export function truncateText(value: string, maxLength = 140) {
  if (value.length <= maxLength) return value
  return `${value.slice(0, maxLength).trimEnd()}…`
}
