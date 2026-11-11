import { Navigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

/**
 * Wraps protected pages.
 *  - Not logged in            -> go to /login
 *  - requireAdmin and not admin -> go to /dashboard
 * Otherwise the page is rendered normally.
 */
export default function ProtectedRoute({ children, requireAdmin = false }) {
  const { isAuthenticated, isAdmin } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />
  }
  if (requireAdmin && !isAdmin) {
    return <Navigate to="/dashboard" replace />
  }
  return children
}
