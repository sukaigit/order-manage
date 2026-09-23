const BASE = import.meta.env.VITE_API_BASE || '/api'

function buildQuery(query) {
  if (!query) return ''
  const qs = new URLSearchParams(
    Object.entries(query).filter(([, v]) => v !== undefined && v !== null && v !== '')
  ).toString()
  return qs ? '?' + qs : ''
}

function authHeaders() {
  const token = localStorage.getItem('token')
  return token ? { Authorization: 'Bearer ' + token } : {}
}

function handle401(payload) {
  localStorage.removeItem('token')
  localStorage.removeItem('session')
  if (!location.pathname.startsWith('/login')) location.href = '/login'
  throw Object.assign(new Error(payload?.message || '未认证'), { code: 401, payload })
}

export async function request(path, { method = 'GET', body, query } = {}) {
  const res = await fetch(BASE + path + buildQuery(query), {
    method,
    headers: { 'Content-Type': 'application/json', ...authHeaders() },
    body: body ? JSON.stringify(body) : undefined,
  })
  const payload = await res.json().catch(() => null)
  if (res.status === 401) handle401(payload)
  if (!res.ok || payload?.code !== 200) {
    const err = new Error(payload?.message || '请求失败')
    err.code = res.status
    err.payload = payload
    throw err
  }
  return payload.data
}

export async function download(path, { query, fallbackName } = {}) {
  const res = await fetch(BASE + path + buildQuery(query), { headers: authHeaders() })
  if (res.status === 401) {
    const payload = await res.json().catch(() => null)
    handle401(payload)
  }
  if (!res.ok) {
    const payload = await res.json().catch(() => null)
    const err = new Error(payload?.message || '导出失败')
    err.code = res.status
    err.payload = payload
    throw err
  }
  const blob = await res.blob()
  const dispo = res.headers.get('Content-Disposition') || ''
  const m = /filename\*?=(?:UTF-8'')?"?([^";]+)"?/i.exec(dispo)
  let filename = m ? decodeURIComponent(m[1]) : (fallbackName || 'export.xls')
  if (!/\.(xls|xlsx|csv)$/i.test(filename)) filename += '.xls'
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
}
