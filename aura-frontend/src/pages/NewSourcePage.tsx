import { Link, useNavigate } from 'react-router-dom'
import type { UseFormRegisterReturn } from 'react-hook-form'
import { EmptyState } from '../components/common/EmptyState'
import { PageHeader } from '../components/common/PageHeader'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { Checkbox } from '../components/ui/Checkbox'
import { Input } from '../components/ui/Input'
import { Textarea } from '../components/ui/Textarea'
import { Select } from '../components/ui/Select'
import { useOrganizationsQuery } from '../features/organizations/queries'
import {
  scheduleIntervalOptions,
} from '../features/sources/schedule.js'
import { useNewSourceForm } from '../features/sources/useNewSourceForm'
import { sourceTypeOptions } from '../lib/constants'

export function NewSourcePage() {
  const navigate = useNavigate()
  const organizationsQuery = useOrganizationsQuery()
  const { createSourceMutation, form, onSubmit, scheduleEnabled, selectedType } =
    useNewSourceForm()

  if (organizationsQuery.isLoading) {
    return <Card className="p-8 text-sm text-slate-600">Загрузка организаций...</Card>
  }

  if (organizationsQuery.isError || !organizationsQuery.data) {
    return (
      <Card className="p-8 text-sm text-slate-600">
        Не удалось загрузить организации. Попробуйте обновить страницу.
      </Card>
    )
  }

  if (organizationsQuery.data.length === 0) {
    return (
      <div className="space-y-6">
        <PageHeader title="Новый источник" />
        <EmptyState
          title="Сначала создайте организацию"
          description="Источник Tabiturient, Otzovik или Vuzopedia должен принадлежать организации."
          actionLabel="Создать организацию"
          onAction={() => navigate('/organizations/new')}
        />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Новый источник" />

      <Card className="max-w-3xl p-6 lg:p-8">
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-5">
          <div className="grid gap-5 md:grid-cols-2">
            <Select
              label="Организация"
              options={organizationsQuery.data.map((organization) => ({
                value: String(organization.id),
                label: `${organization.shortName} — ${organization.name}`,
              }))}
              placeholder="Выберите организацию"
              error={form.formState.errors.organizationId?.message}
              {...form.register('organizationId')}
            />
            <Input
              label="Название"
              placeholder={
                selectedType === 'TABITURIENT'
                  ? 'Например, Отзывы Tabiturient о ДВФУ'
                  : selectedType === 'VUZOPEDIA'
                    ? 'Например, Vuzopedia ДВФУ'
                    : 'Например, Otzovik ДВФУ'
              }
              error={form.formState.errors.name?.message}
              {...form.register('name')}
            />
            <Select
              label="Тип источника"
              options={sourceTypeOptions}
              error={form.formState.errors.type?.message}
              {...form.register('type')}
            />
          </div>

          <SourceBaseUrlField
            selectedType={selectedType}
            error={form.formState.errors.baseUrl?.message}
            register={form.register('baseUrl')}
          />

          <Checkbox
            label="Автоматический сбор"
            error={form.formState.errors.scheduleEnabled?.message}
            checked={Boolean(scheduleEnabled)}
            {...form.register('scheduleEnabled')}
          />

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

          <Textarea
            label="Описание"
            placeholder="Краткое пояснение по источнику и правилам его использования"
            error={form.formState.errors.description?.message}
            {...form.register('description')}
          />

          <div className="flex flex-wrap gap-3 pt-2">
            <Button type="submit" isLoading={createSourceMutation.isPending}>
              Сохранить источник
            </Button>
            <Link to="/sources">
              <Button type="button" variant="ghost">
                Отмена
              </Button>
            </Link>
          </div>
        </form>
      </Card>
    </div>
  )
}

function SourceBaseUrlField({
  selectedType,
  error,
  register,
}: {
  selectedType: 'TABITURIENT' | 'OTZOVIK' | 'VUZOPEDIA'
  error?: string
  register: UseFormRegisterReturn<'baseUrl'>
}) {
  if (selectedType === 'TABITURIENT') {
    return (
      <Input
        label="Адрес источника"
        placeholder="https://tabiturient.ru/vuzu/dvfu/"
        error={error}
        {...register}
      />
    )
  }

  if (selectedType === 'OTZOVIK') {
    return (
      <Input
        label="Адрес источника"
        placeholder="https://otzovik.com/reviews/dalnevostochniy_federalniy_universitet_dvfu/"
        error={error}
        {...register}
      />
    )
  }

  if (selectedType === 'VUZOPEDIA') {
    return (
      <Input
        label="Адрес источника"
        placeholder="https://vuzopedia.ru/vuz/3281/otziv"
        error={error}
        {...register}
      />
    )
  }

  return <Input label="Адрес источника" error={error} {...register} />
}
