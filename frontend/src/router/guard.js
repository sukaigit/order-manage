import { getToken, getSession, setSession, getMe, clearAuth } from '../api/auth.js'

export function setupGuard(router) {
  router.beforeEach(async (to) => {
    const isPublic = to.path === '/login'
    const token = getToken()

    if (isPublic) {
      return token && getSession() ? { path: '/workbench' } : true
    }

    if (!token) return '/login'

    if (!getSession()) {
      try {
        const me = await getMe()
        setSession({ user: me.user, menus: me.menus, permissions: me.permissions })
      } catch {
        clearAuth()
        return '/login'
      }
    }
    return true
  })
}
