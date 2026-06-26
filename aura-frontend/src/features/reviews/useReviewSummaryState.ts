import { useState } from 'react'
import toast from 'react-hot-toast'
import { useReviewSummaryMutation } from './queries'
import {
  buildReviewSummaryErrorMessage,
  buildReviewSummaryMeta,
} from './summary'
import type { ReviewSummaryResponseDto } from '../../types/review'

export function useReviewSummaryState(reviewId: number) {
  const {
    mutate: mutateReviewSummary,
    isPending: isReviewSummaryPending,
  } = useReviewSummaryMutation()
  const [summaryOpen, setSummaryOpen] = useState(false)
  const [summaryError, setSummaryError] = useState<string | null>(null)
  const [summaryData, setSummaryData] = useState<ReviewSummaryResponseDto | null>(null)
  const summaryMeta = summaryData ? buildReviewSummaryMeta(summaryData) : null

  const handleSummaryClick = () => {
    if (summaryData) {
      setSummaryOpen((value) => !value)
      return
    }

    setSummaryError(null)
    setSummaryOpen(true)
    mutateReviewSummary(
      { reviewId },
      {
        onSuccess: (data) => {
          setSummaryData(data)
          setSummaryOpen(true)
        },
        onError: (error) => {
          const message = buildReviewSummaryErrorMessage(error)
          setSummaryError(message)
          toast.error(message)
        },
      },
    )
  }

  const handleForceSummaryClick = () => {
    setSummaryError(null)
    setSummaryOpen(true)
    mutateReviewSummary(
      { reviewId, force: true },
      {
        onSuccess: (data) => {
          setSummaryData(data)
          setSummaryOpen(true)
        },
        onError: (error) => {
          const message = buildReviewSummaryErrorMessage(error)
          setSummaryError(message)
          toast.error(message)
        },
      },
    )
  }

  return {
    handleForceSummaryClick,
    handleSummaryClick,
    isReviewSummaryPending,
    summaryData,
    summaryError,
    summaryMeta,
    summaryOpen,
  }
}
