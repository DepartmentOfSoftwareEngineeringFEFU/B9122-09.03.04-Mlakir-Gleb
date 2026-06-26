import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import toast from 'react-hot-toast'
import { Link, useNavigate } from 'react-router-dom'
import { PageHeader } from '../components/common/PageHeader'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { Input } from '../components/ui/Input'
import { Textarea } from '../components/ui/Textarea'
import {
  createOrganizationSchema,
  defaultCreateOrganizationValues,
  mapCreateOrganizationFormToDto,
  type CreateOrganizationFormInput,
  type CreateOrganizationFormValues,
} from '../features/organizations/form'
import { getOrganizationMutationErrorMessage } from '../features/organizations/errors'
import { useCreateOrganizationMutation } from '../features/organizations/queries'

export function NewOrganizationPage() {
  const navigate = useNavigate()
  const createOrganizationMutation = useCreateOrganizationMutation()
  const form = useForm<
    CreateOrganizationFormInput,
    unknown,
    CreateOrganizationFormValues
  >({
    resolver: zodResolver(createOrganizationSchema),
    defaultValues: defaultCreateOrganizationValues,
  })

  const onSubmit = (values: CreateOrganizationFormValues) => {
    createOrganizationMutation.mutate(mapCreateOrganizationFormToDto(values), {
      onSuccess: () => {
        toast.success('Организация успешно создана')
        navigate('/organizations')
      },
      onError: (error) => {
        toast.error(getOrganizationMutationErrorMessage(error, 'create'))
      },
    })
  }

  return (
    <div className="space-y-6">
      <PageHeader title="Новая организация" />

      <Card className="max-w-3xl p-6 lg:p-8">
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-5">
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

          <Input
            label="Сайт"
            placeholder="https://example.org"
            error={form.formState.errors.website?.message}
            {...form.register('website')}
          />

          <Textarea
            label="Описание"
            placeholder="Краткое описание организации"
            error={form.formState.errors.description?.message}
            {...form.register('description')}
          />

          <div className="flex flex-wrap gap-3 pt-2">
            <Button type="submit" isLoading={createOrganizationMutation.isPending}>
              Сохранить организацию
            </Button>
            <Link to="/organizations">
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
