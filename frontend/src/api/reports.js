import { request, download } from './request.js'

export const getSummary = (query) => request('/reports/summary', { query })
export const getDetail = (query) => request('/reports/detail', { query })
export const exportReport = (query) => download('/reports/export', { query, fallbackName: '订单明细报表.xls' })
