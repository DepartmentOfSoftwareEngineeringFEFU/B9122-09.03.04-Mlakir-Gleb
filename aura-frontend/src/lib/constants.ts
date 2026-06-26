import type { CollectionMode, SourceType } from '../types/source'
import type { ReviewSentiment, ReviewTopic } from '../types/review'

export const sourceTypeOptions: Array<{ value: SourceType; label: string }> = [
  { value: 'MANUAL_IMPORT', label: 'Ручной импорт' },
  { value: 'TABITURIENT', label: 'Tabiturient' },
  { value: 'OTZOVIK', label: 'Otzovik' },
  { value: 'VUZOPEDIA', label: 'Vuzopedia' },
]

export const collectionModeOptions: Array<{
  value: CollectionMode
  label: string
}> = [
  { value: 'MANUAL', label: 'По запросу' },
  { value: 'SCHEDULED', label: 'По расписанию' },
]

export const sentimentOptions: Array<{
  value: ReviewSentiment
  label: string
}> = [
  { value: 'POSITIVE', label: 'Позитивный' },
  { value: 'NEUTRAL', label: 'Нейтральный' },
  { value: 'NEGATIVE', label: 'Негативный' },
]

export const topicOptions: Array<{ value: ReviewTopic; label: string }> = [
  { value: 'EDUCATION', label: 'Обучение' },
  { value: 'TEACHERS', label: 'Преподаватели' },
  { value: 'INFRASTRUCTURE', label: 'Инфраструктура' },
  { value: 'DORMITORY', label: 'Общежитие' },
  { value: 'ADMINISTRATION', label: 'Администрация' },
  { value: 'STUDENT_LIFE', label: 'Студенческая жизнь' },
  { value: 'OTHER', label: 'Другое' },
]

export const sourceTypeLabelMap = Object.fromEntries(
  sourceTypeOptions.map((item) => [item.value, item.label]),
) as Record<SourceType, string>

export const collectionModeLabelMap = Object.fromEntries(
  collectionModeOptions.map((item) => [item.value, item.label]),
) as Record<CollectionMode, string>

export const sentimentLabelMap = Object.fromEntries(
  sentimentOptions.map((item) => [item.value, item.label]),
) as Record<ReviewSentiment, string>

export const topicLabelMap = Object.fromEntries(
  topicOptions.map((item) => [item.value, item.label]),
) as Record<ReviewTopic, string>
