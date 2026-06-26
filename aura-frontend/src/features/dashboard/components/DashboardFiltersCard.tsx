import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Input } from '../../../components/ui/Input'
import { Select } from '../../../components/ui/Select'
import { sentimentOptions } from '../../../lib/constants'
import type { OrganizationResponseDto } from '../../../types/organization'
import type { SourceResponseDto } from '../../../types/source'

interface DashboardFiltersCardProps {
  dateFrom: string
  dateTo: string
  organizations: OrganizationResponseDto[]
  selectedOrganizationId?: number
  sentiment: string
  sourceId: string
  sources: SourceResponseDto[]
  onOrganizationChange: (value?: string) => void
  onReset: () => void
  onParamChange: (key: string, value?: string) => void
}

export function DashboardFiltersCard({
  dateFrom,
  dateTo,
  organizations,
  selectedOrganizationId,
  sentiment,
  sourceId,
  sources,
  onOrganizationChange,
  onReset,
  onParamChange,
}: DashboardFiltersCardProps) {
  return (
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
      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-5">
        <Select
          label="Организация"
          value={selectedOrganizationId ? String(selectedOrganizationId) : ''}
          onChange={(event) => onOrganizationChange(event.target.value || undefined)}
          options={organizations.map((organization) => ({
            value: String(organization.id),
            label: `${organization.shortName} — ${organization.name}`,
          }))}
        />
        <Input
          label="Дата от"
          type="date"
          value={dateFrom}
          onChange={(event) => onParamChange('from', event.target.value || undefined)}
        />
        <Input
          label="Дата до"
          type="date"
          value={dateTo}
          onChange={(event) => onParamChange('to', event.target.value || undefined)}
        />
        <Select
          label="Источник"
          value={sourceId}
          onChange={(event) => onParamChange('sourceId', event.target.value || undefined)}
          options={sources.map((source) => ({
            value: String(source.id),
            label: source.name,
          }))}
          placeholder="Все источники"
        />
        <Select
          label="Тональность"
          value={sentiment}
          onChange={(event) => onParamChange('sentiment', event.target.value || undefined)}
          options={sentimentOptions}
          placeholder="Все"
        />
      </div>
    </Card>
  )
}
