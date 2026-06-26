import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import toast from 'react-hot-toast'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { authApi } from '../api/authApi'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { Input } from '../components/ui/Input'
import { useAuthStore } from '../features/auth/authStore'

const registerSchema = z
  .object({
    login: z.string().min(3, 'Минимум 3 символа'),
    password: z.string().min(6, 'Минимум 6 символов'),
    confirmPassword: z.string().min(1, 'Подтвердите пароль'),
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: 'Пароли не совпадают',
    path: ['confirmPassword'],
  })

type RegisterFormValues = z.infer<typeof registerSchema>

export function RegisterPage() {
  const navigate = useNavigate()
  const setSession = useAuthStore((state) => state.setSession)

  const form = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      login: '',
      password: '',
      confirmPassword: '',
    },
  })

  const registerMutation = useMutation({
    mutationFn: authApi.register,
    onSuccess: (data, variables) => {
      setSession({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken ?? null,
        login: variables.login,
      })
      toast.success('Аккаунт создан')
      navigate('/dashboard', { replace: true })
    },
    onError: () => {
      toast.error('Не удалось зарегистрироваться')
    },
  })

  const onSubmit = ({ login, password }: RegisterFormValues) => {
    registerMutation.mutate({ login, password })
  }

  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <Card className="w-full max-w-md p-8 lg:p-10">
        <h2 className="text-3xl font-semibold tracking-tight text-slate-950">Регистрация</h2>

        <form onSubmit={form.handleSubmit(onSubmit)} className="mt-8 space-y-5">
          <Input
            label="Логин"
            placeholder="Введите логин"
            autoComplete="username"
            error={form.formState.errors.login?.message}
            {...form.register('login')}
          />
          <Input
            label="Пароль"
            type="password"
            placeholder="Введите пароль"
            autoComplete="new-password"
            error={form.formState.errors.password?.message}
            {...form.register('password')}
          />
          <Input
            label="Повторите пароль"
            type="password"
            placeholder="Повторите пароль"
            autoComplete="new-password"
            error={form.formState.errors.confirmPassword?.message}
            {...form.register('confirmPassword')}
          />
          <Button
            type="submit"
            className="w-full"
            isLoading={registerMutation.isPending}
          >
            Создать аккаунт
          </Button>
        </form>
        <p className="mt-6 text-sm text-slate-500">
          Уже есть аккаунт?{' '}
          <Link to="/login" className="font-medium text-slate-900">
            Войти
          </Link>
        </p>
      </Card>
    </div>
  )
}
