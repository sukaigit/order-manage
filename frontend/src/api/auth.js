import { request } from './request.js'

export function getToken() { return localStorage.getItem('token') }
export function setToken(token) { localStorage.setItem('token', token) }
export function getSession() {
  try { return JSON.parse(localStorage.getItem('session') || 'null') } catch { return null }
}
export function setSession(session) { localStorage.setItem('session', JSON.stringify(session)) }
export function clearAuth() {
  localStorage.removeItem('token')
  localStorage.removeItem('session')
}

export const getCaptcha = () => request('/auth/captcha')
export const login = (body) => request('/auth/login', { method: 'POST', body })
export const getMe = () => request('/auth/me')
export const logout = () => request('/auth/logout', { method: 'POST' })
export const changePassword = (body) => request('/auth/password', { method: 'PUT', body })
export const forceChangePassword = (body) => request('/auth/password/force', { method: 'PUT', body })
