import { useEffect, useState } from 'react'

const modules = [
  { name: 'Products', detail: 'SKUs, prices, and categories' },
  { name: 'Inventory', detail: 'Receipts, adjustments, and history' },
  { name: 'Sales', detail: 'Checkout, receipts, and stock deduction' },
  { name: 'Reports', detail: 'Dashboard totals and charts' },
  { name: 'Staff', detail: 'Login and administrator permissions' },
]

export default function App() {
  const [health, setHealth] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false

    fetch('/api/health')
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`API returned ${response.status}`)
        }
        return response.json()
      })
      .then((body) => {
        if (!cancelled) {
          setHealth(body)
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err.message || 'The API is not reachable')
        }
      })

    return () => {
      cancelled = true
    }
  }, [])

  const apiUp = health?.status === 'UP'
  const databaseUp = health?.database === 'UP'

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
        <p className="aside-copy">Inventory and sales for a small shop. Phase 1 is the foundation.</p>
        <nav>
          {modules.map((module) => (
            <div key={module.name} className="nav-item">
              <strong>{module.name}</strong>
              <span>{module.detail}</span>
            </div>
          ))}
        </nav>
      </aside>
      <main>
        <header>
          <p className="eyebrow">Phase 1</p>
          <h2>The shop system is connected.</h2>
          <p>React is talking to the Spring Boot API, and the API is talking to PostgreSQL.</p>
        </header>
        <section className="status-grid">
          <article>
            <p>API</p>
            <strong className={apiUp ? 'up' : error ? 'down' : 'wait'}>
              {apiUp ? 'Running' : error ? 'Unreachable' : 'Checking'}
            </strong>
            <span>{apiUp ? health.application : error || 'Waiting for /api/health'}</span>
          </article>
          <article>
            <p>PostgreSQL</p>
            <strong className={databaseUp ? 'up' : error ? 'down' : 'wait'}>
              {databaseUp ? 'Connected' : error ? 'Unknown' : 'Checking'}
            </strong>
            <span>{databaseUp ? 'stockflow database' : 'Connection is still being confirmed'}</span>
          </article>
        </section>
      </main>
    </div>
  )
}
