import { useMemo, useState } from 'react'
import toast from 'react-hot-toast'
import {
  buildOrganizationInsightsErrorMessage,
  buildOrganizationInsightsMeta,
} from '../organizations/insights'
import { useOrganizationInsightsMutation } from '../organizations/queries'
import type { OrganizationInsightsResponseDto } from '../../types/organization'

interface UseDashboardInsightsParams {
  organizationId: number
  dateFrom: string
  dateTo: string
}

export function useDashboardInsights({
  organizationId,
  dateFrom,
  dateTo,
}: UseDashboardInsightsParams) {
  const [insightsLimit, setInsightsLimit] = useState('50')
  const [insightsState, setInsightsState] = useState<{
    data: OrganizationInsightsResponseDto | null
    error: string | null
    key: string
    open: boolean
  }>({
    data: null,
    error: null,
    key: '',
    open: false,
  })
  const organizationInsightsMutation = useOrganizationInsightsMutation()

  const currentInsightsKey = [organizationId, dateFrom, dateTo, insightsLimit].join(':')
  const insightsData =
    insightsState.key === currentInsightsKey ? insightsState.data : null
  const insightsError =
    insightsState.key === currentInsightsKey ? insightsState.error : null
  const insightsOpen =
    insightsState.key === currentInsightsKey ? insightsState.open : false
  const insightsMeta = useMemo(
    () => (insightsData ? buildOrganizationInsightsMeta(insightsData) : null),
    [insightsData],
  )

  const handleGenerateInsights = () => {
    if (insightsData) {
      setInsightsState((state) => ({
        ...state,
        key: currentInsightsKey,
        open: !insightsOpen,
      }))
      return
    }

    setInsightsState({
      data: null,
      error: null,
      key: currentInsightsKey,
      open: true,
    })
    organizationInsightsMutation.mutate(
      {
        organizationId,
        params: {
          from: dateFrom || undefined,
          to: dateTo || undefined,
          limit: Number(insightsLimit),
        },
      },
      {
        onSuccess: (data) => {
          setInsightsState({
            data,
            error: null,
            key: currentInsightsKey,
            open: true,
          })
        },
        onError: (error) => {
          const message = buildOrganizationInsightsErrorMessage(error)
          setInsightsState({
            data: null,
            error: message,
            key: currentInsightsKey,
            open: true,
          })
          toast.error(message)
        },
      },
    )
  }

  const handleForceInsights = () => {
    setInsightsState((state) => ({
      data: state.key === currentInsightsKey ? state.data : null,
      error: null,
      key: currentInsightsKey,
      open: true,
    }))
    organizationInsightsMutation.mutate(
      {
        organizationId,
        params: {
          force: true,
          from: dateFrom || undefined,
          to: dateTo || undefined,
          limit: Number(insightsLimit),
        },
      },
      {
        onSuccess: (data) => {
          setInsightsState({
            data,
            error: null,
            key: currentInsightsKey,
            open: true,
          })
        },
        onError: (error) => {
          const message = buildOrganizationInsightsErrorMessage(error)
          setInsightsState((state) => ({
            data: state.key === currentInsightsKey ? state.data : null,
            error: message,
            key: currentInsightsKey,
            open: true,
          }))
          toast.error(message)
        },
      },
    )
  }

  return {
    insightsData,
    insightsError,
    insightsLimit,
    insightsMeta,
    insightsOpen,
    isPending: organizationInsightsMutation.isPending,
    setInsightsLimit,
    handleForceInsights,
    handleGenerateInsights,
  }
}
