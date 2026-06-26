import { fireEvent, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it, vi } from 'vitest'
import { ReviewsFiltersCard } from './ReviewsFiltersCard'

describe('ReviewsFiltersCard', () => {
  it('propagates filter changes, debounced keyword search and reset action', async () => {
    const user = userEvent.setup()
    const onParamChange = vi.fn()
    const onReset = vi.fn()

    render(
      <ReviewsFiltersCard
        dateFrom=""
        dateTo=""
        keyword=""
        organizations={[
          {
            id: 2,
            name: 'Дальневосточный федеральный университет',
            shortName: 'ДВФУ',
            description: null,
            website: null,
            isActive: true,
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-02T00:00:00Z',
          },
        ]}
        popularKeywords={[{ keyword: 'общежитие', count: 12 }]}
        selectedOrganizationId=""
        selectedSentiment=""
        selectedSort="publishedAt,desc"
        selectedSourceId=""
        selectedTopic=""
        sources={[
          {
            id: 7,
            organization: {
              id: 2,
              name: 'Дальневосточный федеральный университет',
              shortName: 'ДВФУ',
            },
            name: 'Tabiturient ДВФУ',
            type: 'TABITURIENT',
            baseUrl: 'https://tabiturient.ru/vuzu/dvfu/',
            collectionMode: 'MANUAL',
            scheduleEnabled: false,
            scheduleIntervalMinutes: null,
            lastCollectedAt: null,
            nextCollectionAt: null,
            description: null,
            createdAt: '2026-01-01T00:00:00Z',
            updatedAt: '2026-01-02T00:00:00Z',
            isActive: true,
          },
        ]}
        onParamChange={onParamChange}
        onReset={onReset}
      />,
    )

    await user.selectOptions(screen.getByLabelText('Организация'), '2')
    expect(onParamChange).toHaveBeenCalledWith('organizationId', '2')

    fireEvent.change(
      screen.getByPlaceholderText('Например: общежитие, преподаватели, расписание'),
      { target: { value: '  кампус  ' } },
    )
    await new Promise((resolve) => window.setTimeout(resolve, 450))
    expect(onParamChange).toHaveBeenCalledWith('keyword', 'кампус')

    await user.click(screen.getByRole('button', { name: 'общежитие (12)' }))
    expect(onParamChange).toHaveBeenCalledWith('keyword', 'общежитие')

    await user.click(screen.getByRole('button', { name: 'Сбросить' }))
    expect(onReset).toHaveBeenCalledTimes(1)
  })
})
