import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import toast from 'react-hot-toast'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { z } from 'zod'
import { authApi } from '../api/authApi'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { Input } from '../components/ui/Input'
import { IS_MOCK_DATA_MODE } from '../config/appMode'
import { useAuthStore } from '../features/auth/authStore'

const loginSchema = z.object({
  login: z.string().min(1, 'Введите логин'),
  password: z.string().min(1, 'Введите пароль'),
})

type LoginFormValues = z.infer<typeof loginSchema>

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const setSession = useAuthStore((state) => state.setSession)

  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      login: '',
      password: '',
    },
  })

  const loginMutation = useMutation({
    mutationFn: authApi.login,
    onSuccess: (data, variables) => {
      setSession({
        accessToken: data.accessToken,
        refreshToken: data.refreshToken ?? null,
        login: variables.login,
      })
      toast.success('Вход выполнен')
      const redirectTo =
        (location.state as { from?: { pathname?: string } } | null)?.from?.pathname ??
        '/dashboard'
      navigate(redirectTo, { replace: true })
    },
    onError: () => {
      toast.error('Не удалось выполнить вход. Проверьте логин и пароль.')
    },
  })

  const onSubmit = (values: LoginFormValues) => {
    loginMutation.mutate(values)
  }

  return (
    <div className="flex min-h-screen items-center justify-center p-4">
      <Card className="w-full max-w-md p-8 lg:p-10">
        <h2 className="text-3xl font-semibold tracking-tight text-slate-950">Вход</h2>

        <form onSubmit={form.handleSubmit(onSubmit)} className="mt-8 space-y-5">
          <Input
            label="Логин"
            placeholder="Введите логин"
            autoComplete="username"
            error={form.formState.errors.login?.message}
            defaultValue={IS_MOCK_DATA_MODE ? 'demo-admin' : undefined}
            {...form.register('login')}
          />
          <Input
            label="Пароль"
            type="password"
            placeholder="Введите пароль"
            autoComplete="current-password"
            error={form.formState.errors.password?.message}
            defaultValue={IS_MOCK_DATA_MODE ? 'demo123' : undefined}
            {...form.register('password')}
          />
          <Button
            type="submit"
            className="w-full"
            isLoading={loginMutation.isPending}
          >
            Войти
          </Button>
        </form>
        <p className="mt-6 text-sm text-slate-500">
          Нет аккаунта?{' '}
          <Link to="/register" className="font-medium text-slate-900">
            Зарегистрироваться
          </Link>
        </p>
      </Card>
    </div>
  )
}
