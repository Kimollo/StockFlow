import { useState } from 'react'
import { api } from '../api.js'
import { Banner } from '../ui.jsx'

export default function Login({ onSuccess }) {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)

  async function submit(event) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const body = await api('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ username: username.trim(), password }),
      })
      onSuccess(body)
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  return (
    <div className="login-screen">
      <form className="panel login-card" onSubmit={submit}>
        <div className="brand">
          <span className="mark">SF</span>
          <div>
            <p className="eyebrow">stokivo</p>
            <h2>StockFlow</h2>
          </div>
        </div>
        <p className="muted">Sign in to record sales and look after the shop.</p>
        <Banner error={error} />
        <label>
          Username
          <input value={username} onChange={(event) => setUsername(event.target.value)} autoComplete="username" required />
        </label>
        <label>
          Password
          <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} autoComplete="current-password" required />
        </label>
        <button className="primary" type="submit" disabled={saving}>{saving ? 'Signing in…' : 'Sign in'}</button>
      </form>
    </div>
  )
}
