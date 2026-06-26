import { zodResolver } from '@hookform/resolvers/zod'
import { useEffect, useMemo } from 'react'
import { useForm, useWatch } from 'react-hook-form'
import { Button } from '../../../components/ui/Button'
import { Input } from '../../../components/ui/Input'
import { Select } from '../../../components/ui/Select'
import type { OrganizationResponseDto } from '../../../types/organization'
import type { SourceResponseDto } from '../../../types/source'
import type { ReanalyzeReviewsParams } from '../../../types/review'
import {
  getDefaultReanalyzeValues,
  mapReanalyzeFormToParams,
  MAX_REANALYZE_LIMIT,
  reanalyzeReviewsSchema,
  type ReanalyzeReviewsFormInput,
  type ReanalyzeReviewsFormValues,
} from '../reanalyze'

interface ReanalyzeReviewsDialogProps {
  initialOrganizationId?: number
  isLoading?: boolean
  onClose: () => void
  onSubmit: (params: ReanalyzeReviewsParams) => void
  open: boolean
  organizations: OrganizationResponseDto[]
  sources: SourceResponseDto[]
}

export function ReanalyzeReviewsDialog({
  initialOrganizationId,
  isLoading,
  onClose,
  onSubmit,
  open,
  organizations,
  sources,
}: ReanalyzeReviewsDialogProps) {
  const validInitialOrganizationId = organizations.some(
    (organization) => organization.id === initialOrganizationId,
  )
    ? initialOrganizationId
    : undefined

  const form = useForm<ReanalyzeReviewsFormInput, unknown, ReanalyzeReviewsFormValues>({
    resolver: zodResolver(reanalyzeReviewsSchema),
    defaultValues: getDefaultReanalyzeValues(validInitialOrganizationId),
  })

  const watchedOrganizationId = useWatch({
    control: form.control,
    name: 'organizationId',
  })
  const selectedOrganizationId =
    typeof watchedOrganizationId === 'number'
      ? watchedOrganizationId
      : watchedOrganizationId
        ? Number(watchedOrganizationId)
        : undefined

  const filteredSources = useMemo(() => {
    if (!selectedOrganizationId) {
      return sources
    }

    return sources.filter((source) => source.organization.id === selectedOrganizationId)
  }, [selectedOrganizationId, sources])

  useEffect(() => {
    if (!open) return

    form.reset(getDefaultReanalyzeValues(validInitialOrganizationId))
  }, [form, open, validInitialOrganizationId])

  useEffect(() => {
    const currentSourceId = form.getValues('sourceId')
    if (!currentSourceId) return
    const normalizedSourceId =
      typeof currentSourceId === 'number' ? currentSourceId : Number(currentSourceId)

    const sourceExists = filteredSources.some((source) => source.id === normalizedSourceId)
    if (sourceExists) return

    form.setValue('sourceId', undefined, { shouldValidate: true })
  }, [filteredSources, form])

  if (!open) return null

  const hasOrganizations = organizations.length > 0

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/30 p-4 backdrop-blur-sm">
      <div className="surface-card w-full max-w-lg p-6">
        <div className="space-y-2">
          <h3 className="text-lg font-semibold text-slate-950">Повторить анализ</h3>
          <p className="text-sm text-slate-600">
            Повторно отправьте отзывы со статусом ошибки анализа.
          </p>
        </div>

        {!hasOrganizations ? (
          <div className="mt-6 rounded-2xl border border-slate-200 bg-slate-50 p-4 text-sm text-slate-600">
            Сначала создайте организацию
          </div>
        ) : (
          <form
            onSubmit={form.handleSubmit((values) => onSubmit(mapReanalyzeFormToParams(values)))}
            className="mt-6 space-y-4"
          >
            <Select
              label="Организация"
              options={organizations.map((organization) => ({
                value: String(organization.id),
                label: `${organization.shortName} — ${organization.name}`,
              }))}
              placeholder="Все организации"
              error={form.formState.errors.organizationId?.message}
              {...form.register('organizationId')}
            />

            <Select
              label="Источник"
              options={filteredSources.map((source) => ({
                value: String(source.id),
                label: source.name,
              }))}
              placeholder="Все источники"
              error={form.formState.errors.sourceId?.message}
              disabled={filteredSources.length === 0}
              {...form.register('sourceId')}
            />

            <Input
              label="Лимит"
              type="number"
              min={1}
              max={MAX_REANALYZE_LIMIT}
              error={form.formState.errors.limit?.message}
              {...form.register('limit')}
            />

            <label className="flex items-start gap-3 rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
              <input
                type="checkbox"
                className="mt-1 h-4 w-4 rounded border-slate-300 text-slate-950 focus:ring-slate-300"
                {...form.register('force')}
              />
              <span className="space-y-1">
                <span className="block text-sm font-medium text-slate-700">Force-анализ</span>
              </span>
            </label>

            <div className="flex justify-end gap-3 pt-2">
              <Button type="button" variant="ghost" onClick={onClose} disabled={isLoading}>
                Отмена
              </Button>
              <Button type="submit" isLoading={isLoading}>
                Запустить
              </Button>
            </div>
          </form>
        )}

        {!hasOrganizations && (
          <div className="mt-6 flex justify-end">
            <Button type="button" variant="ghost" onClick={onClose}>
              Закрыть
            </Button>
          </div>
        )}
      </div>
    </div>
  )
}
