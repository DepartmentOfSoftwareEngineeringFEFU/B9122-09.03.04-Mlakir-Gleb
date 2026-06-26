import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { OrganizationEditDialog } from './OrganizationEditDialog'

const firstOrganization = {
  id: 1,
  name: 'Дальневосточный федеральный университет',
  shortName: 'ДВФУ',
  description: 'Первое описание',
  website: 'https://dvfu.ru',
  isActive: true,
  createdAt: '2026-01-01T00:00:00Z',
  updatedAt: '2026-01-02T00:00:00Z',
}

const secondOrganization = {
  id: 2,
  name: 'Тихоокеанский государственный университет',
  shortName: 'ТОГУ',
  description: 'Второе описание',
  website: 'https://pnu.edu.ru',
  isActive: false,
  createdAt: '2026-02-01T00:00:00Z',
  updatedAt: '2026-02-02T00:00:00Z',
}

describe('OrganizationEditDialog', () => {
  it('resets form values when the edited organization changes', async () => {
    const user = userEvent.setup()
    const onSubmit = vi.fn()

    const { rerender } = render(
      <OrganizationEditDialog
        isLoading={false}
        onClose={vi.fn()}
        onSubmit={onSubmit}
        organization={firstOrganization}
      />,
    )

    const nameInput = screen.getByPlaceholderText(
      'Например, Дальневосточный федеральный университет',
    )
    await user.clear(nameInput)
    await user.type(nameInput, 'Временное название')
    expect(nameInput).toHaveValue('Временное название')

    rerender(
      <OrganizationEditDialog
        isLoading={false}
        onClose={vi.fn()}
        onSubmit={onSubmit}
        organization={secondOrganization}
      />,
    )

    expect(
      screen.getByPlaceholderText('Например, Дальневосточный федеральный университет'),
    ).toHaveValue(secondOrganization.name)
    expect(screen.getByPlaceholderText('Например, ДВФУ')).toHaveValue(
      secondOrganization.shortName,
    )
    expect(screen.getByPlaceholderText('https://example.org')).toHaveValue(
      secondOrganization.website,
    )
    expect(screen.getByPlaceholderText('Краткое описание организации')).toHaveValue(
      secondOrganization.description,
    )
    expect(screen.getByRole('combobox')).toHaveValue('false')
  })
})
