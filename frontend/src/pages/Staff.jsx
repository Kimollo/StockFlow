import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { Banner, PageHeader } from '../ui.jsx'

const emptyForm = { name: '', username: '', password: '', role: 'STAFF' }

export default function Staff({ currentUserId }) {
  const [users, setUsers] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [passwords, setPasswords] = useState({})
  const [notice, setNotice] = useState('')
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    api('/api/users').then(setUsers).catch(setError)
  }, [])

  function update(field, value) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  async function create(event) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      await api('/api/users', {
        method: 'POST',
        body: JSON.stringify({
          name: form.name.trim(),
          username: form.username.trim(),
          password: form.password,
          role: form.role,
        }),
      })
      setForm(emptyForm)
      setUsers(await api('/api/users'))
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  async function resetPassword(event, user) {
    event.preventDefault()
    setError(null)
    setNotice('')
    try {
      await api(`/api/users/${user.id}/password`, {
        method: 'PATCH',
        body: JSON.stringify({ password: passwords[user.id] || '' }),
      })
      setPasswords((current) => ({ ...current, [user.id]: '' }))
      if (user.id === currentUserId) {
        await api('/api/auth/me')
      }
      setNotice(`Password updated for ${user.username}. They sign in again with the new one.`)
    } catch (err) {
      setError(err)
    }
  }

  async function changeStatus(user) {
    setError(null)
    setNotice('')
    try {
      await api(`/api/users/${user.id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ active: !user.active }),
      })
      setUsers(await api('/api/users'))
    } catch (err) {
      setError(err)
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Staff"
        title="Who can sign in."
        text="Deactivating an account keeps their old receipts. Setting a new password signs that person out until they use it."
      />
      <Banner error={error} />
      {notice && <p className="success">{notice}</p>}
      <section className="split">
        <form className="panel" onSubmit={create}>
          <h3>New account</h3>
          <label>
            Name
            <input value={form.name} onChange={(event) => update('name', event.target.value)} required />
          </label>
          <label>
            Username
            <input value={form.username} onChange={(event) => update('username', event.target.value)} required />
          </label>
          <label>
            Password
            <input type="password" value={form.password} onChange={(event) => update('password', event.target.value)} minLength={8} required />
          </label>
          <label>
            Role
            <select value={form.role} onChange={(event) => update('role', event.target.value)}>
              <option value="STAFF">Staff</option>
              <option value="ADMINISTRATOR">Administrator</option>
            </select>
          </label>
          <button className="primary" type="submit" disabled={saving}>{saving ? 'Saving…' : 'Add account'}</button>
        </form>
        <section className="panel">
          <h3>Accounts</h3>
          {!users && <p className="muted">Loading accounts…</p>}
          {users?.length === 0 && <p className="muted">No accounts yet.</p>}
          {users?.length > 0 && (
            <ul className="account-list">
              {users.map((user) => (
                <li key={user.id}>
                  <div>
                    <strong>{user.name}</strong>
                    <span>{user.username} · {user.role === 'ADMINISTRATOR' ? 'Administrator' : 'Staff'}{user.active ? '' : ' · inactive'}</span>
                  </div>
                  <form className="password-reset" onSubmit={(event) => resetPassword(event, user)}>
                    <input
                      type="password"
                      minLength={8}
                      placeholder="New password"
                      autoComplete="new-password"
                      value={passwords[user.id] || ''}
                      onChange={(event) => setPasswords((current) => ({ ...current, [user.id]: event.target.value }))}
                      required
                    />
                    <button type="submit" className="ghost">Set password</button>
                    <button type="button" className="ghost" onClick={() => changeStatus(user)}>
                      {user.active ? 'Deactivate' : 'Activate'}
                    </button>
                  </form>
                </li>
              ))}
            </ul>
          )}
        </section>
      </section>
    </>
  )
}
