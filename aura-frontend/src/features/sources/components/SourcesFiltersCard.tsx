import { DebouncedInput } from '../../../components/common/DebouncedInput'
import { Button } from '../../../components/ui/Button'
import { Card } from '../../../components/ui/Card'
import { Select } from '../../../components/ui/Select'
import { sourceTypeOptions } from '../../../lib/constants'

interface SourcesFiltersCardProps {
  organizationOptions: Array<{ value: string; label: string }>
  selectedName: string
  selectedOrganizationId: string
  selectedScheduleEnabled: string
  selectedStatus: string
  selectedType: string
  onParamChange: (key: string, value?: string) => void
  onReset: () => void
}

const statusOptions = [
  { value: 'true', label: 'Активные' },
  { value: 'false', label: 'Отключенные' },
]

const scheduleStatusOptions = [
  { value: 'true', label: 'Автосбор включён' },
  { value: 'false', label: 'Автосбор выключен' },
]

export function SourcesFiltersCard({
  organizationOptions,
  selectedName,
  selectedOrganizationId,
  selectedScheduleEnabled,
  selectedStatus,
  selectedType,
  onParamChange,
  onReset,
}: SourcesFiltersCardProps) {
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
          value={selectedOrganizationId}
          onChange={(event) => onParamChange('organizationId', event.target.value || undefined)}
          options={organizationOptions}
          placeholder="Все организации"
        />
        <DebouncedInput
          label="Название источника"
          value={selectedName}
          placeholder="Например, Tabiturient"
          onCommit={(value) => onParamChange('name', value || undefined)}
        />
        <Select
          label="Тип"
          value={selectedType}
          onChange={(event) => onParamChange('type', event.target.value || undefined)}
          options={sourceTypeOptions}
          placeholder="Все типы"
        />
        <Select
          label="Статус"
          value={selectedStatus}
          onChange={(event) => onParamChange('isActive', event.target.value || undefined)}
          options={statusOptions}
          placeholder="Все статусы"
        />
        <Select
          label="Автосбор"
          value={selectedScheduleEnabled}
          onChange={(event) => onParamChange('scheduleEnabled', event.target.value || undefined)}
          options={scheduleStatusOptions}
          placeholder="Любой"
        />
      </div>
    </Card>
  )
}
