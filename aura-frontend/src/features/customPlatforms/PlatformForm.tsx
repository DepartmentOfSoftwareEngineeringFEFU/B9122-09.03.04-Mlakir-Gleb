import { Button } from '../../components/ui/Button'
import { Checkbox } from '../../components/ui/Checkbox'
import { Input } from '../../components/ui/Input'
import { Select } from '../../components/ui/Select'
import { Textarea } from '../../components/ui/Textarea'
import { cn } from '../../lib/cn'
import {
  authTypeOptions,
  connectionModeOptions,
  httpMethodOptions,
  paginationTypeOptions,
  statusOptions,
  type AuthType,
  type ConnectionMode,
  type HttpMethod,
  type PaginationType,
  type PlatformFormState,
} from './model'

export function PlatformForm({
  value,
  submitLabel,
  cancelLabel,
  onCancel,
  onChange,
  onSubmit,
}: {
  value: PlatformFormState
  submitLabel: string
  cancelLabel?: string
  onCancel?: () => void
  onChange: (value: PlatformFormState) => void
  onSubmit: () => void
}) {
  const updateCommonField = <K extends keyof PlatformFormState>(
    key: K,
    nextValue: PlatformFormState[K],
  ) => {
    onChange({
      ...value,
      [key]: nextValue,
    })
  }

  const updateHtmlField = <K extends keyof PlatformFormState['htmlConfig']>(
    key: K,
    nextValue: PlatformFormState['htmlConfig'][K],
  ) => {
    onChange({
      ...value,
      htmlConfig: {
        ...value.htmlConfig,
        [key]: nextValue,
      },
    })
  }

  const updateApiField = <K extends keyof PlatformFormState['apiConfig']>(
    key: K,
    nextValue: PlatformFormState['apiConfig'][K],
  ) => {
    onChange({
      ...value,
      apiConfig: {
        ...value.apiConfig,
        [key]: nextValue,
      },
    })
  }

  return (
    <div className="space-y-6">
      <section className="space-y-4">
        <h3 className="text-lg font-semibold text-slate-950">Основные настройки</h3>
        <div className="grid gap-4 md:grid-cols-2">
          <Input
            label="Название платформы"
            placeholder="Например: Example Reviews"
            value={value.name}
            onChange={(event) => updateCommonField('name', event.target.value)}
          />
          <Select
            label="Способ подключения"
            options={connectionModeOptions}
            value={value.connectionMode}
            onChange={(event) =>
              updateCommonField('connectionMode', event.target.value as ConnectionMode)
            }
          />
        </div>
        <Textarea
          label="Описание"
          placeholder="Краткое описание платформы и особенностей подключения"
          value={value.description}
          onChange={(event) => updateCommonField('description', event.target.value)}
        />
      </section>

      {value.connectionMode === 'html' ? (
        <HtmlPlatformFields value={value} onChange={updateHtmlField} />
      ) : (
        <ApiPlatformFields value={value} onChange={updateApiField} />
      )}

      <section className="space-y-4">
        <h3 className="text-lg font-semibold text-slate-950">Статус</h3>
        <div className="grid gap-4 md:grid-cols-2">
          <Select
            label="Статус платформы"
            options={statusOptions}
            value={String(value.isActive)}
            onChange={(event) => updateCommonField('isActive', event.target.value === 'true')}
          />
        </div>
      </section>

      <div className={cn('flex flex-wrap gap-3', onCancel ? 'justify-end' : '')}>
        {onCancel && cancelLabel ? (
          <Button type="button" variant="ghost" onClick={onCancel}>
            {cancelLabel}
          </Button>
        ) : null}
        <Button type="button" onClick={onSubmit}>
          {submitLabel}
        </Button>
      </div>
    </div>
  )
}

function HtmlPlatformFields({
  value,
  onChange,
}: {
  value: PlatformFormState
  onChange: <K extends keyof PlatformFormState['htmlConfig']>(
    key: K,
    nextValue: PlatformFormState['htmlConfig'][K],
  ) => void
}) {
  return (
    <>
      <section className="space-y-4">
        <h3 className="text-lg font-semibold text-slate-950">HTML-источник</h3>
        <div className="grid gap-4 md:grid-cols-2">
          <Input
            label="Домен сайта"
            placeholder="example.com"
            value={value.htmlConfig.domain}
            onChange={(event) => onChange('domain', event.target.value)}
          />
          <Input
            label="URL страницы отзывов"
            placeholder="https://example.com/company/reviews"
            value={value.htmlConfig.reviewPageUrl}
            onChange={(event) => onChange('reviewPageUrl', event.target.value)}
          />
        </div>
      </section>

      <section className="space-y-4">
        <h3 className="text-lg font-semibold text-slate-950">CSS-селекторы</h3>
        <div className="grid gap-4 md:grid-cols-2">
          <Input
            label="Селектор карточки отзыва"
            placeholder=".review-card"
            value={value.htmlConfig.reviewCardSelector}
            onChange={(event) => onChange('reviewCardSelector', event.target.value)}
          />
          <Input
            label="Селектор ссылки на полный отзыв"
            placeholder="a.review-link"
            value={value.htmlConfig.reviewLinkSelector}
            onChange={(event) => onChange('reviewLinkSelector', event.target.value)}
          />
          <Input
            label="Селектор заголовка"
            placeholder=".review-title"
            value={value.htmlConfig.reviewTitleSelector}
            onChange={(event) => onChange('reviewTitleSelector', event.target.value)}
          />
          <Input
            label="Селектор текста отзыва"
            placeholder=".review-text"
            value={value.htmlConfig.reviewTextSelector}
            onChange={(event) => onChange('reviewTextSelector', event.target.value)}
          />
          <Input
            label="Селектор автора"
            placeholder=".review-author"
            value={value.htmlConfig.reviewAuthorSelector}
            onChange={(event) => onChange('reviewAuthorSelector', event.target.value)}
          />
          <Input
            label="Селектор даты"
            placeholder=".review-date"
            value={value.htmlConfig.reviewDateSelector}
            onChange={(event) => onChange('reviewDateSelector', event.target.value)}
          />
          <Input
            label="Селектор рейтинга"
            placeholder=".review-rating"
            value={value.htmlConfig.reviewRatingSelector}
            onChange={(event) => onChange('reviewRatingSelector', event.target.value)}
          />
          <Input
            label="Селектор достоинств"
            placeholder=".review-plus"
            value={value.htmlConfig.reviewProsSelector}
            onChange={(event) => onChange('reviewProsSelector', event.target.value)}
          />
          <Input
            label="Селектор недостатков"
            placeholder=".review-minus"
            value={value.htmlConfig.reviewConsSelector}
            onChange={(event) => onChange('reviewConsSelector', event.target.value)}
          />
          <Input
            label="Селектор следующей страницы"
            placeholder="a.next-page"
            value={value.htmlConfig.nextPageSelector}
            onChange={(event) => onChange('nextPageSelector', event.target.value)}
          />
        </div>
      </section>

      <section className="space-y-4">
        <h3 className="text-lg font-semibold text-slate-950">Ограничения и поведение</h3>
        <div className="grid gap-4 md:grid-cols-2">
          <Checkbox
            checked={value.htmlConfig.loadFullReview}
            label="Загружать полный текст отзыва со страницы детали"
            onChange={(event) => onChange('loadFullReview', event.target.checked)}
          />
          <Checkbox
            checked={value.htmlConfig.stopOnCaptcha}
            label="Останавливать сбор при обнаружении капчи"
            onChange={(event) => onChange('stopOnCaptcha', event.target.checked)}
          />
          <Input
            type="number"
            label="Максимум страниц за один запуск"
            placeholder="100"
            value={value.htmlConfig.maxPages}
            onChange={(event) => onChange('maxPages', event.target.value)}
          />
          <Input
            type="number"
            label="Задержка между запросами, мс"
            placeholder="500"
            value={value.htmlConfig.requestDelayMs}
            onChange={(event) => onChange('requestDelayMs', event.target.value)}
          />
        </div>
      </section>
    </>
  )
}

function ApiPlatformFields({
  value,
  onChange,
}: {
  value: PlatformFormState
  onChange: <K extends keyof PlatformFormState['apiConfig']>(
    key: K,
    nextValue: PlatformFormState['apiConfig'][K],
  ) => void
}) {
  return (
    <>
      <section className="space-y-4">
        <h3 className="text-lg font-semibold text-slate-950">API-источник</h3>
        <div className="grid gap-4 md:grid-cols-2">
          <Input
            label="Базовый URL API"
            placeholder="https://api.example.com"
            value={value.apiConfig.baseUrl}
            onChange={(event) => onChange('baseUrl', event.target.value)}
          />
          <Input
            label="Endpoint списка отзывов"
            placeholder="/v1/reviews"
            value={value.apiConfig.reviewsEndpoint}
            onChange={(event) => onChange('reviewsEndpoint', event.target.value)}
          />
          <Select
            label="HTTP method"
            options={httpMethodOptions}
            value={value.apiConfig.httpMethod}
            onChange={(event) => onChange('httpMethod', event.target.value as HttpMethod)}
          />
        </div>
      </section>

      <section className="space-y-4">
        <h3 className="text-lg font-semibold text-slate-950">Авторизация</h3>
        <div className="grid gap-4 md:grid-cols-2">
          <Select
            label="Тип авторизации"
            options={authTypeOptions}
            value={value.apiConfig.authType}
            onChange={(event) => onChange('authType', event.target.value as AuthType)}
          />
          <Input
            label="Header для API key"
            placeholder="X-API-Key"
            value={value.apiConfig.tokenHeaderName}
            onChange={(event) => onChange('tokenHeaderName', event.target.value)}
          />
          <Input
            label="Token / ключ"
            placeholder="Будет храниться на backend в зашифрованном виде"
            value={value.apiConfig.tokenValueHint}
            onChange={(event) => onChange('tokenValueHint', event.target.value)}
          />
        </div>
      </section>

      <section className="space-y-4">
        <h3 className="text-lg font-semibold text-slate-950">Маппинг JSON</h3>
        <div className="grid gap-4 md:grid-cols-2">
          <Input
            label="JSON path до массива отзывов"
            placeholder="$.items"
            value={value.apiConfig.itemsPath}
            onChange={(event) => onChange('itemsPath', event.target.value)}
          />
          <Input
            label="JSON path до текста отзыва"
            placeholder="$.text"
            value={value.apiConfig.reviewTextPath}
            onChange={(event) => onChange('reviewTextPath', event.target.value)}
          />
          <Input
            label="JSON path до автора"
            placeholder="$.author.name"
            value={value.apiConfig.authorPath}
            onChange={(event) => onChange('authorPath', event.target.value)}
          />
          <Input
            label="JSON path до даты"
            placeholder="$.createdAt"
            value={value.apiConfig.datePath}
            onChange={(event) => onChange('datePath', event.target.value)}
          />
          <Input
            label="JSON path до рейтинга"
            placeholder="$.rating"
            value={value.apiConfig.ratingPath}
            onChange={(event) => onChange('ratingPath', event.target.value)}
          />
          <Input
            label="JSON path до внешнего ID"
            placeholder="$.id"
            value={value.apiConfig.externalIdPath}
            onChange={(event) => onChange('externalIdPath', event.target.value)}
          />
        </div>
      </section>

      <section className="space-y-4">
        <h3 className="text-lg font-semibold text-slate-950">Пагинация</h3>
        <div className="grid gap-4 md:grid-cols-2">
          <Select
            label="Тип пагинации"
            options={paginationTypeOptions}
            value={value.apiConfig.paginationType}
            onChange={(event) =>
              onChange('paginationType', event.target.value as PaginationType)
            }
          />
          <Input
            label="Параметр страницы"
            placeholder="page"
            value={value.apiConfig.pageParamName}
            onChange={(event) => onChange('pageParamName', event.target.value)}
          />
          <Input
            label="Параметр размера страницы"
            placeholder="size"
            value={value.apiConfig.pageSizeParamName}
            onChange={(event) => onChange('pageSizeParamName', event.target.value)}
          />
          <Input
            label="JSON path до следующего cursor"
            placeholder="$.nextCursor"
            value={value.apiConfig.nextCursorPath}
            onChange={(event) => onChange('nextCursorPath', event.target.value)}
          />
        </div>
      </section>

      <section className="space-y-4">
        <h3 className="text-lg font-semibold text-slate-950">Ограничения</h3>
        <div className="grid gap-4 md:grid-cols-3">
          <Input
            type="number"
            label="Максимум отзывов за запуск"
            placeholder="1000"
            value={value.apiConfig.maxReviews}
            onChange={(event) => onChange('maxReviews', event.target.value)}
          />
          <Input
            type="number"
            label="Таймаут запроса, мс"
            placeholder="5000"
            value={value.apiConfig.timeoutMs}
            onChange={(event) => onChange('timeoutMs', event.target.value)}
          />
          <Input
            type="number"
            label="Задержка между запросами, мс"
            placeholder="500"
            value={value.apiConfig.requestDelayMs}
            onChange={(event) => onChange('requestDelayMs', event.target.value)}
          />
        </div>
      </section>
    </>
  )
}
