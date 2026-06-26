import { DebouncedInput } from '../../../components/common/DebouncedInput'
import { Badge } from '../../../components/ui/Badge'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { Select } from '../../../components/ui/Select'
import { sentimentOptions, topicOptions } from '../../../lib/constants'
import type { KeywordStatDto } from '../../../types/review'
import type { OrganizationResponseDto } from '../../../types/organization'
import type { SourceResponseDto } from '../../../types/source'

const sortOptions = [
  { value: 'publishedAt,desc', label: 'Сначала новые' },
  { value: 'publishedAt,asc', label: 'Сначала старые' },
  { value: 'rating,desc', label: 'По рейтингу' },
]

interface ReviewsFiltersCardProps {
  dateFrom: string
  dateTo: string
  keyword: string
  organizations: OrganizationResponseDto[]
  popularKeywords: KeywordStatDto[]
  selectedOrganizationId: string
  selectedSourceId: string
  selectedSentiment: string
  selectedSort: string
  selectedTopic: string
  sources: SourceResponseDto[]
  onParamChange: (key: string, value?: string) => void
  onReset: () => void
}

export function ReviewsFiltersCard({
  dateFrom,
  dateTo,
  keyword,
  organizations,
  popularKeywords,
  selectedOrganizationId,
  selectedSentiment,
  selectedSort,
  selectedSourceId,
  selectedTopic,
  sources,
  onParamChange,
  onReset,
}: ReviewsFiltersCardProps) {
  return (
    <>
      <Card className="p-5">
        <div className="mb-4">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h2 className="text-lg font-semibold text-slate-950">Фильтры</h2>
            </div>
            <Button size="sm" type="button" variant="ghost" onClick={onReset}>
              Сбросить
            </Button>
          </div>
        </div>
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-7">
          <Select
            label="Организация"
            value={selectedOrganizationId}
            onChange={(event) => onParamChange('organizationId', event.target.value || undefined)}
            options={organizations.map((organization) => ({
              value: String(organization.id),
              label: organization.shortName,
            }))}
            placeholder="Все организации"
          />
          <Select
            label="Источник"
            value={selectedSourceId}
            onChange={(event) => onParamChange('sourceId', event.target.value || undefined)}
            options={sources.map((source) => ({
              value: String(source.id),
              label: source.name,
            }))}
            placeholder="Все источники"
          />
          <Select
            label="Тональность"
            value={selectedSentiment}
            onChange={(event) => onParamChange('sentiment', event.target.value || undefined)}
            options={sentimentOptions}
            placeholder="Все"
          />
          <Select
            label="Тема"
            value={selectedTopic}
            onChange={(event) => onParamChange('topic', event.target.value || undefined)}
            options={topicOptions}
            placeholder="Все"
          />
          <Input
            label="Дата от"
            type="date"
            value={dateFrom}
            onChange={(event) => onParamChange('dateFrom', event.target.value || undefined)}
          />
          <Input
            label="Дата до"
            type="date"
            value={dateTo}
            onChange={(event) => onParamChange('dateTo', event.target.value || undefined)}
          />
          <Select
            label="Сортировка"
            value={selectedSort}
            onChange={(event) => onParamChange('sort', event.target.value)}
            options={sortOptions}
          />
        </div>
      </Card>

      <Card className="p-5">
        <div className="mb-4">
          <h2 className="text-lg font-semibold text-slate-950">Поиск по ключевым словам</h2>
        </div>
        <DebouncedInput
          value={keyword}
          onCommit={(value) => onParamChange('keyword', value || undefined)}
          placeholder="Например: общежитие, преподаватели, расписание"
          className="xl:col-span-2"
        />
        {popularKeywords.length ? (
          <div className="mt-4">
            <div className="flex flex-wrap gap-2">
              {popularKeywords.map((item) => {
                const isActive = item.keyword === keyword

                return (
                  <button
                    key={item.keyword}
                    type="button"
                    onClick={() => onParamChange('keyword', item.keyword)}
                    className="rounded-full focus:outline-none focus-visible:ring-2 focus-visible:ring-blue-200"
                  >
                    <Badge
                      variant={isActive ? 'active' : 'default'}
                      className="cursor-pointer transition hover:bg-blue-50 hover:text-blue-700"
                    >
                      {item.keyword} ({item.count})
                    </Badge>
                  </button>
                )
              })}
            </div>
          </div>
        ) : null}
      </Card>
    </>
  )
}
