import router from './router'
import { ElMessage } from 'element-plus'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getToken } from '@/utils/auth'
import { isHttp, isPathMatch } from '@/utils/validate'
import { isRelogin } from '@/utils/request'
import useUserStore from '@/store/modules/user'
import useLockStore from '@/store/modules/lock'
import useSettingsStore from '@/store/modules/settings'
import usePermissionStore from '@/store/modules/permission'

NProgress.configure({ showSpinner: false })

const whiteList = ['/login', '/register']

const isWhiteList = (path: string): boolean => {
  return whiteList.some((pattern: string) => isPathMatch(pattern, path))
}

router.beforeEach(async (to, from) => {
  NProgress.start()
  if (getToken()) {
    to.meta.title && useSettingsStore().setTitle(to.meta.title as string)
    const isLock = useLockStore().isLock
    if (to.path === '/login') {
      NProgress.done()
      return { path: '/' }
    }
    if (isWhiteList(to.path)) {
      return true
    }
    if (isLock && to.path !== '/lock') {
      NProgress.done()
      return { path: '/lock' }
    }
    if (!isLock && to.path === '/lock') {
      NProgress.done()
      return { path: '/' }
    }
    if (useUserStore().roles.length === 0) {
      isRelogin.show = true
      try {
        // 拉取user_info信息
        await useUserStore().getInfo()
        isRelogin.show = false
        // 根据roles权限生成可访问的路由
        const accessRoutes = await usePermissionStore().generateRoutes()
        await useUserStore().load_frequent_menu(buildFrequentMenuMap())
        useUserStore().start_frequent_menu_sync()
        accessRoutes.forEach((route: any) => {
          if (!isHttp(route.path)) {
            router.addRoute(route)
          }
        })
        // 重新导航到目标路由，确保动态路由已注册
        return { ...to, replace: true }
      } catch (err) {
        await useUserStore().logOut()
        ElMessage.error(err)
        return { path: '/' }
      }
    }
    return true
  } else {
    // 没有token
    if (isWhiteList(to.path)) {
      // 在免登录白名单，直接进入
      return true
    }
    NProgress.done()
    return `/login?redirect=${to.fullPath}` // 否则全部重定向到登录页
  }
})

router.afterEach((to) => {
  recordFrequentMenu(to)
  NProgress.done()
})

function recordFrequentMenu(to: any) {
  const menuId = to.meta && to.meta.menuId
  if (!menuId) return

  const leafMenu = findSystemLeafMenuById(menuId)
  if (!leafMenu || !leafMenu.meta || !leafMenu.meta.title) return

  useUserStore().put_frequent_menu({
    menuId: Number(leafMenu.meta.menuId),
    title: leafMenu.meta.title,
    path: to.path,
    icon: leafMenu.meta.icon || 'list',
    query: Object.keys(to.query || {}).length > 0 ? JSON.stringify(to.query) : leafMenu.query
  })
}

function buildFrequentMenuMap(): Map<number, any> {
  const menuMap = new Map<number, any>()
  collectSystemLeafMenus(usePermissionStore().defaultRoutes).forEach((route: any) => {
    const menuId = Number(route.meta && route.meta.menuId)
    if (!menuId || Number.isNaN(menuId)) return
    menuMap.set(menuId, {
      menuId,
      title: route.meta.title,
      path: route.path,
      icon: route.meta.icon || 'list',
      query: route.query
    })
  })
  return menuMap
}

function findSystemLeafMenuById(menuId: number | string) {
  return collectSystemLeafMenus(usePermissionStore().defaultRoutes)
    .find((route: any) => String(route.meta && route.meta.menuId) === String(menuId))
}

function collectSystemLeafMenus(routes: any[]): any[] {
  const leaves: any[] = []
  routes.forEach(route => {
    if (route.hidden) return
    if (route.children && route.children.length > 0) {
      leaves.push(...collectSystemLeafMenus(route.children))
      return
    }
    if (route.meta && route.meta.title && route.meta.menuId) {
      leaves.push(route)
    }
  })
  return leaves
}
