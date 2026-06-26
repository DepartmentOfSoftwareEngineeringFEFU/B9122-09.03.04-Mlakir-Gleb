import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect } from 'react'
import { useForm } from 'react-hook-form'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { Select } from '../../../components/ui/Select'
import { Textarea } from '../../../components/ui/Textarea'
import {
  getUpdateOrganizationFormValues,
  updateOrganizationSchema,
  type UpdateOrganizationFormInput,
  type UpdateOrganizationFormValues,
} from '../form'
import type { OrganizationResponseDto } from '../../../types/organization'

const statusOptions = [
  { value: 'true', label: 'Активна' },
  { value: 'false', label: 'Отключена' },
]

export function OrganizationEditDialog({
  isLoading,
  onClose,
  onSubmit,
  organization,
}: {
  isLoading: boolean
  onClose: () => void
  onSubmit: (values: UpdateOrganizationFormValues) => void
  organization: OrganizationResponseDto
}) {
  const form = useForm<
    UpdateOrganizationFormInput,
    unknown,
    UpdateOrganizationFormValues
  >({
    resolver: zodResolver(updateOrganizationSchema),
    defaultValues: getUpdateOrganizationFormValues(organization),
  })

  useEffect(() => {
    form.reset(getUpdateOrganizationFormValues(organization))
  }, [form, organization])

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 p-4 backdrop-blur-sm">
      <Card className="w-full max-w-3xl p-6 lg:p-8">
        <div className="space-y-2">
          <h2 className="text-xl font-semibold text-slate-950">Редактирование организации</h2>
          <p className="text-sm leading-6 text-slate-600">
            Обновите основные данные организации и сохраните изменения.
          </p>
        </div>

        <form onSubmit={form.handleSubmit(onSubmit)} className="mt-6 space-y-5">
          <div className="grid gap-5 md:grid-cols-2">
            <Input
              label="Название"
              placeholder="Например, Дальневосточный федеральный университет"
              error={form.formState.errors.name?.message}
              {...form.register('name')}
            />
            <Input
              label="Краткое название"
              placeholder="Например, ДВФУ"
              error={form.formState.errors.shortName?.message}
              {...form.register('shortName')}
            />
          </div>

          <div className="grid gap-5 md:grid-cols-2">
            <Input
              label="Сайт"
              placeholder="https://example.org"
              error={form.formState.errors.website?.message}
              {...form.register('website')}
            />
            <Select
              label="Статус"
              options={statusOptions}
              error={form.formState.errors.isActive?.message}
              {...form.register('isActive')}
            />
          </div>

          <Textarea
            label="Описание"
            placeholder="Краткое описание организации"
            error={form.formState.errors.description?.message}
            {...form.register('description')}
          />

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
