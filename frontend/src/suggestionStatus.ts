import type { SuggestionStatus } from './types'

export function suggestionStatusText(status: SuggestionStatus): string {
  switch (status) {
    case 'SUBMITTED':
      return 'Submitted — awaiting review'
    case 'APPROVED':
      return 'Approved — added to catalogue'
    case 'REJECTED':
      return 'Rejected'
    default:
      return status
  }
}

export function duplicateReasonText(reason: string): string {
  switch (reason) {
    case 'SAME_NAME_AND_REGION':
      return 'Same name and region'
    case 'SAME_NAME_AND_PROXIMITY':
      return 'Same name and nearby coordinates'
    case 'SIMILAR_NAME_AND_REGION':
      return 'Similar name in the same region'
    case 'PROXIMITY_AND_CONTEXT':
      return 'Nearby with matching context'
    default:
      return reason
  }
}
