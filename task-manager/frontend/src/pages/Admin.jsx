import { useEffect, useState } from 'react'
import client from '../api/client'
import Pagination from '../components/Pagination'

// Admin page: only reachable by users with the ADMIN role.
// Two tabs: all users (with delete) and all tasks.
export default function Admin() {
  const [tab, setTab] = useState('users')

  const [users, setUsers] = useState([])
  const [usersPage, setUsersPage] = useState(0)
  const [usersTotalPages, setUsersTotalPages] = useState(0)

  const [tasks, setTasks] = useState([])
  const [tasksPage, setTasksPage] = useState(0)
  const [tasksTotalPages, setTasksTotalPages] = useState(0)

  const [error, setError] = useState('')

  const loadUsers = async () => {
    try {
      const { data } = await client.get('/admin/users', { params: { page: usersPage, size: 5 } })
      setUsers(data.content)
      setUsersTotalPages(data.totalPages)
    } catch (err) {
      setError(err.response?.data?.message || 'Could not load users')
    }
  }

  const loadTasks = async () => {
    try {
      const { data } = await client.get('/admin/tasks', { params: { page: tasksPage, size: 5 } })
      setTasks(data.content)
      setTasksTotalPages(data.totalPages)
    } catch (err) {
      setError(err.response?.data?.message || 'Could not load tasks')
    }
  }

  useEffect(() => { loadUsers() }, [usersPage])
  useEffect(() => { loadTasks() }, [tasksPage])

  const deleteUser = async (id) => {
    if (!window.confirm('Delete this user and all of their tasks?')) return
    try {
      await client.delete(`/admin/users/${id}`)
      loadUsers()
    } catch (err) {
      setError(err.response?.data?.message || 'Could not delete the user')
    }
  }

  return (
    <div>
      <h1>Admin</h1>
      {error && <div className="alert alert-error">{error}</div>}

      <div className="tabs">
        <button
          className={`btn ${tab === 'users' ? 'btn-primary' : ''}`}
          onClick={() => setTab('users')}
        >
          Users
        </button>
        <button
          className={`btn ${tab === 'tasks' ? 'btn-primary' : ''}`}
          onClick={() => setTab('tasks')}
        >
          All Tasks
        </button>
      </div>

      {tab === 'users' && (
        <>
          <table className="table">
            <thead>
              <tr>
                <th>ID</th><th>Username</th><th>Email</th><th>Role</th><th></th>
              </tr>
            </thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>{u.id}</td>
                  <td>{u.username}</td>
                  <td>{u.email}</td>
                  <td><span className="badge">{u.role}</span></td>
                  <td>
                    <button className="btn btn-danger" onClick={() => deleteUser(u.id)}>
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <Pagination page={usersPage} totalPages={usersTotalPages} onChange={setUsersPage} />
        </>
      )}

      {tab === 'tasks' && (
        <>
          <table className="table">
            <thead>
              <tr>
                <th>ID</th><th>Title</th><th>Owner</th><th>Priority</th><th>Status</th><th>Due</th>
              </tr>
            </thead>
            <tbody>
              {tasks.map((t) => (
                <tr key={t.id} className={t.overdue ? 'row-overdue' : ''}>
                  <td>{t.id}</td>
                  <td>{t.title}</td>
                  <td>{t.ownerUsername}</td>
                  <td>{t.priority}</td>
                  <td>{t.status}</td>
                  <td>{t.dueDate ? new Date(t.dueDate).toLocaleString() : '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <Pagination page={tasksPage} totalPages={tasksTotalPages} onChange={setTasksPage} />
        </>
      )}
    </div>
  )
}
