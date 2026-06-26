import type {
  AuthResponseDto,
  LoginRequestDto,
  RegisterRequestDto,
  TokenRequestDto,
  UserRole,
} from '../types/auth'
import type { CollectionJobResponseDto } from '../types/collection'
import type {
  DashboardCategoryStatDto,
  DashboardFilters,
  DashboardResponseDto,
  DashboardTimelinePointDto,
} from '../types/dashboard'
import type {
  CreateOrganizationRequestDto,
  OrganizationFilters,
  OrganizationInsightsResponseDto,
  OrganizationResponseDto,
  UpdateOrganizationRequestDto,
} from '../types/organization'
import type {
  KeywordStatDto,
  PageResponseDto,
  ReanalyzeReviewsParams,
  ReanalyzeReviewsResponseDto,
  ReviewAnalysisDto,
  ReviewFilters,
  ReviewListItemDto,
  ReviewResponseDto,
  ReviewSentiment,
  ReviewSummaryResponseDto,
  ReviewTopic,
} from '../types/review'
import type {
  CreateSourceRequestDto,
  SourceFilters,
  SourceResponseDto,
  UpdateSourceRequestDto,
} from '../types/source'
import { createMockAccessToken } from './token'

interface MockSession {
  users: Array<{
    login: string
    password: string
    role: UserRole
  }>
  organizations: OrganizationResponseDto[]
  sources: SourceResponseDto[]
  reviews: ReviewResponseDto[]
  jobs: CollectionJobResponseDto[]
}

const STORAGE_KEY = 'aura-mock-db'

function nowIso() {
  return new Date().toISOString()
}

function plusDays(date: Date, days: number) {
  const next = new Date(date)
  next.setDate(next.getDate() + days)
  return next.toISOString()
}

function createAnalysis(
  sentiment: ReviewSentiment,
  topic: ReviewTopic,
  keywords: string[],
  confidence: number,
): ReviewAnalysisDto {
  return {
    sentiment,
    topic,
    keywords,
    confidence,
    modelVersion: 'mock-aura-v1',
    analyzedAt: nowIso(),
  }
}

function createInitialSession(): MockSession {
  const organizations: OrganizationResponseDto[] = [
    {
      id: 1,
      name: 'Дальневосточный федеральный университет',
      shortName: 'ДВФУ',
      description: 'Крупный университет с насыщенной студенческой жизнью.',
      website: 'https://www.dvfu.ru',
      isActive: true,
      createdAt: '2026-01-10T09:00:00Z',
      updatedAt: '2026-05-20T11:30:00Z',
    },
    {
      id: 2,
      name: 'Тихоокеанский государственный университет',
      shortName: 'ТОГУ',
      description: 'Региональный вуз с активной приёмной кампанией.',
      website: 'https://pnu.edu.ru',
      isActive: true,
      createdAt: '2026-01-15T09:00:00Z',
      updatedAt: '2026-05-18T10:00:00Z',
    },
  ]

  const sources: SourceResponseDto[] = [
    {
      id: 1,
      organization: { id: 1, name: organizations[0].name, shortName: organizations[0].shortName },
      name: 'Tabiturient ДВФУ',
      type: 'TABITURIENT',
      baseUrl: 'https://tabiturient.ru/vuzu/dvfu/',
      collectionMode: 'MANUAL',
      scheduleEnabled: false,
      scheduleIntervalMinutes: null,
      lastCollectedAt: '2026-05-25T06:30:00Z',
      nextCollectionAt: null,
      description: 'Основной внешний источник отзывов абитуриентов.',
      createdAt: '2026-01-20T09:00:00Z',
      updatedAt: '2026-05-25T06:30:00Z',
      isActive: true,
    },
    {
      id: 2,
      organization: { id: 2, name: organizations[1].name, shortName: organizations[1].shortName },
      name: 'Otzovik ТОГУ',
      type: 'OTZOVIK',
      baseUrl: 'https://otzovik.com/reviews/pacific_national_university/',
      collectionMode: 'MANUAL',
      scheduleEnabled: false,
      scheduleIntervalMinutes: null,
      lastCollectedAt: '2026-05-19T08:20:00Z',
      nextCollectionAt: null,
      description: 'Публичные отзывы из Otzovik.',
      createdAt: '2026-02-10T09:00:00Z',
      updatedAt: '2026-05-19T08:20:00Z',
      isActive: true,
    },
    {
      id: 3,
      organization: { id: 2, name: organizations[1].name, shortName: organizations[1].shortName },
      name: 'Vuzopedia ТОГУ',
      type: 'VUZOPEDIA',
      baseUrl: 'https://vuzopedia.ru/vuz/412/otziv',
      collectionMode: 'SCHEDULED',
      scheduleEnabled: true,
      scheduleIntervalMinutes: 10080,
      lastCollectedAt: '2026-05-10T05:00:00Z',
      nextCollectionAt: '2026-06-07T05:00:00Z',
      description: 'Еженедельный мониторинг отзывов.',
      createdAt: '2026-02-12T09:00:00Z',
      updatedAt: '2026-05-10T05:00:00Z',
      isActive: true,
    },
  ]

  const reviews: ReviewResponseDto[] = [
    {
      id: 1,
      sourceId: 1,
      sourceName: 'Tabiturient ДВФУ',
      externalId: 'tb-101',
      text: 'Сильные преподаватели, но общежития требуют обновления.',
      authorName: 'Анна',
      rating: 4,
      publishedAt: '2026-03-02T09:00:00Z',
      collectedAt: '2026-03-02T12:00:00Z',
      status: 'ANALYZED',
      analysis: createAnalysis('NEUTRAL', 'DORMITORY', ['общежитие', 'преподаватели'], 0.88),
      originalUrl: 'https://tabiturient.ru/vuzu/dvfu/review-101',
    },
    {
      id: 2,
      sourceId: 1,
      sourceName: 'Tabiturient ДВФУ',
      externalId: 'tb-102',
      text: 'Кампус и инфраструктура отличные, много возможностей для студентов.',
      authorName: 'Илья',
      rating: 5,
      publishedAt: '2026-03-12T10:00:00Z',
      collectedAt: '2026-03-12T12:00:00Z',
      status: 'ANALYZED',
      analysis: createAnalysis('POSITIVE', 'INFRASTRUCTURE', ['кампус', 'инфраструктура'], 0.94),
      originalUrl: 'https://tabiturient.ru/vuzu/dvfu/review-102',
    },
    {
      id: 3,
      sourceId: 2,
      sourceName: 'Otzovik ТОГУ',
      externalId: 'ot-501',
      text: 'Хороший уровень преподавания, но корпусам нужен ремонт.',
      authorName: 'Виктор',
      rating: 4,
      publishedAt: '2026-02-18T08:00:00Z',
      collectedAt: '2026-02-18T10:00:00Z',
      status: 'ANALYZED',
      analysis: createAnalysis('NEUTRAL', 'INFRASTRUCTURE', ['ремонт', 'корпус'], 0.81),
      originalUrl: 'https://otzovik.com/review_501.html',
    },
    {
      id: 4,
      sourceId: 2,
      sourceName: 'Otzovik ТОГУ',
      externalId: 'ot-502',
      text: 'Преподаватели помогают, на кафедре всегда можно получить обратную связь.',
      authorName: 'Светлана',
      rating: 5,
      publishedAt: '2026-03-05T08:00:00Z',
      collectedAt: '2026-03-05T10:00:00Z',
      status: 'ANALYZED',
      analysis: createAnalysis('POSITIVE', 'TEACHERS', ['кафедра', 'преподаватели'], 0.92),
      originalUrl: 'https://otzovik.com/review_502.html',
    },
    {
      id: 5,
      sourceId: 3,
      sourceName: 'Vuzopedia ТОГУ',
      externalId: 'vz-77',
      text: 'Есть интересные направления, но общежитие оказалось переполненным.',
      authorName: 'Никита',
      rating: 3,
      publishedAt: '2026-03-18T14:00:00Z',
      collectedAt: '2026-03-18T16:00:00Z',
      status: 'ANALYZED',
      analysis: createAnalysis('NEGATIVE', 'DORMITORY', ['общежитие', 'места'], 0.9),
      originalUrl: 'https://vuzopedia.ru/review/77',
    },
    {
      id: 6,
      sourceId: 3,
      sourceName: 'Vuzopedia ТОГУ',
      externalId: 'vz-78',
      text: 'Студенческая жизнь живая, часто проходят хакатоны и встречи с работодателями.',
      authorName: 'Диана',
      rating: 5,
      publishedAt: '2026-04-01T14:00:00Z',
      collectedAt: '2026-04-01T16:00:00Z',
      status: 'ANALYZED',
      analysis: createAnalysis('POSITIVE', 'STUDENT_LIFE', ['хакатоны', 'работодатели'], 0.95),
      originalUrl: 'https://vuzopedia.ru/review/78',
    },
    {
      id: 7,
      sourceId: 1,
      sourceName: 'Tabiturient ДВФУ',
      externalId: 'tb-103',
      text: 'Учебные программы сильные, но в административных вопросах много бюрократии.',
      authorName: 'Павел',
      rating: 4,
      publishedAt: '2026-05-04T09:00:00Z',
      collectedAt: '2026-05-04T12:00:00Z',
      status: 'ANALYZED',
      analysis: createAnalysis('NEUTRAL', 'ADMINISTRATION', ['бюрократия', 'программы'], 0.84),
      originalUrl: 'https://tabiturient.ru/vuzu/dvfu/review-103',
    },
    {
      id: 8,
      sourceId: 3,
      sourceName: 'Vuzopedia ТОГУ',
      externalId: 'vz-79',
      text: 'Учиться интересно, но добираться до кампуса неудобно.',
      authorName: 'Алексей',
      rating: 4,
      publishedAt: '2026-05-21T14:00:00Z',
      collectedAt: '2026-05-21T16:00:00Z',
      status: 'ANALYZED',
      analysis: createAnalysis('NEUTRAL', 'INFRASTRUCTURE', ['кампус', 'логистика'], 0.79),
      originalUrl: 'https://vuzopedia.ru/review/79',
    },
  ]

  const jobs: CollectionJobResponseDto[] = [
    {
      id: 1,
      sourceId: 2,
      sourceName: 'Импорт отзывов приёмной комиссии',
      status: 'SUCCESS',
      startedAt: '2026-05-26T02:00:00Z',
      finishedAt: '2026-05-26T02:03:00Z',
      collectedCount: 14,
      triggeredBy: 'demo-admin',
    },
    {
      id: 2,
      sourceId: 4,
      sourceName: 'Vuzopedia ТОГУ',
      status: 'SUCCESS',
      startedAt: '2026-05-10T05:00:00Z',
      finishedAt: '2026-05-10T05:04:00Z',
      collectedCount: 8,
      triggeredBy: 'scheduler',
    },
  ]

  return {
    users: [
      { login: 'demo-admin', password: 'demo123', role: 'ROLE_ADMIN' },
      { login: 'demo-user', password: 'demo123', role: 'ROLE_USER' },
    ],
    organizations,
    sources,
    reviews,
    jobs,
  }
}

class MockDatabase {
  private state: MockSession

  constructor() {
    this.state = this.load()
  }

  private load() {
    if (typeof window === 'undefined') {
      return createInitialSession()
    }

    const saved = window.localStorage.getItem(STORAGE_KEY)
    if (!saved) return createInitialSession()

    try {
      return JSON.parse(saved) as MockSession
    } catch {
      return createInitialSession()
    }
  }

  private save() {
    if (typeof window === 'undefined') return
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(this.state))
  }

  private nextId(items: Array<{ id: number }>) {
    return items.reduce((max, item) => Math.max(max, item.id), 0) + 1
  }

  private getSourceById(sourceId: number) {
    const source = this.state.sources.find((item) => item.id === sourceId)
    if (!source) {
      throw new Error(`Unknown source: ${sourceId}`)
    }
    return source
  }

  private getOrganizationById(organizationId: number) {
    const organization = this.state.organizations.find((item) => item.id === organizationId)
    if (!organization) {
      throw new Error(`Unknown organization: ${organizationId}`)
    }
    return organization
  }

  private getSourceOrganization(sourceId: number) {
    return this.getSourceById(sourceId).organization.id
  }

  private getReviewsForOrganization(organizationId: number) {
    const sourceIds = this.state.sources
      .filter((source) => source.organization.id === organizationId)
      .map((source) => source.id)

    return this.state.reviews.filter((review) => sourceIds.includes(review.sourceId))
  }

  private buildInsightSummary(organizationId: number) {
    const organization = this.getOrganizationById(organizationId)
    const reviews = this.getReviewsForOrganization(organizationId)
    const positive = reviews.filter((review) => review.analysis?.sentiment === 'POSITIVE').length
    const negative = reviews.filter((review) => review.analysis?.sentiment === 'NEGATIVE').length

    return {
      organizationName: organization.shortName,
      summary:
        positive >= negative
          ? `Вокруг ${organization.shortName} преобладает умеренно позитивный фон: чаще отмечают сильных преподавателей, кампус и студенческую среду.`
          : `По ${organization.shortName} заметен смешанный фон: сильные стороны признаются, но часть отзывов указывает на инфраструктурные проблемы.`,
      strengths: ['Сильный преподавательский состав', 'Вовлечённая студенческая среда', 'Узнаваемый бренд вуза'],
      weaknesses: ['Инфраструктура и общежития', 'Скорость административной коммуникации', 'Непрозрачность отдельных процессов'],
      recommendations: ['Усилить коммуникацию с абитуриентами', 'Приоритизировать проблемы кампуса', 'Регулярно обновлять дашборд по ключевым темам'],
    }
  }

  login(payload: LoginRequestDto): AuthResponseDto {
    const user = this.state.users.find(
      (item) => item.login === payload.login && item.password === payload.password,
    )
    const resolvedUser = user ?? this.state.users[0]

    return {
      accessToken: createMockAccessToken(resolvedUser.login, resolvedUser.role),
      refreshToken: `mock-refresh-${resolvedUser.login}`,
    }
  }

  register(payload: RegisterRequestDto): AuthResponseDto {
    const exists = this.state.users.some((item) => item.login === payload.login)
    if (!exists) {
      this.state.users.push({
        login: payload.login,
        password: payload.password,
        role: 'ROLE_USER',
      })
      this.save()
    }

    return {
      accessToken: createMockAccessToken(payload.login, 'ROLE_USER'),
      refreshToken: `mock-refresh-${payload.login}`,
    }
  }

  refresh(payload: TokenRequestDto): AuthResponseDto {
    const login = payload.token.replace(/^mock-refresh-/, '') || 'demo-admin'
    const user = this.state.users.find((item) => item.login === login) ?? this.state.users[0]

    return {
      accessToken: createMockAccessToken(user.login, user.role),
      refreshToken: `mock-refresh-${user.login}`,
    }
  }

  getOrganizations(filters?: OrganizationFilters) {
    return this.state.organizations.filter((organization) => {
      if (filters?.name) {
        const normalizedName = filters.name.toLowerCase()
        const matchesName =
          organization.name.toLowerCase().includes(normalizedName) ||
          organization.shortName.toLowerCase().includes(normalizedName)
        if (!matchesName) return false
      }

      if (filters?.isActive !== undefined && organization.isActive !== filters.isActive) {
        return false
      }

      return true
    })
  }

  createOrganization(payload: CreateOrganizationRequestDto) {
    const organization: OrganizationResponseDto = {
      id: this.nextId(this.state.organizations),
      name: payload.name,
      shortName: payload.shortName,
      description: payload.description ?? null,
      website: payload.website ?? null,
      isActive: true,
      createdAt: nowIso(),
      updatedAt: nowIso(),
    }

    this.state.organizations.unshift(organization)
    this.save()
    return organization
  }

  updateOrganization(id: number, payload: UpdateOrganizationRequestDto) {
    const organization = this.getOrganizationById(id)

    Object.assign(organization, {
      ...payload,
      updatedAt: nowIso(),
    })

    this.state.sources = this.state.sources.map((source) =>
      source.organization.id === id
        ? {
            ...source,
            organization: {
              id: organization.id,
              name: organization.name,
              shortName: organization.shortName,
            },
          }
        : source,
    )

    this.save()
    return organization
  }

  generateOrganizationInsights(
    organizationId: number,
    params?: { force?: boolean; limit?: number; from?: string; to?: string },
  ) {
    const base = this.buildInsightSummary(organizationId)

    return {
      organizationId,
      organizationName: base.organizationName,
      summary: base.summary,
      strengths: base.strengths,
      weaknesses: base.weaknesses,
      recommendations: base.recommendations,
      generatedAt: nowIso(),
      modelVersion: 'mock-aura-insights-v1',
      cached: !params?.force,
      reviewsUsed: Math.min(params?.limit ?? 50, this.getReviewsForOrganization(organizationId).length),
    } satisfies OrganizationInsightsResponseDto
  }

  getSources(filters?: SourceFilters) {
    return this.state.sources.filter((source) => {
      if (filters?.organizationId && source.organization.id !== filters.organizationId) return false
      if (filters?.name && !source.name.toLowerCase().includes(filters.name.toLowerCase())) return false
      if (filters?.type && source.type !== filters.type) return false
      if (filters?.isActive !== undefined && source.isActive !== filters.isActive) return false
      if (filters?.scheduleEnabled !== undefined && source.scheduleEnabled !== filters.scheduleEnabled) return false
      return true
    })
  }

  createSource(payload: CreateSourceRequestDto) {
    const organization = this.getOrganizationById(payload.organizationId)
    const source: SourceResponseDto = {
      id: this.nextId(this.state.sources),
      organization: {
        id: organization.id,
        name: organization.name,
        shortName: organization.shortName,
      },
      name: payload.name,
      type: payload.type,
      baseUrl: payload.baseUrl,
      collectionMode: payload.collectionMode ?? 'MANUAL',
      scheduleEnabled: payload.scheduleEnabled ?? false,
      scheduleIntervalMinutes: payload.scheduleIntervalMinutes ?? null,
      lastCollectedAt: null,
      nextCollectionAt:
        payload.scheduleEnabled && payload.scheduleIntervalMinutes
          ? plusDays(new Date(), 1)
          : null,
      description: payload.description ?? null,
      createdAt: nowIso(),
      updatedAt: nowIso(),
      isActive: true,
    }

    this.state.sources.unshift(source)
    this.save()
    return source
  }

  updateSource(id: number, payload: UpdateSourceRequestDto) {
    const source = this.getSourceById(id)
    const organization = this.getOrganizationById(payload.organizationId ?? source.organization.id)

    Object.assign(source, {
      ...payload,
      organization: {
        id: organization.id,
        name: organization.name,
        shortName: organization.shortName,
      },
      nextCollectionAt:
        payload.scheduleEnabled === false
          ? null
          : source.scheduleEnabled
            ? plusDays(new Date(), 1)
            : source.nextCollectionAt,
      updatedAt: nowIso(),
    })

    this.save()
    return source
  }

  runCollection(sourceId: number) {
    const source = this.getSourceById(sourceId)
    const timestamp = nowIso()
    const job: CollectionJobResponseDto = {
      id: this.nextId(this.state.jobs),
      sourceId,
      sourceName: source.name,
      status: 'SUCCESS',
      startedAt: timestamp,
      finishedAt: timestamp,
      collectedCount: 1,
      triggeredBy: 'demo-admin',
    }

    const review: ReviewResponseDto = {
      id: this.nextId(this.state.reviews),
      sourceId,
      sourceName: source.name,
      externalId: `collect-${Date.now()}`,
      text: `Новый отзыв из макетного сценария для источника «${source.name}».`,
      authorName: 'Новый респондент',
      rating: 4,
      publishedAt: timestamp,
      collectedAt: timestamp,
      status: 'ANALYZED',
      analysis: createAnalysis('POSITIVE', 'OTHER', ['демо', 'сбор'], 0.74),
      originalUrl: source.baseUrl,
    }

    this.state.jobs.unshift(job)
    this.state.reviews.unshift(review)
    source.lastCollectedAt = timestamp
    source.updatedAt = timestamp
    source.nextCollectionAt = source.scheduleEnabled ? plusDays(new Date(), 1) : null
    this.save()

    return job
  }

  getJobs(limit = 10) {
    return this.state.jobs.slice(0, limit)
  }

  private filteredReviews(filters: ReviewFilters) {
    return this.state.reviews.filter((review) => {
      const organizationId = this.getSourceOrganization(review.sourceId)
      if (filters.organizationId && organizationId !== filters.organizationId) return false
      if (filters.sourceId && review.sourceId !== filters.sourceId) return false
      if (filters.sentiment && review.analysis?.sentiment !== filters.sentiment) return false
      if (filters.topic && review.analysis?.topic !== filters.topic) return false
      if (filters.keyword) {
        const keyword = filters.keyword.toLowerCase()
        const haystack = [
          review.text,
          review.authorName ?? '',
          ...(review.analysis?.keywords ?? []),
        ]
          .join(' ')
          .toLowerCase()

        if (!haystack.includes(keyword)) return false
      }
      if (filters.dateFrom && review.publishedAt && review.publishedAt < filters.dateFrom) return false
      if (filters.dateTo && review.publishedAt && review.publishedAt > `${filters.dateTo}T23:59:59Z`) return false
      return true
    })
  }

  getReviews(filters: ReviewFilters) {
    const filtered = this.filteredReviews(filters)
    const [sortField = 'publishedAt', sortDirection = 'desc'] = (filters.sort ?? 'publishedAt,desc').split(',')
    const sorted = filtered.slice().sort((left, right) => {
      const leftValue = String((left as unknown as Record<string, unknown>)[sortField] ?? '')
      const rightValue = String((right as unknown as Record<string, unknown>)[sortField] ?? '')
      return sortDirection === 'asc'
        ? leftValue.localeCompare(rightValue)
        : rightValue.localeCompare(leftValue)
    })

    const page = filters.page ?? 0
    const size = filters.size ?? 10
    const start = page * size
    const content = sorted.slice(start, start + size)

    return {
      content,
      page,
      size,
      totalElements: sorted.length,
      totalPages: Math.max(1, Math.ceil(sorted.length / size)),
      last: start + size >= sorted.length,
    } satisfies PageResponseDto<ReviewListItemDto>
  }

  getPopularKeywords(params?: { organizationId?: number; limit?: number }) {
    const counts = new Map<string, number>()

    this.filteredReviews({ organizationId: params?.organizationId }).forEach((review) => {
      ;(review.analysis?.keywords ?? []).forEach((keyword) => {
        counts.set(keyword, (counts.get(keyword) ?? 0) + 1)
      })
    })

    return Array.from(counts.entries())
      .map(([keyword, count]) => ({ keyword, count }))
      .sort((left, right) => right.count - left.count)
      .slice(0, params?.limit ?? 12) satisfies KeywordStatDto[]
  }

  getReviewById(id: number) {
    const review = this.state.reviews.find((item) => item.id === id)
    if (!review) {
      throw new Error(`Unknown review: ${id}`)
    }
    return review
  }

  getReviewSummary(reviewId: number, force?: boolean) {
    const review = this.getReviewById(reviewId)

    return {
      reviewId,
      summary: `Краткий вывод: ${review.text.slice(0, 120)}${review.text.length > 120 ? '...' : ''}`,
      generatedAt: nowIso(),
      modelVersion: 'mock-aura-summary-v1',
      cached: !force,
    } satisfies ReviewSummaryResponseDto
  }

  reanalyzeReviews(params?: ReanalyzeReviewsParams) {
    const targetReviews = this.state.reviews.filter((review) => {
      const organizationId = this.getSourceOrganization(review.sourceId)
      if (params?.organizationId && organizationId !== params.organizationId) return false
      if (params?.sourceId && review.sourceId !== params.sourceId) return false
      return review.status === 'FAILED_ANALYSIS'
    })

    const limitedReviews = params?.limit ? targetReviews.slice(0, params.limit) : targetReviews
    limitedReviews.forEach((review) => {
      review.status = 'ANALYZED'
      review.analysis = createAnalysis('NEUTRAL', 'OTHER', ['повторный анализ'], 0.7)
    })

    this.save()

    return {
      requestedCount: limitedReviews.length,
      reanalyzedCount: limitedReviews.length,
      failedCount: 0,
      skippedCount: 0,
      errorMessage: null,
    } satisfies ReanalyzeReviewsResponseDto
  }

  getDashboard(filters: DashboardFilters) {
    const reviews = this.filteredReviews({
      organizationId: filters.organizationId,
      sourceId: filters.sourceId,
      sentiment: filters.sentiment,
      dateFrom: filters.from,
      dateTo: filters.to,
    })

    const sentiment = reviews.reduce(
      (acc, review) => {
        const value = review.analysis?.sentiment
        if (value === 'POSITIVE') acc.positive += 1
        if (value === 'NEUTRAL') acc.neutral += 1
        if (value === 'NEGATIVE') acc.negative += 1
        return acc
      },
      { positive: 0, neutral: 0, negative: 0 },
    )

    const topicCounts = new Map<ReviewTopic, number>()
    reviews.forEach((review) => {
      const topic = review.analysis?.topic
      if (!topic) return
      topicCounts.set(topic, (topicCounts.get(topic) ?? 0) + 1)
    })

    const topCategories = Array.from(topicCounts.entries())
      .map(([category, count]) => ({ category, count }))
      .sort((left, right) => right.count - left.count)
      .slice(0, 5) satisfies DashboardCategoryStatDto[]

    const timelineMap = new Map<string, number>()
    reviews.forEach((review) => {
      if (!review.publishedAt) return
      const month = review.publishedAt.slice(0, 7)
      timelineMap.set(month, (timelineMap.get(month) ?? 0) + 1)
    })

    const timeline = Array.from(timelineMap.entries())
      .sort(([left], [right]) => left.localeCompare(right))
      .map(([month, count]) => ({ month, count })) satisfies DashboardTimelinePointDto[]

    const sourcesCount = new Set(reviews.map((review) => review.sourceId)).size

    return {
      totalReviews: reviews.length,
      sourcesCount,
      sentiment,
      topCategories,
      timeline,
    } satisfies DashboardResponseDto
  }
}

const mockDb = new MockDatabase()

function asyncResult<T>(value: T, delay = 120) {
  return new Promise<T>((resolve) => {
    window.setTimeout(() => resolve(value), delay)
  })
}

export const mockAuthApi = {
  login: async (payload: LoginRequestDto) => asyncResult(mockDb.login(payload)),
  register: async (payload: RegisterRequestDto) => asyncResult(mockDb.register(payload)),
  refresh: async (payload: TokenRequestDto) => asyncResult(mockDb.refresh(payload)),
  logout: async () => asyncResult(undefined),
}

export const mockOrganizationsApi = {
  getOrganizations: async (filters?: OrganizationFilters) =>
    asyncResult(mockDb.getOrganizations(filters)),
  getOrganizationById: async (id: number) =>
    asyncResult(mockDb.getOrganizations().find((item) => item.id === id)!),
  generateOrganizationInsights: async (
    organizationId: number,
    params?: { force?: boolean; limit?: number; from?: string; to?: string },
  ) => asyncResult(mockDb.generateOrganizationInsights(organizationId, params), 220),
  createOrganization: async (payload: CreateOrganizationRequestDto) =>
    asyncResult(mockDb.createOrganization(payload)),
  updateOrganization: async (id: number, payload: UpdateOrganizationRequestDto) =>
    asyncResult(mockDb.updateOrganization(id, payload)),
  deleteOrganization: async () => asyncResult(undefined),
}

export const mockSourcesApi = {
  getSources: async (filters?: SourceFilters) => asyncResult(mockDb.getSources(filters)),
  getSourceById: async (id: number) => asyncResult(mockDb.getSources().find((item) => item.id === id)!),
  createSource: async (payload: CreateSourceRequestDto) => asyncResult(mockDb.createSource(payload)),
  updateSource: async (id: number, payload: UpdateSourceRequestDto) =>
    asyncResult(mockDb.updateSource(id, payload)),
  deleteSource: async () => asyncResult(undefined),
}

export const mockReviewsApi = {
  getReviews: async (filters: ReviewFilters) => asyncResult(mockDb.getReviews(filters)),
  getPopularKeywords: async (params?: { organizationId?: number; limit?: number }) =>
    asyncResult(mockDb.getPopularKeywords(params)),
  getReviewById: async (id: number) => asyncResult(mockDb.getReviewById(id)),
  getReviewSummary: async (reviewId: number, force?: boolean) =>
    asyncResult(mockDb.getReviewSummary(reviewId, force), 200),
  reanalyzeReviews: async (params?: ReanalyzeReviewsParams) =>
    asyncResult(mockDb.reanalyzeReviews(params), 240),
}

export const mockDashboardApi = {
  getDashboard: async (filters: DashboardFilters) => asyncResult(mockDb.getDashboard(filters)),
}

export const mockCollectionApi = {
  runCollection: async (sourceId: number) => asyncResult(mockDb.runCollection(sourceId), 220),
  getJobs: async (limit = 10) => asyncResult(mockDb.getJobs(limit)),
}
