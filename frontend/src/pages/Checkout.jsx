import { useEffect, useState } from 'react'
import { api, money } from '../api.js'
import { Banner, PageHeader } from '../ui.jsx'

export default function Checkout() {
  const [search, setSearch] = useState('')
  const [matches, setMatches] = useState([])
  const [cart, setCart] = useState([])
  const [paymentMethod, setPaymentMethod] = useState('CASH')
  const [receipt, setReceipt] = useState(null)
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    let cancelled = false
    loadMatches(search)
      .then((content) => {
        if (!cancelled) {
          setMatches(content)
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

  function loadMatches(query) {
    const params = new URLSearchParams({ size: '8', active: 'true' })
    if (query.trim()) {
      params.set('search', query.trim())
    }
    return api(`/api/products?${params}`).then((body) => body.content)
  }

  function add(product) {
    setReceipt(null)
    setCart((current) => {
      const existing = current.find((line) => line.productId === product.id)
      if (existing) {
        return current.map((line) => (
          line.productId === product.id
            ? { ...line, quantity: Math.min(line.quantity + 1, line.stockQuantity) }
            : line
        ))
      }
      return [...current, {
        productId: product.id,
        sku: product.sku,
        name: product.name,
        unitPrice: product.sellingPrice,
        stockQuantity: product.stockQuantity,
        quantity: 1,
      }]
    })
  }

  function setQuantity(productId, quantity) {
    setCart((current) => current.flatMap((line) => {
      if (line.productId !== productId) {
        return [line]
      }
      if (quantity < 1) {
        return []
      }
      return [{ ...line, quantity: Math.min(quantity, line.stockQuantity) }]
    }))
  }

  const preview = cart.reduce((sum, line) => sum + Math.round(Number(line.unitPrice) * 100) * line.quantity, 0)

  async function complete(event) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    try {
      const sale = await api('/api/sales', {
        method: 'POST',
        body: JSON.stringify({
          paymentMethod,
          items: cart.map((line) => ({ productId: line.productId, quantity: line.quantity })),
        }),
      })
      setReceipt(sale)
      setCart([])
      setMatches(await loadMatches(search))
    } catch (err) {
      setError(err)
      const content = await loadMatches(search).catch(() => null)
      if (content) {
        setMatches(content)
        setCart((current) => current.flatMap((line) => {
          const fresh = content.find((item) => item.id === line.productId)
          if (!fresh || fresh.outOfStock) {
            return []
          }
          return [{
            ...line,
            unitPrice: fresh.sellingPrice,
            stockQuantity: fresh.stockQuantity,
            quantity: Math.min(line.quantity, fresh.stockQuantity),
          }]
        }))
      }
    } finally {
      setSaving(false)
    }
  }

  return (
    <>
      <PageHeader
        eyebrow="Checkout"
        title="Sell what is on the shelf."
        text="The cart shows catalog prices. The receipt uses the price stored by the server at the moment of the sale."
      />
      <Banner error={error} />
      <section className="split">
        <div className="panel">
          <label>
            Add a product
            <input value={search} onChange={(event) => setSearch(event.target.value)} placeholder="Name or SKU" />
          </label>
          <div className="choice-list">
            {matches.map((product) => (
              <button
                key={product.id}
                type="button"
                className="choice"
                disabled={product.outOfStock}
                onClick={() => add(product)}
              >
                <strong>{product.name}</strong>
                <span>{product.sku} · {money(product.sellingPrice)} · {product.stockQuantity} on hand</span>
              </button>
            ))}
            {matches.length === 0 && <p className="muted">No active products match that search.</p>}
          </div>
        </div>
        <form className="panel" onSubmit={complete}>
          <h3>Cart</h3>
          {cart.length === 0 && <p className="muted">The cart is empty.</p>}
          {cart.map((line) => (
            <div key={line.productId} className="cart-line">
              <div>
                <strong>{line.name}</strong>
                <span>{money(line.unitPrice)} each · {line.stockQuantity} available</span>
              </div>
              <input
                type="number"
                min="1"
                max={line.stockQuantity}
                value={line.quantity}
                onChange={(event) => setQuantity(line.productId, Number(event.target.value))}
              />
              <button type="button" className="ghost" onClick={() => setQuantity(line.productId, 0)}>Remove</button>
            </div>
          ))}
          <p className="total">Preview {money(preview / 100)}</p>
          <label>
            Payment
            <select value={paymentMethod} onChange={(event) => setPaymentMethod(event.target.value)}>
              <option value="CASH">Cash</option>
              <option value="CARD">Card</option>
              <option value="EFT">EFT</option>
            </select>
          </label>
          <button className="primary" type="submit" disabled={cart.length === 0 || saving}>
            {saving ? 'Completing…' : 'Complete sale'}
          </button>
        </form>
      </section>
      {receipt && <Receipt sale={receipt} />}
    </>
  )
}

export function Receipt({ sale }) {
  return (
    <section className="panel receipt">
      <p className="eyebrow">Receipt</p>
      <h3>{sale.saleNumber}</h3>
      <p className="muted">{sale.paymentMethod} · {sale.status}</p>
      <div className="table-wrap">
        <table>
          <thead>
            <tr>
              <th>Product</th>
              <th>Qty</th>
              <th>Price</th>
              <th>Line</th>
            </tr>
          </thead>
          <tbody>
            {sale.items.map((item) => (
              <tr key={item.id}>
                <td>{item.productName}<span className="sku">{item.sku}</span></td>
                <td>{item.quantity}</td>
                <td>{money(item.unitPrice)}</td>
                <td>{money(item.lineTotal)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="total">Total {money(sale.total)}</p>
    </section>
  )
}
