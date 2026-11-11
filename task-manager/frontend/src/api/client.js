import axios from 'axios'

// One shared Axios instance for the whole app.
// baseURL "/api" works both in development (Vite proxies /api -> :8080) and in
// production (nginx proxies /api -> backend). So no full URL is hardcoded.
const client = axios.create({
  baseURL: '/api',
})

// REQUEST interceptor: runs before every request. We read the JWT from
// localStorage and add it as an "Authorization: Bearer ..." header.
client.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

// RESPONSE interceptor: if the server says 401 (missing/expired token),
// clear the stored login and send the user back to the login page.
client.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      localStorage.removeItem('token')
      localStorage.removeItem('user')
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    return Promise.reject(error)
  }
)

export default client
