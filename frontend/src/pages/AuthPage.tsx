import { useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { ApiFailure, pdcm } from '../api'
import { useAuth } from '../auth'

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const { signIn } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError(null)
    try {
      const result = mode === 'register'
        ? await pdcm.register({ email, displayName, password })
        : await pdcm.login({ email, password })
      signIn(result.accessToken, result.user)
      const from = (location.state as { from?: string } | null)?.from ?? '/'
      navigate(from)
    } catch (reason) {
      const details = reason instanceof ApiFailure ? reason.details.filter(Boolean).join(' ') : ''
      setError(reason instanceof ApiFailure ? `${reason.message}${details ? ` ${details}` : ''}` : 'Sign-in failed.')
    }
  }

  return (
    <form className="mx-auto max-w-md space-y-4 rounded-3xl border border-line bg-white p-6" onSubmit={submit}>
      <h1 className="text-3xl">{mode === 'register' ? 'Create account' : 'Sign in'}</h1>
      {mode === 'register' && (
        <label className="block text-sm">Display name
          <input className="mt-1 w-full rounded-lg border border-line px-3 py-2" value={displayName} onChange={(event) => setDisplayName(event.target.value)} required minLength={2} />
        </label>
      )}
      <label className="block text-sm">Email
        <input className="mt-1 w-full rounded-lg border border-line px-3 py-2" type="email" value={email} onChange={(event) => setEmail(event.target.value)} required />
      </label>
      <label className="block text-sm">Password
        <input className="mt-1 w-full rounded-lg border border-line px-3 py-2" type="password" value={password} onChange={(event) => setPassword(event.target.value)} required minLength={mode === 'register' ? 8 : 1} />
      </label>
      {error && <p className="text-sm text-clay">{error}</p>}
      <button type="submit" className="rounded-full bg-ink px-4 py-2 text-paper">{mode === 'register' ? 'Create account' : 'Sign in'}</button>
      <p className="text-sm">
        {mode === 'register'
          ? <Link to="/login" className="underline">Already registered? Sign in</Link>
          : <Link to="/register" className="underline">Need an account? Register</Link>}
      </p>
    </form>
  )
}
