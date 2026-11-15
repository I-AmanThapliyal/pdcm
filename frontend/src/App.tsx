import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth'
import { Layout } from './components/Layout'
import { AdminModerationPage } from './pages/AdminModerationPage'
import { AdminSuggestionsPage } from './pages/AdminSuggestionsPage'
import { AuthPage } from './pages/AuthPage'
import { DashboardPage } from './pages/DashboardPage'
import { DestinationPage } from './pages/DestinationPage'
import { DestinationsPage } from './pages/DestinationsPage'
import { MapDiscoverPage } from './pages/MapDiscoverPage'
import { MySuggestionsPage } from './pages/MySuggestionsPage'
import { SuggestDestinationPage } from './pages/SuggestDestinationPage'

export function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<DashboardPage />} />
            <Route path="destinations" element={<DestinationsPage />} />
            <Route path="map" element={<MapDiscoverPage />} />
            <Route path="destinations/:id" element={<DestinationPage />} />
            <Route path="suggest" element={<SuggestDestinationPage />} />
            <Route path="my-suggestions" element={<MySuggestionsPage />} />
            <Route path="admin/moderation" element={<AdminModerationPage />} />
            <Route path="admin/suggestions" element={<AdminSuggestionsPage />} />
            <Route path="login" element={<AuthPage mode="login" />} />
            <Route path="register" element={<AuthPage mode="register" />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  )
}
