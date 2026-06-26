import toast from 'react-hot-toast'
import { useImportReviewsMutation, useRunCollectionMutation, useUpdateSourceMutation } from './queries'
import {
  getRunCollectionErrorMessage,
  getRunCollectionToastMessage,
  getSourceMutationErrorMessage,
} from './errors'
import type { SourceResponseDto, UpdateSourceRequestDto } from '../../types/source'

export function useSourceActions({
  editingSource,
  onEditClose,
  onImportClose,
}: {
  editingSource: SourceResponseDto | null
  onEditClose: () => void
  onImportClose: () => void
}) {
  const updateSourceMutation = useUpdateSourceMutation()
  const runCollectionMutation = useRunCollectionMutation()
  const importReviewsMutation = useImportReviewsMutation()

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

  const handleImportReviews = (source: SourceResponseDto | null, file: File) => {
    if (!source) return

    importReviewsMutation.mutate(
      { sourceId: source.id, file },
      {
        onSuccess: (result) => {
          toast.success(
            `Импорт завершён: добавлено ${result.importedCount}, дубликатов ${result.duplicateCount}, некорректных строк ${result.invalidCount}`,
          )
          onImportClose()
        },
        onError: (error) => {
          toast.error(getSourceMutationErrorMessage(error, { action: 'import' }))
        },
      },
    )
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
    handleImportReviews,
    handleRunCollection,
    importReviewsMutation,
    runCollectionMutation,
    updateSourceMutation,
  }
}
