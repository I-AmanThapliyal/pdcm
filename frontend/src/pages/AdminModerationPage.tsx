import { useEffect, useState } from 'react'
import { Link, Navigate } from 'react-router-dom'
import { ApiFailure, pdcm } from '../api'
import { useAuth } from '../auth'
import { formatWhen } from '../format'
import { trustLabelText } from '../trustLabel'
import type { AdminReportSummary } from '../types'

export function AdminModerationPage() {
  const { user, ready } = useAuth()
  const [reports, setReports] = useState<AdminReportSummary[]>([])
  const [queue, setQueue] = useState<'FLAGGED' | 'PENDING'>('FLAGGED')
  const [error, setError] = useState<string | null>(null)
  const [message, setMessage] = useState<string | null>(null)
  const [busyId, setBusyId] = useState<number | null>(null)

  useEffect(() => {
    if (!user || user.role !== 'ADMIN') return
    setError(null)
    pdcm.adminReports(queue)
      .then(setReports)
      .catch((reason: unknown) => {
        setError(reason instanceof ApiFailure ? reason.message : 'Could not load moderation queue.')
      })
  }, [user, queue, message])

  if (!ready) return <p>Loading…</p>
  if (!user) return <Navigate to="/login" replace />
  if (user.role !== 'ADMIN') return <Navigate to="/" replace />

  async function act(id: number, action: 'verify' | 'reject') {
    setBusyId(id)
    setMessage(null)
    setError(null)
    try {
      if (action === 'verify') {
        await pdcm.adminVerifyReport(id, 'Admin confirmed location eligibility')
        setMessage(`Report ${id} verified.`)
      } else {
        await pdcm.adminRejectReport(id, 'Admin rejected after review')
        setMessage(`Report ${id} rejected.`)
      }
    } catch (reason) {
      setError(reason instanceof ApiFailure ? reason.message : 'Moderation action failed.')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-3xl">Report moderation</h1>
        <p className="mt-2 max-w-2xl text-sm text-stone-600">
          ADMIN only. Verify promotes to location-verified only when distance (or retained GPS) is within the geofence.
          Raw coordinates are never shown here. Unverified reports never feed live crowd by accident.
        </p>
      </header>

      <div className="flex gap-3 text-sm">
        <button
          type="button"
          className={queue === 'FLAGGED' ? 'underline font-medium' : 'text-stone-600'}
          onClick={() => setQueue('FLAGGED')}
        >
          Flagged
        </button>
        <button
          type="button"
          className={queue === 'PENDING' ? 'underline font-medium' : 'text-stone-600'}
          onClick={() => setQueue('PENDING')}
        >
          Pending
        </button>
      </div>

      {error && <p className="text-clay">{error}</p>}
      {message && <p className="text-stone-700">{message}</p>}

      {reports.length === 0 ? (
        <p className="text-stone-700">No reports in this queue.</p>
      ) : (
        <ul className="divide-y divide-line rounded-2xl border border-line bg-white">
          {reports.map((report) => (
            <li key={report.id} className="space-y-2 px-4 py-4 text-sm">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div>
                  <Link to={`/destinations/${report.destinationId}`} className="font-medium">
                    {report.destinationName}
                  </Link>
                  <p className="text-stone-600">
                    Report #{report.id} · contributor #{report.contributorUserId} ·{' '}
                    {trustLabelText(report.trustLabel, report.verificationStatus)}
                  </p>
                  <p className="text-stone-600">
                    Crowd {report.crowdIntensity}/5 · {report.trafficCondition.toLowerCase()} · observed{' '}
                    {formatWhen(report.observedAt)}
                  </p>
                  <p className="text-stone-600">
                    Distance{' '}
                    {report.distanceToDestinationMeters == null
                      ? 'unknown'
                      : `${Math.round(report.distanceToDestinationMeters)} m`}
                    {' · '}
                    coords {report.coordinatesRetained ? 'retained (not displayed)' : 'purged or absent'}
                  </p>
                </div>
                <div className="flex gap-2">
                  <button
                    type="button"
                    className="rounded-lg border border-line px-3 py-1.5"
                    disabled={busyId === report.id}
                    onClick={() => act(report.id, 'verify')}
                  >
                    Verify
                  </button>
                  {queue === 'FLAGGED' && (
                    <button
                      type="button"
                      className="rounded-lg border border-line px-3 py-1.5"
                      disabled={busyId === report.id}
                      onClick={() => act(report.id, 'reject')}
                    >
                      Reject
                    </button>
                  )}
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
