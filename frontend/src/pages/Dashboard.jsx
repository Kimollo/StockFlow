import { useEffect, useState } from 'react'
import { api, money, todayInput, shiftDate } from '../api.js'
import { Banner, PageHeader } from '../ui.jsx'

const periods = [
  { id: 'today', label: 'Today', days: 0 },
  { id: 'week', label: '7 days', days: 6 },
  { id: 'month', label: '30 days', days: 29 },
]

export default function Dashboard() {
  const [period, setPeriod] = useState('today')
  const [summary, setSummary] = useState(null)
  const [trend, setTrend] = useState([])
  const [top, setTop] = useState([])
  const [lowStock, setLowStock] = useState([])
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  const today = todayInput()
  const selected = periods.find((item) => item.id === period)
  const from = shiftDate(today, -selected.days)
  const to = today

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)
    const query = `from=${from}&to=${to}`
    Promise.all([
      api(`/api/dashboard/summary?${query}`),
      api(`/api/dashboard/sales-trend?${query}`),
      api(`/api/dashboard/top-products?${query}&limit=5`),
      api('/api/products?active=true&lowStock=true&size=6'),
    ])
      .then(([summaryBody, trendBody, topBody, lowBody]) => {
        if (cancelled) {
          return
        }
        setSummary(summaryBody)
        setTrend(trendBody.points)
        setTop(topBody.products)
        setLowStock(lowBody.content)
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err)
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false)
        }
      })
    return () => {
      cancelled = true
    }
  }, [from, to])

  async function downloadCsv() {
    try {
      const csv = await api(`/api/dashboard/sales.csv?from=${from}&to=${to}`)
      const blob = new Blob([csv], { type: 'text/csv' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `stockflow-sales-${from}-to-${to}.csv`
      link.click()
      URL.revokeObjectURL(url)
    } catch (err) {
      setError(err)
    }
  }

  const max = Math.max(...trend.map((point) => Number(point.total)), 0)

  return (
    <>
      <PageHeader
        eyebrow="Dashboard"
        title="How the shop is doing."
        text="Totals come from saved sales, with returns taken off. Today's cards stay on today even when the chart covers a longer period."
      >
        <div className="row">
          {periods.map((item) => (
            <button
              key={item.id}
              type="button"
              className={item.id === period ? 'primary' : 'ghost'}
              onClick={() => setPeriod(item.id)}
            >
              {item.label}
            </button>
          ))}
          <button type="button" className="ghost" onClick={downloadCsv}>Download CSV</button>
        </div>
      </PageHeader>
      <Banner error={error} />
      {loading && <p className="muted">Loading the shop figures…</p>}
      {summary && (
        <section className="status-grid">
          <article>
            <p>Today</p>
            <strong>{money(summary.todayTotal)}</strong>
            <span>{summary.todayCount} sale{summary.todayCount === 1 ? '' : 's'}</span>
          </article>
          <article>
            <p>This period</p>
            <strong>{money(summary.periodTotal)}</strong>
            <span>{summary.periodCount} sale{summary.periodCount === 1 ? '' : 's'}</span>
          </article>
          <article>
            <p>Active products</p>
            <strong>{summary.activeProducts}</strong>
            <span>Still available to sell</span>
          </article>
          <article>
            <p>Low stock</p>
            <strong className={summary.lowStockProducts > 0 ? 'down' : 'up'}>{summary.lowStockProducts}</strong>
            <span>At or below the minimum</span>
          </article>
        </section>
      )}
      <section className="split">
        <article>
          <p>Sales by day</p>
          {trend.length === 0 && <span>No days in this period.</span>}
          {trend.length > 0 && max === 0 && <span>No sales in this period.</span>}
          {trend.length > 0 && (
            <div className="chart" aria-label="Daily sales totals">
              {trend.map((point) => (
                <div key={point.date} className="bar-slot" title={`${point.date}: ${money(point.total)}`}>
                  <div
                    className={Number(point.total) > 0 ? 'bar' : 'bar empty'}
                    style={{ height: `${max === 0 ? 8 : Math.max(8, (Number(point.total) / max) * 100)}%` }}
                  />
                </div>
              ))}
            </div>
          )}
          {trend.length > 0 && (
            <span className="chart-caption">{trend[0].date} to {trend[trend.length - 1].date}</span>
          )}
        </article>
        <article>
          <p>Best sellers</p>
          {top.length === 0 && <span>No products sold in this period.</span>}
          {top.length > 0 && (
            <ul className="plain-list">
              {top.map((product) => (
                <li key={product.productId}>
                  <strong>{product.name}</strong>
                  <span>{product.quantitySold} sold · {money(product.revenue)}</span>
                </li>
              ))}
            </ul>
          )}
        </article>
      </section>
      <section className="panel">
        <h3>Needs restocking</h3>
        {lowStock.length === 0 && <p className="muted">No active products are at or below their minimum.</p>}
        {lowStock.length > 0 && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>SKU</th>
                  <th>Product</th>
                  <th>On hand</th>
                  <th>Minimum</th>
                </tr>
              </thead>
              <tbody>
                {lowStock.map((product) => (
                  <tr key={product.id}>
                    <td>{product.sku}</td>
                    <td>{product.name}</td>
                    <td>{product.stockQuantity}</td>
                    <td>{product.minimumStockLevel}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>
    </>
  )
}
