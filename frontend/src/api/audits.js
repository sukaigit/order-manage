import { request } from './request.js'

export const getOrderAudits = (query) => request('/order-audits', { query })
export const passAudit = (id, body) => request(`/order-audits/${id}/pass`, { method: 'POST', body })
export const rejectAudit = (id, body) => request(`/order-audits/${id}/reject`, { method: 'POST', body })
