import { useEffect } from 'react'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm, useWatch } from 'react-hook-form'
import toast from 'react-hot-toast'
import { useNavigate } from 'react-router-dom'
import {
  createSourceSchema,
  defaultCreateSourceValues,
  mapCreateSourceFormToDto,
  type CreateSourceFormInput,
  type CreateSourceFormValues,
} from './form'
import {
  DEFAULT_SCHEDULE_INTERVAL_MINUTES,
} from './schedule.js'
import { getSourceMutationErrorMessage } from './errors'
import { useCreateSourceMutation } from './queries'

export function useNewSourceForm() {
  const navigate = useNavigate()
  const createSourceMutation = useCreateSourceMutation()
  const form = useForm<CreateSourceFormInput, unknown, CreateSourceFormValues>({
    resolver: zodResolver(createSourceSchema),
    defaultValues: defaultCreateSourceValues,
  })
  const selectedType = useWatch({
    control: form.control,
    name: 'type',
  })
  const scheduleEnabled = useWatch({
    control: form.control,
    name: 'scheduleEnabled',
  })

  useEffect(() => {
    if (form.getValues('baseUrl').trim() === '') {
      form.setValue('baseUrl', '', {
        shouldDirty: false,
        shouldValidate: false,
      })
    }
  }, [form, selectedType])

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

  const onSubmit = (values: CreateSourceFormValues) => {
    createSourceMutation.mutate(mapCreateSourceFormToDto(values), {
      onSuccess: () => {
        toast.success('Источник успешно создан')
        navigate('/sources')
      },
      onError: (error) => {
        toast.error(
          getSourceMutationErrorMessage(error, {
            action: 'create',
            sourceType: selectedType,
          }),
        )
      },
    })
  }

  return {
    createSourceMutation,
    form,
    onSubmit,
    scheduleEnabled,
    selectedType,
  }
}
