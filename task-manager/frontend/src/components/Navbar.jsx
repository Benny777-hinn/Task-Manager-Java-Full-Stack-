import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

// Top navigation bar. Links change depending on whether you are logged in
// and whether you are an ADMIN.
export default function Navbar() {
  const { user, isAuthenticated, isAdmin, logout } = useAuth()
  const navigate = useNavigate()

  const handleLogout = () => {
    logout()
    navigate('/login')
  }

  return (
    <nav className="navbar">
      <Link to="/" className="brand">Task Manager</Link>

      {isAuthenticated && (
        <div className="nav-links">
          <Link to="/dashboard">Dashboard</Link>
          <Link to="/categories">Categories</Link>
          <Link to="/tasks/new">Add Task</Link>
          {/* The Admin link is only shown to admins (role-based UI). */}
          {isAdmin && <Link to="/admin">Admin</Link>}

          <span className="nav-user">
            {user?.username} <span className="badge">{user?.role}</span>
          </span>
          <button className="btn btn-outline" onClick={handleLogout}>Logout</button>
        </div>
      )}
    </nav>
  )
}
