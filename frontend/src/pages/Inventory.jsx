import { useEffect, useState } from 'react'
import { api, when } from '../api.js'
import { Banner, PageHeader } from '../ui.jsx'

export default function Inventory() {
  const [search, setSearch] = useState('')
  const [matches, setMatches] = useState([])
  const [product, setProduct] = useState(null)
  const [receiptQty, setReceiptQty] = useState('1')
  const [note, setNote] = useState('')
  const [adjustQty, setAdjustQty] = useState('1')
  const [direction, setDirection] = useState('DECREASE')
  const [reason, setReason] = useState('')
  const [movements, setMovements] = useState(null)
  const [error, setError] = useState(null)
  const [message, setMessage] = useState('')

  useEffect(() => {
    let cancelled = false
    const params = new URLSearchParams({ size: '8', active: 'true' })
    if (search.trim()) {
      params.set('search', search.trim())
    }
    api(`/api/products?${params}`)
      .then((body) => {
        if (!cancelled) {
          setMatches(body.content)
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err)
        }
      })
    return () => {
      cancelled = true
    }
  }, [search])

  useEffect(() => {
    let cancelled = false
    const params = new URLSearchParams({ size: '8' })
    if (product) {
      params.set('productId', String(product.id))
    }
    api(`/api/inventory/movements?${params}`)
      .then((body) => {
        if (!cancelled) {
          setMovements(body)
        }
      })
      .catch((err) => {
        if (!cancelled) {
          setError(err)
        }
      })
    return () => {
      cancelled = true
    }
  }, [product])

  async function refreshProduct(id) {
    const updated = await api(`/api/products/${id}`)
    setProduct(updated)
    setMatches((current) => current.map((item) => (item.id === updated.id ? updated : item)))
    setMovements(await api(`/api/inventory/movements?productId=${id}&size=8`))
  }

  async function receive(event) {
    event.preventDefault()
    if (!product) return
    setError(null)
    setMessage('')
    try {
      await api('/api/inventory/receipts', {
        method: 'POST',
        body: JSON.stringify({
          productId: product.id,
          quantity: Number(receiptQty),
          note: note.trim() || null,
        }),
      })
      setNote('')
      setReceiptQty('1')
      await refreshProduct(product.id)
      setMessage(`Received ${receiptQty} into ${product.name}.`)
    } catch (err) {
      setError(err)
    }
  }

  async function adjust(event) {
    event.preventDefault()
    if (!product) return
    setError(null)
    setMessage('')
    try {
      await api('/api/inventory/adjustments', {
        method: 'POST',
        body: JSON.stringify({
          productId: product.id,
          quantity: Number(adjustQty),
          direction,
          reason: reason.trim(),
        }),
      })
      setReason('')
      setAdjustQty('1')
      await refreshProduct(product.id)
      setMessage(`Adjusted ${product.name}.`)
    } catch (err) {
      setError(err)
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Inventory"
        title="Stock only changes with a reason."
        text="A receipt records stock arriving. An adjustment records a count, damage, or other correction. Sales deduct stock on their own."
      />
      <Banner error={error} />
      {message && <p className="success">{message}</p>}
      <section className="panel">
        <label>
          Find a product
          <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Name or SKU" />
        </label>
        <div className="choice-list">
          {matches.map((item) => (
            <button
              key={item.id}
              type="button"
              className={product?.id === item.id ? 'choice selected' : 'choice'}
              onClick={() => setProduct(item)}
            >
              <strong>{item.name}</strong>
              <span>{item.sku} · {item.stockQuantity} on hand</span>
            </button>
          ))}
          {matches.length === 0 && <p className="muted">No active products match that search.</p>}
        </div>
      </section>
      {product && (
        <section className="split">
          <form className="panel" onSubmit={receive}>
            <h3>Receive stock</h3>
            <p className="muted">{product.name} currently has {product.stockQuantity}.</p>
            <label>
              Quantity
              <input type="number" min="1" value={receiptQty} onChange={(event) => setReceiptQty(event.target.value)} required />
            </label>
            <label>
              Note
              <input value={note} onChange={(event) => setNote(event.target.value)} placeholder="Supplier delivery" />
            </label>
            <button className="primary" type="submit">Record receipt</button>
          </form>
          <form className="panel" onSubmit={adjust}>
            <h3>Adjust stock</h3>
            <label>
              Direction
              <select value={direction} onChange={(event) => setDirection(event.target.value)}>
                <option value="INCREASE">Increase</option>
                <option value="DECREASE">Decrease</option>
              </select>
            </label>
            <label>
              Quantity
              <input type="number" min="1" value={adjustQty} onChange={(event) => setAdjustQty(event.target.value)} required />
            </label>
            <label>
              Reason
              <input value={reason} onChange={(event) => setReason(event.target.value)} required placeholder="Damaged on delivery" />
            </label>
            <button className="primary" type="submit">Record adjustment</button>
          </form>
        </section>
      )}
      <section className="panel">
        <h3>{product ? `History for ${product.name}` : 'Latest movements'}</h3>
        {movements?.content.length === 0 && <p className="muted">No stock movements yet.</p>}
        {movements?.content.length > 0 && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>When</th>
                  <th>Product</th>
                  <th>Type</th>
                  <th>Change</th>
                  <th>After</th>
                  <th>By</th>
                  <th>Reason</th>
                </tr>
              </thead>
              <tbody>
                {movements.content.map((movement) => (
                  <tr key={movement.id}>
                    <td>{when(movement.createdAt)}</td>
                    <td>{movement.productName}</td>
                    <td>{movement.type}</td>
                    <td>{movement.quantityChange > 0 ? `+${movement.quantityChange}` : movement.quantityChange}</td>
                    <td>{movement.stockAfter}</td>
                    <td>{movement.recordedByName || '—'}</td>
                    <td>{movement.reason || '—'}</td>
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
