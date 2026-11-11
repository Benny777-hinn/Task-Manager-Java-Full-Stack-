import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import client from '../api/client'

// Convert an ISO date string into the format <input type="datetime-local"> wants
// ("YYYY-MM-DDTHH:mm").
function toLocalInput(value) {
  if (!value) return ''
  const d = new Date(value)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// One form used for BOTH creating and editing.
// If the URL has an :id param, we are editing; otherwise we are creating.
export default function TaskForm() {
  const { id } = useParams()
  const navigate = useNavigate()
  const isEdit = Boolean(id)

  const [form, setForm] = useState({
    title: '',
    description: '',
    dueDate: '',
    priority: 'MEDIUM',
    status: 'PENDING',
    categoryId: '',
  })
  const [categories, setCategories] = useState([])
  const [errors, setErrors] = useState({})
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  // Load categories for the dropdown, and (when editing) the task itself.
  useEffect(() => {
    client.get('/categories').then((res) => setCategories(res.data)).catch(() => {})

    if (isEdit) {
      client.get(`/tasks/${id}`)
        .then((res) => {
          const t = res.data
          setForm({
            title: t.title,
            description: t.description || '',
            dueDate: toLocalInput(t.dueDate),
            priority: t.priority,
            status: t.status,
            categoryId: t.categoryId ? String(t.categoryId) : '',
          })
        })
        .catch(() => setError('Could not load this task'))
    }
  }, [id, isEdit])

  const handleChange = (e) => setForm({ ...form, [e.target.name]: e.target.value })

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError('')
    setErrors({})
    setLoading(true)

    // Build the request body. categoryId is sent as a number (or null).
    const payload = {
      title: form.title,
      description: form.description,
      dueDate: form.dueDate,
      priority: form.priority,
      status: form.status,
      categoryId: form.categoryId ? Number(form.categoryId) : null,
    }

    try {
      if (isEdit) {
        await client.put(`/tasks/${id}`, payload)
      } else {
        await client.post('/tasks', payload)
      }
      navigate('/dashboard')
    } catch (err) {
      const data = err.response?.data
      if (data?.fieldErrors) {
        setErrors(data.fieldErrors)
      } else {
        setError(data?.message || 'Could not save the task')
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="card">
      <h1>{isEdit ? 'Edit Task' : 'New Task'}</h1>
      {error && <div className="alert alert-error">{error}</div>}

      <form onSubmit={handleSubmit}>
        <label>Title</label>
        <input name="title" value={form.title} onChange={handleChange} required />
        {errors.title && <span className="field-error">{errors.title}</span>}

        <label>Description</label>
        <textarea name="description" rows="3" value={form.description} onChange={handleChange} />

        <label>Due date &amp; time</label>
        <input
          name="dueDate"
          type="datetime-local"
          value={form.dueDate}
          onChange={handleChange}
          required
        />
        {errors.dueDate && <span className="field-error">{errors.dueDate}</span>}

        <label>Priority</label>
        <select name="priority" value={form.priority} onChange={handleChange}>
          <option value="LOW">Low</option>
          <option value="MEDIUM">Medium</option>
          <option value="HIGH">High</option>
        </select>

        <label>Category</label>
        <select name="categoryId" value={form.categoryId} onChange={handleChange}>
          <option value="">Uncategorized</option>
          {categories.map((c) => (
            <option key={c.id} value={c.id}>{c.name}</option>
          ))}
        </select>

        {/* Status is only editable when editing an existing task. */}
        {isEdit && (
          <>
            <label>Status</label>
            <select name="status" value={form.status} onChange={handleChange}>
              <option value="PENDING">Pending</option>
              <option value="COMPLETED">Completed</option>
            </select>
          </>
        )}

        <div className="form-actions">
          <button className="btn btn-primary" type="submit" disabled={loading}>
            {loading ? 'Saving...' : 'Save'}
          </button>
          <button type="button" className="btn" onClick={() => navigate('/dashboard')}>
            Cancel
          </button>
        </div>
      </form>
    </div>
  )
}
