import { createContext, useContext, useState } from 'react'
import client from '../api/client'

// AuthContext lets any component know who is logged in, without passing props
// through every level. useAuth() is the hook components use to read it.
const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  // Load the user from localStorage so a page refresh keeps you logged in.
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('user')
    return stored ? JSON.parse(stored) : null
  })

  // Shared helper to store the token + user after login/register.
  const storeSession = (data) => {
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(data))
    setUser(data)
  }

  const login = async (username, password) => {
    const { data } = await client.post('/auth/login', { username, password })
    storeSession(data)
    return data
  }

  const register = async (username, email, password) => {
    const { data } = await client.post('/auth/register', { username, email, password })
    storeSession(data)
    return data
  }

  const logout = () => {
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    setUser(null)
  }

  const isAuthenticated = !!localStorage.getItem('token')
  const isAdmin = user?.role === 'ADMIN'

  return (
    <AuthContext.Provider value={{ user, login, register, logout, isAuthenticated, isAdmin }}>
      {children}
    </AuthContext.Provider>
  )
}

// Small custom hook so components can write: const { user } = useAuth()
export function useAuth() {
  return useContext(AuthContext)
}
