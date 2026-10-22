import { useEffect, useState } from 'react'
import { api } from '../api.js'
import { Banner, PageHeader } from '../ui.jsx'

const emptyForm = { name: '', username: '', password: '', role: 'STAFF' }

export default function Staff() {
  const [users, setUsers] = useState(null)
  const [form, setForm] = useState(emptyForm)
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

  async function changeStatus(user) {
    setError(null)
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
        text="Deactivating an account keeps their old receipts. They cannot sign in until the account is active again."
      />
      <Banner error={error} />
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
            <div className="table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Username</th>
                    <th>Role</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((user) => (
                    <tr key={user.id}>
                      <td>{user.name}</td>
                      <td>{user.username}</td>
                      <td>{user.role === 'ADMINISTRATOR' ? 'Administrator' : 'Staff'}{user.active ? '' : ' · inactive'}</td>
                      <td>
                        <button type="button" className="ghost" onClick={() => changeStatus(user)}>
                          {user.active ? 'Deactivate' : 'Activate'}
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      </section>
    </>
  )
}
