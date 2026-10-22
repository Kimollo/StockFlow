import { useState } from 'react'
import Dashboard from './pages/Dashboard.jsx'
import Products from './pages/Products.jsx'
import Inventory from './pages/Inventory.jsx'
import Checkout from './pages/Checkout.jsx'
import Sales from './pages/Sales.jsx'

const pages = [
  { id: 'dashboard', label: 'Dashboard', detail: 'Today and the period' },
  { id: 'products', label: 'Products', detail: 'SKUs, prices, categories' },
  { id: 'inventory', label: 'Inventory', detail: 'Receipts and adjustments' },
  { id: 'checkout', label: 'Checkout', detail: 'Sell and keep a receipt' },
  { id: 'sales', label: 'Sales', detail: 'History that stays put' },
]

const views = {
  dashboard: Dashboard,
  products: Products,
  inventory: Inventory,
  checkout: Checkout,
  sales: Sales,
}

export default function App() {
  const [page, setPage] = useState('dashboard')
  const Current = views[page]

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
        <p className="aside-copy">Inventory and sales for a small shop.</p>
        <nav>
          {pages.map((item) => (
            <button
              key={item.id}
              type="button"
              className={page === item.id ? 'nav-item current' : 'nav-item'}
              aria-current={page === item.id ? 'page' : undefined}
              onClick={() => setPage(item.id)}
            >
              <strong>{item.label}</strong>
              <span>{item.detail}</span>
            </button>
          ))}
        </nav>
      </aside>
      <main>
        <Current />
      </main>
    </div>
  )
}
