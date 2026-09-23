import { request, download } from './request.js'

export const getOrders = (query) => request('/orders', { query })
export const getOrder = (id) => request(`/orders/${id}`)
export const createOrder = (body) => request('/orders', { method: 'POST', body })
export const updateOrder = (id, body) => request(`/orders/${id}`, { method: 'PUT', body })
export const deleteOrder = (id) => request(`/orders/${id}`, { method: 'DELETE' })
export const exportOrders = (query) => download('/orders/export', { query, fallbackName: '订单列表.xls' })
export const getOrderAudits = (id) => request(`/orders/${id}/audits`)
