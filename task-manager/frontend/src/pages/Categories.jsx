import { useEffect, useState } from 'react'
import client from '../api/client'

// Page to manage the logged-in user's categories (Work, Study, Personal, ...).
export default function Categories() {
  const [categories, setCategories] = useState([])
  const [name, setName] = useState('')
  const [error, setError] = useState('')

  const loadCategories = async () => {
    try {
      const { data } = await client.get('/categories')
      setCategories(data)
    } catch (err) {
      setError(err.response?.data?.message || 'Could not load categories')
    }
  }

  useEffect(() => { loadCategories() }, [])

  const handleAdd = async (e) => {
    e.preventDefault()
    setError('')
    try {
      await client.post('/categories', { name })
      setName('')
      loadCategories()
    } catch (err) {
      setError(err.response?.data?.message || 'Could not create the category')
    }
  }

  const handleDelete = async (id) => {
    if (!window.confirm('Delete this category? Tasks in it become Uncategorized.')) return
    try {
      await client.delete(`/categories/${id}`)
      loadCategories()
    } catch (err) {
      setError(err.response?.data?.message || 'Could not delete the category')
    }
  }

  return (
    <div>
      <h1>Categories</h1>
      {error && <div className="alert alert-error">{error}</div>}

      <form className="inline-form" onSubmit={handleAdd}>
        <input
          placeholder="New category name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          required
        />
        <button className="btn btn-primary" type="submit">Add</button>
      </form>

      {categories.length === 0 ? (
        <p className="muted">No categories yet.</p>
      ) : (
        <ul className="simple-list">
          {categories.map((c) => (
            <li key={c.id}>
              <span>{c.name}</span>
              <button className="btn btn-danger" onClick={() => handleDelete(c.id)}>Delete</button>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
