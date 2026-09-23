import { request } from './request.js'

export const getStats = () => request('/workbench/stats')
