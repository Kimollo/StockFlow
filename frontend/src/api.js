let token = null
let onSignedOut = () => {}

export function setToken(next) {
  token = next || null
}

export function onUnauthorized(handler) {
  onSignedOut = handler
}

export async function api(path, options = {}) {
  const headers = { ...(options.headers || {}) }
  if (options.body && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json'
  }
  if (token) {
    headers.Authorization = `Bearer ${token}`
  }
  const response = await fetch(path, { ...options, headers })
  if (response.status === 401 && !path.startsWith('/api/auth/login')) {
    onSignedOut()
  }
  if (response.status === 204) {
    return null
  }
  const text = await response.text()
  const isCsv = (response.headers.get('content-type') || '').includes('text/csv')
  const body = !text || isCsv ? text : JSON.parse(text)
  if (!response.ok) {
    const error = new Error(body?.message || `Request failed (${response.status})`)
    error.status = response.status
    error.errors = body?.errors || null
    throw error
  }
  return body
}

export function money(value) {
  const amount = Number(value)
  if (Number.isNaN(amount)) {
    return '0.00'
  }
  return amount.toFixed(2)
}

export function when(value) {
  if (!value) {
    return ''
  }
  return new Date(value).toLocaleString(undefined, {
    dateStyle: 'medium',
    timeStyle: 'short',
  })
}

export function todayInput() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}

export function shiftDate(isoDate, days) {
  const date = new Date(`${isoDate}T12:00:00`)
  date.setDate(date.getDate() + days)
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}
