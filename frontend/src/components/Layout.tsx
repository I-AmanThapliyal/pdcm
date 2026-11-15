import { NavLink, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../auth'

const linkClass = ({ isActive }: { isActive: boolean }) =>
  isActive ? 'text-clay' : 'text-stone-700 hover:text-ink'

export function Layout() {
  const { user, signOut } = useAuth()
  const location = useLocation()
  const wide = location.pathname === '/map'

  return (
    <div className="min-h-screen">
      <header className="border-b border-line bg-paper/95">
        <div className={`mx-auto flex flex-wrap items-center gap-x-6 gap-y-3 px-4 py-4 ${wide ? 'max-w-6xl' : 'max-w-5xl'}`}>
          <NavLink to="/" className="display text-xl text-ink">
            Please Don&apos;t Come Here
          </NavLink>
          <nav className="flex gap-4 text-sm">
            <NavLink to="/" end className={linkClass}>Dashboard</NavLink>
            <NavLink to="/destinations" end className={linkClass}>Destinations</NavLink>
            <NavLink to="/map" className={linkClass}>Map</NavLink>
            {user && (
              <>
                <NavLink to="/suggest" className={linkClass}>Suggest</NavLink>
                <NavLink to="/my-suggestions" className={linkClass}>My suggestions</NavLink>
              </>
            )}
            {user?.role === 'ADMIN' && (
              <>
                <NavLink to="/admin/moderation" className={linkClass}>Moderation</NavLink>
                <NavLink to="/admin/suggestions" className={linkClass}>Suggestions</NavLink>
              </>
            )}
          </nav>
          <div className="ml-auto flex items-center gap-3 text-sm">
            {user ? (
              <>
                <span>{user.displayName}</span>
                <button type="button" className="underline" onClick={signOut}>Sign out</button>
              </>
            ) : (
              <>
                <NavLink to="/login" className={linkClass}>Sign in</NavLink>
                <NavLink to="/register" className="rounded-full bg-ink px-3 py-1.5 text-paper">Create account</NavLink>
              </>
            )}
          </div>
        </div>
      </header>
      <main className={`mx-auto px-4 py-8 ${wide ? 'max-w-6xl' : 'max-w-5xl'}`}>
        <Outlet />
      </main>
    </div>
  )
}
