import toast from 'react-hot-toast'
import { useRunCollectionMutation, useUpdateSourceMutation } from './queries'
import {
  getRunCollectionErrorMessage,
  getRunCollectionToastMessage,
  getSourceMutationErrorMessage,
} from './errors'
import type { SourceResponseDto, UpdateSourceRequestDto } from '../../types/source'

export function useSourceActions({
  editingSource,
  onEditClose,
}: {
  editingSource: SourceResponseDto | null
  onEditClose: () => void
}) {
  const updateSourceMutation = useUpdateSourceMutation()
  const runCollectionMutation = useRunCollectionMutation()

  const handleRunCollection = (source: SourceResponseDto) => {
    runCollectionMutation.mutate(source.id, {
      onSuccess: (result) => {
        const feedback = getRunCollectionToastMessage(result, source.name)
        if (feedback.tone === 'error') {
          toast.error(feedback.message, { duration: 6000 })
          return
        }
        toast.success(feedback.message)
      },
      onError: (error) => {
        toast.error(getRunCollectionErrorMessage(error), { duration: 6000 })
      },
    })
  }

  const handleEditSubmit = (payload: { id: number; data: UpdateSourceRequestDto }) => {
    updateSourceMutation.mutate(payload, {
      onSuccess: () => {
        toast.success('Источник обновлён')
        onEditClose()
      },
      onError: (error) => {
        toast.error(
          getSourceMutationErrorMessage(error, {
            action: 'update',
            sourceType: editingSource?.type,
          }),
        )
      },
    })
  }

  return {
    handleEditSubmit,
    handleRunCollection,
    runCollectionMutation,
    updateSourceMutation,
  }
}
