import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { PageHeader } from '../components/common/PageHeader'
import { Button } from '../components/ui/Button'
import { Card } from '../components/ui/Card'
import { PlatformForm } from '../features/customPlatforms/PlatformForm'
import {
  createDefaultPlatformFormState,
  createPlatform,
  type PlatformFormState,
} from '../features/customPlatforms/model'

export function NewCustomPlatformPage() {
  const navigate = useNavigate()
  const [value, setValue] = useState<PlatformFormState>(() =>
    createDefaultPlatformFormState(),
  )

  const handleSubmit = () => {
    createPlatform(value)
    navigate('/custom-sources')
  }

  return (
    <div className="space-y-6">
      <PageHeader
        title="Новая платформа"
        actions={
          <Link to="/custom-sources">
            <Button variant="ghost">Назад к списку</Button>
          </Link>
        }
      />

      <Card className="space-y-6">
        <PlatformForm
          submitLabel="Сохранить платформу"
          value={value}
          onChange={setValue}
          onSubmit={handleSubmit}
        />
      </Card>
    </div>
  )
}
