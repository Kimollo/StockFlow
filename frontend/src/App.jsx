import { useEffect, useState } from 'react'
import { onUnauthorized, setToken } from './api.js'
import Dashboard from './pages/Dashboard.jsx'
import Products from './pages/Products.jsx'
import Inventory from './pages/Inventory.jsx'
import Checkout from './pages/Checkout.jsx'
import Sales from './pages/Sales.jsx'
import Login from './pages/Login.jsx'
import Staff from './pages/Staff.jsx'

const pages = [
  { id: 'dashboard', label: 'Dashboard', detail: 'Today and the period', admin: true },
  { id: 'products', label: 'Products', detail: 'SKUs, prices, categories' },
  { id: 'inventory', label: 'Inventory', detail: 'Receipts and adjustments', admin: true },
  { id: 'checkout', label: 'Checkout', detail: 'Sell and keep a receipt' },
  { id: 'sales', label: 'Sales', detail: 'History that stays put' },
  { id: 'staff', label: 'Staff', detail: 'Accounts and access', admin: true },
]

const views = {
  dashboard: Dashboard,
  products: Products,
  inventory: Inventory,
  checkout: Checkout,
  sales: Sales,
  staff: Staff,
}

function readSession() {
  const raw = sessionStorage.getItem('stockflow.session')
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw)
  } catch {
    return null
  }
}

export default function App() {
  const [session, setSession] = useState(() => {
    const existing = readSession()
    setToken(existing?.token)
    return existing
  })
  const [page, setPage] = useState('dashboard')

  useEffect(() => {
    onUnauthorized(() => {
      sessionStorage.removeItem('stockflow.session')
      setToken(null)
      setSession(null)
    })
  }, [])

  if (!session) {
    return (
      <Login onSuccess={(body) => {
        const next = { token: body.token, user: body.user }
        sessionStorage.setItem('stockflow.session', JSON.stringify(next))
        setToken(next.token)
        setSession(next)
        setPage(body.user.role === 'ADMINISTRATOR' ? 'dashboard' : 'checkout')
      }} />
    )
  }

  const admin = session.user.role === 'ADMINISTRATOR'
  const visible = pages.filter((item) => admin || !item.admin)
  const current = visible.some((item) => item.id === page) ? page : visible[0].id
  const Current = views[current]

  async function logout() {
    try {
      await fetch('/api/auth/logout', { method: 'POST', headers: { Authorization: `Bearer ${session.token}` } })
    } catch {
      // The token is discarded locally either way.
    }
    sessionStorage.removeItem('stockflow.session')
    setToken(null)
    setSession(null)
  }

  return (
    <div className="shell">
      <aside>
        <div className="brand">
          <span className="mark">SF</span>
          <div>
            <p className="eyebrow">stokivo</p>
            <h1>StockFlow</h1>
          </div>
        </div>
        <p className="aside-copy">{session.user.name} · {admin ? 'Administrator' : 'Staff'}</p>
        <nav>
          {visible.map((item) => (
            <button
              key={item.id}
              type="button"
              className={current === item.id ? 'nav-item current' : 'nav-item'}
              aria-current={current === item.id ? 'page' : undefined}
              onClick={() => setPage(item.id)}
            >
              <strong>{item.label}</strong>
              <span>{item.detail}</span>
            </button>
          ))}
        </nav>
        <button type="button" className="ghost" onClick={logout}>Log out</button>
      </aside>
      <main>
        {current === 'products' && <Products canManage={admin} />}
        {current === 'sales' && <Sales ownOnly={!admin} />}
        {current === 'staff' && <Staff currentUserId={session.user.id} />}
        {current !== 'products' && current !== 'sales' && current !== 'staff' && <Current />}
      </main>
    </div>
  )
}
