export type ConnectionMode = 'html' | 'api'
export type HttpMethod = 'GET' | 'POST'
export type AuthType = 'none' | 'api-key' | 'bearer' | 'basic' | 'oauth2'
export type PaginationType = 'none' | 'page-size' | 'offset-limit' | 'cursor'

export interface CustomPlatform {
  id: number
  name: string
  connectionMode: ConnectionMode
  isActive: boolean
  description: string
  updatedAt: string
  htmlConfig: {
    domain: string
    reviewPageUrl: string
    reviewCardSelector: string
    reviewLinkSelector: string
    reviewTitleSelector: string
    reviewTextSelector: string
    reviewAuthorSelector: string
    reviewDateSelector: string
    reviewRatingSelector: string
    reviewProsSelector: string
    reviewConsSelector: string
    nextPageSelector: string
    maxPages: string
    requestDelayMs: string
    loadFullReview: boolean
    stopOnCaptcha: boolean
  }
  apiConfig: {
    baseUrl: string
    reviewsEndpoint: string
    httpMethod: HttpMethod
    authType: AuthType
    tokenHeaderName: string
    tokenValueHint: string
    itemsPath: string
    reviewTextPath: string
    authorPath: string
    datePath: string
    ratingPath: string
    externalIdPath: string
    paginationType: PaginationType
    pageParamName: string
    pageSizeParamName: string
    nextCursorPath: string
    maxReviews: string
    timeoutMs: string
    requestDelayMs: string
  }
}

export type PlatformFormState = Omit<CustomPlatform, 'id' | 'updatedAt'>

export const CUSTOM_PLATFORMS_STORAGE_KEY = 'aura-custom-platforms'

export const connectionModeOptions = [
  { value: 'html', label: 'HTML-скрапинг' },
  { value: 'api', label: 'API-интеграция' },
]

export const authTypeOptions = [
  { value: 'none', label: 'Без авторизации' },
  { value: 'api-key', label: 'API Key' },
  { value: 'bearer', label: 'Bearer Token' },
  { value: 'basic', label: 'Basic Auth' },
  { value: 'oauth2', label: 'OAuth 2.0' },
]

export const httpMethodOptions = [
  { value: 'GET', label: 'GET' },
  { value: 'POST', label: 'POST' },
]

export const paginationTypeOptions = [
  { value: 'none', label: 'Без пагинации' },
  { value: 'page-size', label: 'Page/size' },
  { value: 'offset-limit', label: 'Offset/limit' },
  { value: 'cursor', label: 'Cursor-based' },
]

export const statusOptions = [
  { value: 'true', label: 'Активна' },
  { value: 'false', label: 'Отключена' },
]

export function createDefaultPlatformFormState(): PlatformFormState {
  return {
    name: '',
    connectionMode: 'html',
    isActive: true,
    description: '',
    htmlConfig: {
      domain: '',
      reviewPageUrl: '',
      reviewCardSelector: '.review-card',
      reviewLinkSelector: 'a.review-link',
      reviewTitleSelector: '.review-title',
      reviewTextSelector: '.review-text',
      reviewAuthorSelector: '.review-author',
      reviewDateSelector: '.review-date',
      reviewRatingSelector: '.review-rating',
      reviewProsSelector: '.review-plus',
      reviewConsSelector: '.review-minus',
      nextPageSelector: 'a.next-page',
      maxPages: '100',
      requestDelayMs: '500',
      loadFullReview: false,
      stopOnCaptcha: true,
    },
    apiConfig: {
      baseUrl: '',
      reviewsEndpoint: '/v1/reviews',
      httpMethod: 'GET',
      authType: 'none',
      tokenHeaderName: 'X-API-Key',
      tokenValueHint: '',
      itemsPath: '$.items',
      reviewTextPath: '$.text',
      authorPath: '$.author.name',
      datePath: '$.createdAt',
      ratingPath: '$.rating',
      externalIdPath: '$.id',
      paginationType: 'none',
      pageParamName: 'page',
      pageSizeParamName: 'size',
      nextCursorPath: '$.nextCursor',
      maxReviews: '1000',
      timeoutMs: '5000',
      requestDelayMs: '500',
    },
  }
}

export function createInitialPlatforms(): CustomPlatform[] {
  return [
    {
      id: 1,
      name: 'Отзывы Example University',
      connectionMode: 'html',
      isActive: true,
      description: 'Подключение внешнего сайта отзывов через HTML-селекторы.',
      updatedAt: '2026-06-05T08:30:00Z',
      htmlConfig: {
        domain: 'example-reviews.ru',
        reviewPageUrl: 'https://example-reviews.ru/universities/example',
        reviewCardSelector: '.review-card',
        reviewLinkSelector: 'a.review-link',
        reviewTitleSelector: '.review-title',
        reviewTextSelector: '.review-text',
        reviewAuthorSelector: '.review-author',
        reviewDateSelector: '.review-date',
        reviewRatingSelector: '.review-rating',
        reviewProsSelector: '.review-plus',
        reviewConsSelector: '.review-minus',
        nextPageSelector: 'a.next-page',
        maxPages: '100',
        requestDelayMs: '700',
        loadFullReview: true,
        stopOnCaptcha: true,
      },
      apiConfig: createDefaultPlatformFormState().apiConfig,
    },
    {
      id: 2,
      name: 'Campus Reviews API',
      connectionMode: 'api',
      isActive: true,
      description: 'Интеграция с внешним API отзывов по вузам.',
      updatedAt: '2026-06-06T11:10:00Z',
      htmlConfig: createDefaultPlatformFormState().htmlConfig,
      apiConfig: {
        baseUrl: 'https://api.campus-reviews.io',
        reviewsEndpoint: '/v1/reviews',
        httpMethod: 'GET',
        authType: 'bearer',
        tokenHeaderName: 'Authorization',
        tokenValueHint: 'Bearer ***',
        itemsPath: '$.data.items',
        reviewTextPath: '$.text',
        authorPath: '$.author.name',
        datePath: '$.publishedAt',
        ratingPath: '$.rating',
        externalIdPath: '$.id',
        paginationType: 'page-size',
        pageParamName: 'page',
        pageSizeParamName: 'size',
        nextCursorPath: '$.meta.nextCursor',
        maxReviews: '1500',
        timeoutMs: '6000',
        requestDelayMs: '400',
      },
    },
  ]
}

export function loadPlatformsFromStorage() {
  if (typeof window === 'undefined') return createInitialPlatforms()

  const value = window.localStorage.getItem(CUSTOM_PLATFORMS_STORAGE_KEY)
  if (!value) return createInitialPlatforms()

  try {
    const parsed = JSON.parse(value) as CustomPlatform[]
    return parsed.length > 0 ? parsed : createInitialPlatforms()
  } catch {
    return createInitialPlatforms()
  }
}

export function savePlatformsToStorage(platforms: CustomPlatform[]) {
  if (typeof window === 'undefined') return
  window.localStorage.setItem(CUSTOM_PLATFORMS_STORAGE_KEY, JSON.stringify(platforms))
}

export function getConnectionModeLabel(mode: ConnectionMode) {
  return mode === 'html' ? 'HTML-скрапинг' : 'API-интеграция'
}

export function getPlatformEndpointLabel(platform: CustomPlatform) {
  return platform.connectionMode === 'html'
    ? platform.htmlConfig.reviewPageUrl || '—'
    : `${platform.apiConfig.baseUrl}${platform.apiConfig.reviewsEndpoint}`
}

export function getPlatformFormState(platform: CustomPlatform): PlatformFormState {
  return {
    name: platform.name,
    connectionMode: platform.connectionMode,
    isActive: platform.isActive,
    description: platform.description,
    htmlConfig: { ...platform.htmlConfig },
    apiConfig: { ...platform.apiConfig },
  }
}

export function createPlatform(values: PlatformFormState) {
  const platforms = loadPlatformsFromStorage()
  const platform: CustomPlatform = {
    id: platforms.reduce((max, item) => Math.max(max, item.id), 0) + 1,
    updatedAt: new Date().toISOString(),
    ...values,
    htmlConfig: { ...values.htmlConfig },
    apiConfig: { ...values.apiConfig },
  }

  const next = [platform, ...platforms]
  savePlatformsToStorage(next)
  return platform
}

export function updatePlatform(id: number, values: PlatformFormState) {
  const platforms = loadPlatformsFromStorage()
  const next = platforms.map((platform) =>
    platform.id === id
      ? {
          ...platform,
          ...values,
          htmlConfig: { ...values.htmlConfig },
          apiConfig: { ...values.apiConfig },
          updatedAt: new Date().toISOString(),
        }
      : platform,
  )

  savePlatformsToStorage(next)
  return next
}
