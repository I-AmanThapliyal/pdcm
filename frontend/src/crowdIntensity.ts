/** Crowd intensity is a whole number from 1 through 5. Zero and decimals are rejected. */
export function parseCrowdIntensity(raw: string): number | null {
	const trimmed = raw.trim()
	if (trimmed === '') return null
	if (!/^[1-5]$/.test(trimmed)) return null
	return Number(trimmed)
}

export const CROWD_INTENSITY_ERROR =
	'Crowd intensity must be a whole number from 1 to 5.'
