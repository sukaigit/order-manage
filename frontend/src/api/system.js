import { request } from './request.js'

export const getUsers = (query) => request('/users', { query })
export const createUser = (body) => request('/users', { method: 'POST', body })
export const updateUser = (id, body) => request(`/users/${id}`, { method: 'PUT', body })
export const updateUserStatus = (id, status) => request(`/users/${id}/status`, { method: 'PUT', body: { status } })
export const resetUserPassword = (id) => request(`/users/${id}/reset-password`, { method: 'PUT' })
export const deleteUser = (id) => request(`/users/${id}`, { method: 'DELETE' })

export const getRoles = (query) => request('/roles', { query })
export const createRole = (body) => request('/roles', { method: 'POST', body })
export const updateRole = (id, body) => request(`/roles/${id}`, { method: 'PUT', body })
export const deleteRole = (id) => request(`/roles/${id}`, { method: 'DELETE' })
export const getRolePermissions = (id) => request(`/roles/${id}/permissions`)
export const updateRolePermissions = (id, body) => request(`/roles/${id}/permissions`, { method: 'PUT', body })

export const getDepartments = (query) => request('/departments', { query })
export const createDepartment = (body) => request('/departments', { method: 'POST', body })
export const updateDepartment = (id, body) => request(`/departments/${id}`, { method: 'PUT', body })
export const deleteDepartment = (id) => request(`/departments/${id}`, { method: 'DELETE' })

export const getOrganizations = (query) => request('/organizations', { query })
export const createOrganization = (body) => request('/organizations', { method: 'POST', body })
export const updateOrganization = (id, body) => request(`/organizations/${id}`, { method: 'PUT', body })
export const deleteOrganization = (id) => request(`/organizations/${id}`, { method: 'DELETE' })

export const getMenus = (query) => request('/menus', { query })
export const getMenuTree = () => request('/menus/tree')
export const createMenu = (body) => request('/menus', { method: 'POST', body })
export const updateMenu = (id, body) => request(`/menus/${id}`, { method: 'PUT', body })
export const deleteMenu = (id) => request(`/menus/${id}`, { method: 'DELETE' })

export const getFunctions = (query) => request('/functions', { query })
export const createFunction = (body) => request('/functions', { method: 'POST', body })
export const updateFunction = (id, body) => request(`/functions/${id}`, { method: 'PUT', body })
export const deleteFunction = (id) => request(`/functions/${id}`, { method: 'DELETE' })

export const getLogs = (query) => request('/logs', { query })
