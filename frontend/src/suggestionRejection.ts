/** Max length matches destination_suggestions.admin_note / event note (VARCHAR 500). */
export const REJECTION_REASON_MAX_LENGTH = 500

/** Editable default when admin rejects a likely catalogue duplicate. */
export const DEFAULT_DUPLICATE_REJECTION_REASON =
  'A destination with the same name and region already exists in our catalogue.'

export type RejectionReasonParseResult =
  | { ok: true; reason: string }
  | { ok: false; message: string }

export function parseRejectionReason(raw: string | null | undefined): RejectionReasonParseResult {
  if (raw == null) {
    return { ok: false, message: 'Rejection reason is required' }
  }
  const reason = raw.trim()
  if (!reason) {
    return { ok: false, message: 'Rejection reason is required' }
  }
  if (reason.length > REJECTION_REASON_MAX_LENGTH) {
    return {
      ok: false,
      message: `Rejection reason must be at most ${REJECTION_REASON_MAX_LENGTH} characters`,
    }
  }
  return { ok: true, reason }
}

export function defaultRejectionReason(hasPossibleDuplicate: boolean): string {
  return hasPossibleDuplicate ? DEFAULT_DUPLICATE_REJECTION_REASON : ''
}
