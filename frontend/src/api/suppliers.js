import { request, download } from './request.js'

export const getSuppliers = (query) => request('/suppliers', { query })
export const createSupplier = (body) => request('/suppliers', { method: 'POST', body })
export const updateSupplier = (id, body) => request(`/suppliers/${id}`, { method: 'PUT', body })
export const deleteSupplier = (id) => request(`/suppliers/${id}`, { method: 'DELETE' })
export const updateSupplierStatus = (id, status) => request(`/suppliers/${id}/status`, { method: 'PUT', body: { status } })
export const exportSuppliers = (query) => download('/suppliers/export', { query, fallbackName: '供应商列表.xls' })
export const getSupplierOptions = (query) => request('/suppliers/options', { query })
