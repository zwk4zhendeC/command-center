<template>
  <div class="system-function-list">
    <SquareModule
      v-for="module in systemModules"
      :key="module.title"
      :title="module.title"
      :icon="module.icon"
      :menus="module.menus"
      class="system-square-module"
      type="square"
    />
  </div>
</template>

<script setup lang="ts">
import SquareModule from './SquareModule.vue'
import usePermissionStore from '@/store/modules/permission'
import { getNormalPath } from '@/utils/common'
import { isHttp } from '@/utils/validate'

interface SquareMenuItem {
  menuId?: number
  title: string
  path: string
  icon?: string
  color?: string
  bg?: string
  query?: string
}

interface RouteModule {
  title: string
  icon: string
  menus: SquareMenuItem[]
}

const permissionStore = usePermissionStore()

const systemModules = computed<RouteModule[]>(() => {
  return permissionStore.defaultRoutes
    .filter((route: any) => !route.hidden && route.meta && route.meta.title)
    .map((route: any) => {
      const basePath = normalizeRoutePath('', route.path)
      const menus = collectLeafMenus(route.children || [], basePath)
      return {
        title: route.meta.title,
        icon: route.meta.icon || 'system',
        menus
      }
    })
    .filter((module: RouteModule) => module.menus.length > 0)
})

function collectLeafMenus(routes: any[], basePath: string): SquareMenuItem[] {
  const menus: SquareMenuItem[] = []
  routes.forEach(route => {
    if (route.hidden) return
    const path = normalizeRoutePath(basePath, route.path)
    if (route.children && route.children.length > 0) {
      menus.push(...collectLeafMenus(route.children, path))
      return
    }
    if (!route.meta || !route.meta.title) return
    menus.push({
      menuId: route.meta.menuId,
      title: route.meta.title,
      path,
      icon: route.meta.icon || 'list',
      query: route.query
    })
  })
  return menus
}

function normalizeRoutePath(basePath: string, routePath: string): string {
  if (isHttp(routePath)) return routePath
  const path = routePath && routePath[0] === '/' ? routePath : '/' + routePath
  return getNormalPath(basePath + path)
}
</script>

<style lang="scss" scoped>
.system-function-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.system-square-module {
  padding-bottom: 12px;
}
</style>
