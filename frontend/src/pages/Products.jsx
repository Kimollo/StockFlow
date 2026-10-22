import { useEffect, useState } from 'react'
import { api, money } from '../api.js'
import { Banner, PageHeader } from '../ui.jsx'

const emptyForm = {
  sku: '',
  name: '',
  categoryId: '',
  sellingPrice: '',
  costPrice: '',
  initialQuantity: '0',
  minimumStockLevel: '0',
}

export default function Products({ canManage = true }) {
  const [products, setProducts] = useState(null)
  const [categories, setCategories] = useState([])
  const [search, setSearch] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [active, setActive] = useState('true')
  const [lowStock, setLowStock] = useState(false)
  const [page, setPage] = useState(0)
  const [form, setForm] = useState(emptyForm)
  const [editingId, setEditingId] = useState(null)
  const [categoryName, setCategoryName] = useState('')
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    api('/api/categories')
      .then(setCategories)
      .catch(setError)
  }, [])

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    const params = new URLSearchParams({ page: String(page), size: '8' })
    if (search.trim()) {
      params.set('search', search.trim())
    }
    if (categoryId) {
      params.set('categoryId', categoryId)
    }
    if (active) {
      params.set('active', active)
    }
    if (lowStock) {
      params.set('lowStock', 'true')
    }
    api(`/api/products?${params}`)
      .then((body) => {
        if (!cancelled) {
          setProducts(body)
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
  }, [search, categoryId, active, lowStock, page])

  function update(field, value) {
    setForm((current) => ({ ...current, [field]: value }))
  }

  function edit(product) {
    setEditingId(product.id)
    setForm({
      sku: product.sku,
      name: product.name,
      categoryId: String(product.categoryId),
      sellingPrice: money(product.sellingPrice),
      costPrice: money(product.costPrice),
      initialQuantity: '0',
      minimumStockLevel: String(product.minimumStockLevel),
    })
    setError(null)
  }

  function resetForm() {
    setEditingId(null)
    setForm(emptyForm)
  }

  async function saveCategory(event) {
    event.preventDefault()
    setError(null)
    try {
      const created = await api('/api/categories', {
        method: 'POST',
        body: JSON.stringify({ name: categoryName.trim() }),
      })
      setCategories((current) => [...current, created].sort((a, b) => a.name.localeCompare(b.name)))
      setCategoryName('')
      update('categoryId', String(created.id))
    } catch (err) {
      setError(err)
    }
  }

  async function saveProduct(event) {
    event.preventDefault()
    setSaving(true)
    setError(null)
    const payload = {
      sku: form.sku.trim(),
      name: form.name.trim(),
      categoryId: Number(form.categoryId),
      sellingPrice: form.sellingPrice,
      costPrice: form.costPrice,
      minimumStockLevel: Number(form.minimumStockLevel),
    }
    try {
      if (editingId) {
        await api(`/api/products/${editingId}`, { method: 'PUT', body: JSON.stringify(payload) })
      } else {
        payload.initialQuantity = Number(form.initialQuantity)
        await api('/api/products', { method: 'POST', body: JSON.stringify(payload) })
      }
      resetForm()
      setPage(0)
      setSearch((current) => current)
      const params = new URLSearchParams({ page: '0', size: '8' })
      if (search.trim()) params.set('search', search.trim())
      if (categoryId) params.set('categoryId', categoryId)
      if (active) params.set('active', active)
      if (lowStock) params.set('lowStock', 'true')
      setProducts(await api(`/api/products?${params}`))
    } catch (err) {
      setError(err)
    } finally {
      setSaving(false)
    }
  }

  async function changeStatus(product) {
    setError(null)
    try {
      await api(`/api/products/${product.id}/status`, {
        method: 'PATCH',
        body: JSON.stringify({ active: !product.active }),
      })
      setProducts(await reload())
    } catch (err) {
      setError(err)
    }
  }

  function reload() {
    const params = new URLSearchParams({ page: String(page), size: '8' })
    if (search.trim()) params.set('search', search.trim())
    if (categoryId) params.set('categoryId', categoryId)
    if (active) params.set('active', active)
    if (lowStock) params.set('lowStock', 'true')
    return api(`/api/products?${params}`)
  }

  return (
    <>
      <PageHeader
        eyebrow="Products"
        title="What the shop sells."
        text={canManage
          ? 'Deactivating a product keeps it on old receipts. Stock changes happen in Inventory, not in this form.'
          : 'Prices and stock levels for what is on the shelf. A manager changes the catalog.'}
      />
      <Banner error={error} />
      {canManage && (
      <section className="split">
        <form className="panel" onSubmit={saveProduct}>
          <h3>{editingId ? 'Edit product' : 'New product'}</h3>
          <label>
            SKU
            <input value={form.sku} onChange={(event) => update('sku', event.target.value)} required />
          </label>
          <label>
            Name
            <input value={form.name} onChange={(event) => update('name', event.target.value)} required />
          </label>
          <label>
            Category
            <select value={form.categoryId} onChange={(event) => update('categoryId', event.target.value)} required>
              <option value="">Choose a category</option>
              {categories.map((category) => (
                <option key={category.id} value={category.id}>{category.name}</option>
              ))}
            </select>
          </label>
          <div className="field-row">
            <label>
              Selling price
              <input value={form.sellingPrice} onChange={(event) => update('sellingPrice', event.target.value)} inputMode="decimal" required />
            </label>
            <label>
              Cost price
              <input value={form.costPrice} onChange={(event) => update('costPrice', event.target.value)} inputMode="decimal" required />
            </label>
          </div>
          <div className="field-row">
            {!editingId && (
              <label>
                Opening stock
                <input type="number" min="0" value={form.initialQuantity} onChange={(event) => update('initialQuantity', event.target.value)} required />
              </label>
            )}
            <label>
              Minimum
              <input type="number" min="0" value={form.minimumStockLevel} onChange={(event) => update('minimumStockLevel', event.target.value)} required />
            </label>
          </div>
          <div className="row">
            <button className="primary" type="submit" disabled={saving}>{saving ? 'Saving…' : editingId ? 'Save changes' : 'Add product'}</button>
            {editingId && <button className="ghost" type="button" onClick={resetForm}>Cancel</button>}
          </div>
        </form>
        <form className="panel" onSubmit={saveCategory}>
          <h3>New category</h3>
          <label>
            Name
            <input value={categoryName} onChange={(event) => setCategoryName(event.target.value)} required />
          </label>
          <button className="ghost" type="submit">Add category</button>
        </form>
      </section>
      )}
      <section className="panel">
        <div className="filters">
          <input
            placeholder="Search name or SKU"
            value={search}
            onChange={(event) => {
              setPage(0)
              setSearch(event.target.value)
            }}
          />
          <select value={categoryId} onChange={(event) => { setPage(0); setCategoryId(event.target.value) }}>
            <option value="">All categories</option>
            {categories.map((category) => (
              <option key={category.id} value={category.id}>{category.name}</option>
            ))}
          </select>
          <select value={active} onChange={(event) => { setPage(0); setActive(event.target.value) }}>
            <option value="true">Active</option>
            <option value="false">Inactive</option>
            <option value="">All</option>
          </select>
          <label className="check">
            <input type="checkbox" checked={lowStock} onChange={(event) => { setPage(0); setLowStock(event.target.checked) }} />
            Low stock
          </label>
        </div>
        {loading && <p className="muted">Loading products…</p>}
        {!loading && products?.content.length === 0 && <p className="muted">No products match these filters.</p>}
        {products?.content.length > 0 && (
          <div className="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>SKU</th>
                  <th>Name</th>
                  <th>Category</th>
                  <th>Price</th>
                  <th>Stock</th>
                  <th>Status</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {products.content.map((product) => (
                  <tr key={product.id}>
                    <td>{product.sku}</td>
                    <td>{product.name}</td>
                    <td>{product.categoryName}</td>
                    <td>{money(product.sellingPrice)}</td>
                    <td>{product.stockQuantity}</td>
                    <td><span className={badgeClass(product)}>{badge(product)}</span></td>
                    {canManage && (
                      <td className="actions">
                        <button type="button" className="ghost" onClick={() => edit(product)}>Edit</button>
                        <button type="button" className="ghost" onClick={() => changeStatus(product)}>
                          {product.active ? 'Deactivate' : 'Activate'}
                        </button>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
        {products && products.totalPages > 1 && (
          <div className="row">
            <button type="button" className="ghost" disabled={page === 0} onClick={() => setPage((current) => current - 1)}>Previous</button>
            <span className="muted">Page {page + 1} of {products.totalPages}</span>
            <button type="button" className="ghost" disabled={page + 1 >= products.totalPages} onClick={() => setPage((current) => current + 1)}>Next</button>
          </div>
        )}
      </section>
    </>
  )
}

function badge(product) {
  if (!product.active) return 'Inactive'
  if (product.outOfStock) return 'Out of stock'
  if (product.lowStock) return 'Low'
  return 'In stock'
}

function badgeClass(product) {
  if (!product.active || product.outOfStock) return 'badge bad'
  if (product.lowStock) return 'badge warn'
  return 'badge ok'
}
