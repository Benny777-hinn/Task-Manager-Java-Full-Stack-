import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import client from '../api/client'
import Pagination from '../components/Pagination'

const PAGE_SIZE = 5

// Small helper to show a nice date/time instead of the raw ISO string.
function formatDate(value) {
  if (!value) return '-'
  return new Date(value).toLocaleString()
}

// The Dashboard: list, filter, search, paginate, complete and delete tasks.
export default function Dashboard() {
  const [tasks, setTasks] = useState([])
  const [categories, setCategories] = useState([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  const [filters, setFilters] = useState({ status: '', priority: '', categoryId: '', keyword: '' })
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalElements, setTotalElements] = useState(0)
  const [sortDir, setSortDir] = useState('asc')

  // Load the categories once, so we can show them in the filter dropdown.
  useEffect(() => {
    client.get('/categories')
      .then((res) => setCategories(res.data))
      .catch(() => setCategories([]))
  }, [])

  // Reload the task list whenever the page, sort direction or filters change.
  useEffect(() => {
    const loadTasks = async () => {
      setLoading(true)
      setError('')
      try {
        // Only send the filters that are actually set.
        const params = { page, size: PAGE_SIZE, sortDir }
        if (filters.status) params.status = filters.status
        if (filters.priority) params.priority = filters.priority
        if (filters.categoryId) params.categoryId = filters.categoryId
        if (filters.keyword) params.keyword = filters.keyword

        const { data } = await client.get('/tasks', { params })
        setTasks(data.content)        // Page object -> array of tasks
        setTotalPages(data.totalPages)
        setTotalElements(data.totalElements)
      } catch (err) {
        setError(err.response?.data?.message || 'Could not load tasks')
      } finally {
        setLoading(false)
      }
    }
    loadTasks()
  }, [page, sortDir, filters])

  // Any filter change should jump back to the first page.
  const updateFilter = (name, value) => {
    setFilters((prev) => ({ ...prev, [name]: value }))
    setPage(0)
  }

  const clearFilters = () => {
    setFilters({ status: '', priority: '', categoryId: '', keyword: '' })
    setPage(0)
  }

  const completeTask = async (id) => {
    await client.patch(`/tasks/${id}/complete`)
    // Simple way to refresh: toggle a filter-independent reload.
    setFilters((prev) => ({ ...prev }))
  }

  const deleteTask = async (id) => {
    if (!window.confirm('Delete this task?')) return
    await client.delete(`/tasks/${id}`)
    setFilters((prev) => ({ ...prev }))
  }

  return (
    <div>
      <div className="page-header">
        <h1>My Tasks</h1>
        <Link className="btn btn-primary" to="/tasks/new">+ New Task</Link>
      </div>

      {/* Filter / search bar */}
      <div className="filters">
        <input
          placeholder="Search by title..."
          value={filters.keyword}
          onChange={(e) => updateFilter('keyword', e.target.value)}
        />

        <select value={filters.status} onChange={(e) => updateFilter('status', e.target.value)}>
          <option value="">All statuses</option>
          <option value="PENDING">Pending</option>
          <option value="COMPLETED">Completed</option>
        </select>

        <select value={filters.priority} onChange={(e) => updateFilter('priority', e.target.value)}>
          <option value="">All priorities</option>
          <option value="LOW">Low</option>
          <option value="MEDIUM">Medium</option>
          <option value="HIGH">High</option>
        </select>

        <select value={filters.categoryId} onChange={(e) => updateFilter('categoryId', e.target.value)}>
          <option value="">All categories</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </select>

        <select value={sortDir} onChange={(e) => setSortDir(e.target.value)}>
          <option value="asc">Due date: earliest first</option>
          <option value="desc">Due date: latest first</option>
        </select>

        <button className="btn" onClick={clearFilters}>Clear</button>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {loading && <p className="muted">Loading...</p>}

      {!loading && tasks.length === 0 && (
        <p className="muted">No tasks found. Create your first task!</p>
      )}

      <ul className="task-list">
        {tasks.map((task) => (
          // Add extra CSS classes to colour-code priority and highlight overdue tasks.
          <li
            key={task.id}
            className={`task-item priority-${task.priority.toLowerCase()} ${task.overdue ? 'overdue' : ''} ${task.status === 'COMPLETED' ? 'completed' : ''}`}
          >
            <div className="task-main">
              <div className="task-title">
                {task.title}
                <span className="badge">{task.priority}</span>
                {task.categoryName && <span className="badge badge-cat">{task.categoryName}</span>}
                {task.overdue && <span className="badge badge-overdue">OVERDUE</span>}
                {task.status === 'COMPLETED' && <span className="badge badge-done">DONE</span>}
              </div>
              {task.description && <p className="task-desc">{task.description}</p>}
              <p className="task-meta">Due: {formatDate(task.dueDate)}</p>
            </div>

            <div className="task-actions">
              {task.status === 'PENDING' && (
                <button className="btn btn-success" onClick={() => completeTask(task.id)}>
                  Complete
                </button>
              )}
              <Link className="btn" to={`/tasks/${task.id}/edit`}>Edit</Link>
              <button className="btn btn-danger" onClick={() => deleteTask(task.id)}>Delete</button>
            </div>
          </li>
        ))}
      </ul>

      <Pagination page={page} totalPages={totalPages} onChange={setPage} />
      {totalElements > 0 && <p className="muted">{totalElements} task(s) total</p>}
    </div>
  )
}
