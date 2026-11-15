import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { pdcm, saveToken, token } from './api'
import type { UserProfile } from './types'

interface AuthState {
  user: UserProfile | null
  ready: boolean
  signIn: (accessToken: string, user: UserProfile) => void
  signOut: () => void
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserProfile | null>(null)
  const [ready, setReady] = useState(false)

  useEffect(() => {
    if (!token()) {
      setReady(true)
      return
    }
    pdcm.me()
      .then(setUser)
      .catch(() => {
        saveToken(null)
        setUser(null)
      })
      .finally(() => setReady(true))
  }, [])

  const value = useMemo<AuthState>(() => ({
    user,
    ready,
    signIn: (accessToken, nextUser) => {
      saveToken(accessToken)
      setUser(nextUser)
    },
    signOut: () => {
      saveToken(null)
      setUser(null)
    },
  }), [user, ready])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const value = useContext(AuthContext)
  if (!value) throw new Error('AuthProvider is missing')
  return value
}
