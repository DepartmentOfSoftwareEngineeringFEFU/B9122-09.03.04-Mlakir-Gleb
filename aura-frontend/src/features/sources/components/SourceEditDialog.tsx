import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Checkbox } from '../../../components/ui/Checkbox'
import { Input } from '../../../components/ui/Input'
import { Select } from '../../../components/ui/Select'
import { Textarea } from '../../../components/ui/Textarea'
import {
  getUpdateSourceFormValues,
  mapUpdateSourceFormToDto,
  updateSourceSchema,
  type UpdateSourceFormInput,
  type UpdateSourceFormValues,
} from '../form'
import {
  DEFAULT_SCHEDULE_INTERVAL_MINUTES,
  scheduleIntervalOptions,
} from '../schedule.js'
import type { SourceResponseDto, UpdateSourceRequestDto } from '../../../types/source'

interface SourceEditDialogProps {
  isLoading?: boolean
  onClose: () => void
  onSubmit: (payload: { id: number; data: UpdateSourceRequestDto }) => void
  organizationOptions: Array<{ value: string; label: string }>
  source: SourceResponseDto
}

const statusOptions = [
  { value: 'true', label: 'Активен' },
  { value: 'false', label: 'Отключен' },
]

function formatSourceTypeLabel(type: SourceResponseDto['type']) {
  switch (type) {
    case 'TABITURIENT':
      return 'Tabiturient'
    case 'OTZOVIK':
      return 'Otzovik'
    case 'VUZOPEDIA':
      return 'Vuzopedia'
    default:
      return 'Ручной импорт'
  }
}

export function SourceEditDialog({
  isLoading,
  onClose,
  onSubmit,
  organizationOptions,
  source,
}: SourceEditDialogProps) {
  const form = useForm<UpdateSourceFormInput, unknown, UpdateSourceFormValues>({
    resolver: zodResolver(updateSourceSchema),
    defaultValues: getUpdateSourceFormValues(source),
  })
  const scheduleEnabled = useWatch({
    control: form.control,
    name: 'scheduleEnabled',
  })

  useEffect(() => {
    form.reset(getUpdateSourceFormValues(source))
  }, [form, source])

  useEffect(() => {
    if (scheduleEnabled) {
      const interval = form.getValues('scheduleIntervalMinutes')
      if (interval == null) {
        form.setValue('scheduleIntervalMinutes', DEFAULT_SCHEDULE_INTERVAL_MINUTES, {
          shouldDirty: true,
          shouldValidate: true,
        })
      }
      return
    }

    if (form.getValues('scheduleIntervalMinutes') !== null) {
      form.setValue('scheduleIntervalMinutes', null, {
        shouldDirty: true,
        shouldValidate: true,
      })
    }
  }, [form, scheduleEnabled])

  const handleSubmit = (values: UpdateSourceFormValues) => {
    onSubmit({
      id: source.id,
      data: mapUpdateSourceFormToDto(values),
    })
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 p-4 backdrop-blur-sm">
      <Card className="w-full max-w-3xl p-6 lg:p-8">
        <div className="space-y-2">
          <h2 className="text-xl font-semibold text-slate-950">Редактирование источника</h2>
          <p className="text-sm leading-6 text-slate-600">
            Обновите параметры источника и сохраните изменения.
          </p>
        </div>

        <form onSubmit={form.handleSubmit(handleSubmit)} className="mt-6 space-y-5">
          <div className="grid gap-5 md:grid-cols-2">
            <Select
              label="Организация"
              options={organizationOptions}
              error={form.formState.errors.organizationId?.message}
              {...form.register('organizationId')}
            />
            <Input
              label="Название"
              placeholder="Например, Отзывы Tabiturient о ДВФУ"
              error={form.formState.errors.name?.message}
              {...form.register('name')}
            />
            <Input
              label="Тип источника"
              value={formatSourceTypeLabel(source.type)}
              readOnly
            />
            <Checkbox
              label="Автоматический сбор"
              error={form.formState.errors.scheduleEnabled?.message}
              checked={Boolean(scheduleEnabled)}
              {...form.register('scheduleEnabled')}
            />
          </div>

          {source.type === 'MANUAL_IMPORT' ? (
            <Input
              label="Адрес источника"
              readOnly
              error={form.formState.errors.baseUrl?.message}
              {...form.register('baseUrl')}
            />
          ) : (
            <Input
              label="Адрес источника"
              placeholder={
                source.type === 'OTZOVIK'
                  ? 'https://otzovik.com/reviews/dalnevostochniy_federalniy_universitet_dvfu/'
                  : source.type === 'VUZOPEDIA'
                    ? 'https://vuzopedia.ru/vuz/3281/otziv'
                  : 'https://tabiturient.ru/vuzu/dvfu/'
              }
              error={form.formState.errors.baseUrl?.message}
              {...form.register('baseUrl')}
            />
          )}

          {scheduleEnabled && (
            <Select
              label="Интервал сбора"
              options={scheduleIntervalOptions.map((option) => ({
                value: String(option.value),
                label: option.label,
              }))}
              error={form.formState.errors.scheduleIntervalMinutes?.message}
              {...form.register('scheduleIntervalMinutes', {
                setValueAs: (value) => (value ? Number(value) : null),
              })}
            />
          )}

          <div className="grid gap-5 md:grid-cols-2">
            <Select
              label="Статус"
              options={statusOptions}
              error={form.formState.errors.isActive?.message}
              {...form.register('isActive')}
            />
            <Textarea
              label="Описание"
              placeholder="Краткое пояснение по источнику и правилам его использования"
              error={form.formState.errors.description?.message}
              {...form.register('description')}
            />
          </div>

          <div className="flex flex-wrap justify-end gap-3 pt-2">
            <Button type="button" variant="ghost" onClick={onClose} disabled={isLoading}>
              Отмена
            </Button>
            <Button type="submit" isLoading={isLoading}>
              Сохранить изменения
            </Button>
          </div>
        </form>
      </Card>
    </div>
  )
}
