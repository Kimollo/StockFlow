import { useEffect, useState } from 'react'
import { api, money, when } from '../api.js'
import { Receipt } from './Checkout.jsx'
import { Banner, PageHeader } from '../ui.jsx'

export default function Sales({ ownOnly = false }) {
  const [sales, setSales] = useState(null)
  const [page, setPage] = useState(0)
  const [selected, setSelected] = useState(null)
  const [returnQty, setReturnQty] = useState({})
  const [returning, setReturning] = useState(false)
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    api(`/api/sales?page=${page}&size=8`)
      .then((body) => {
        if (!cancelled) {
          setSales(body)
        }
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
  }, [page])

  async function open(sale) {
    setError(null)
    setReturnQty({})
    try {
      setSelected(await api(`/api/sales/${sale.id}`))
    } catch (err) {
      setError(err)
    }
  }

  async function submitReturn(event) {
    event.preventDefault()
    const items = selected.items
      .map((item) => ({ saleItemId: item.id, quantity: Number(returnQty[item.id] || 0) }))
      .filter((item) => item.quantity > 0)
    if (items.length === 0) {
      setError(new Error('Choose a quantity to return.'))
      return
    }
    setReturning(true)
    setError(null)
    try {
      const updated = await api(`/api/sales/${selected.id}/returns`, {
        method: 'POST',
        body: JSON.stringify({ items }),
      })
      setSelected(updated)
      setReturnQty({})
    } catch (err) {
      setError(err)
    } finally {
      setReturning(false)
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Sales"
        title="Receipts stay as they were sold."
        text={ownOnly
          ? 'These are the sales you completed.'
          : 'A later price change or a deactivated product does not rewrite these lines.'}
      />
      <Banner error={error} />
      <section className="panel">
        {loading && <p className="muted">Loading sales…</p>}
        {!loading && sales?.content.length === 0 && <p className="muted">No sales yet. Complete one from Checkout.</p>}
        {sales?.content.length > 0 && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>Receipt</th>
                  <th>When</th>
                  <th>Payment</th>
                  <th>Cashier</th>
                  <th>Items</th>
                  <th>Total</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {sales.content.map((sale) => (
                  <tr key={sale.id}>
                    <td>{sale.saleNumber}</td>
                    <td>{when(sale.soldAt)}</td>
                    <td>{sale.paymentMethod}</td>
                    <td>{sale.cashierName || '—'}</td>
                    <td>{sale.items.length}</td>
                    <td>{money(sale.total)}</td>
                    <td><button type="button" className="ghost" onClick={() => open(sale)}>View</button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {sales && sales.totalPages > 1 && (
          <div className="row">
            <button type="button" className="ghost" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>Previous</button>
            <span className="muted">Page {page + 1} of {sales.totalPages}</span>
            <button type="button" className="ghost" disabled={page + 1 >= sales.totalPages} onClick={() => setPage((current) => current + 1)}>Next</button>
          </div>
        )}
      </section>
      {selected && <Receipt sale={selected} />}
      {selected && selected.items.some((item) => item.quantity - item.returnedQuantity > 0) && (
        <form className="panel" onSubmit={submitReturn}>
          <h3>Return stock</h3>
          <p className="muted">The receipt stays as it was sold. Returned quantities go back on the shelf.</p>
          {selected.items.filter((item) => item.quantity - item.returnedQuantity > 0).map((item) => {
            const remaining = item.quantity - item.returnedQuantity
            return (
              <label key={item.id}>
                {item.productName} · {remaining} still out
                <input
                  type="number"
                  min="0"
                  max={remaining}
                  value={returnQty[item.id] ?? ''}
                  placeholder="0"
                  onChange={(event) => setReturnQty((current) => ({ ...current, [item.id]: event.target.value }))}
                />
              </label>
            )
          })}
          <button className="primary" type="submit" disabled={returning}>{returning ? 'Returning…' : 'Return selected'}</button>
        </form>
      )}
    </>
  )
}
