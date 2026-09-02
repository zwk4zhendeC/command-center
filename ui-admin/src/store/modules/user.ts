import router from '@/router'
import cache from '@/plugins/cache'
import { ElMessageBox } from 'element-plus'
import { login, logout, getInfo } from '@/api/login'
import { getFrequentMenus, updateFrequentMenus } from '@/api/system/user'
import { getToken, setToken, removeToken } from '@/utils/auth'
import { isHttp, isEmpty } from "@/utils/validate"
import useLockStore from '@/store/modules/lock'
import defAva from '@/assets/images/profile.png'
import { LRUCache } from '@/utils/lru'

export interface FrequentMenu {
  menuId: number
  title: string
  path: string
  icon?: string
  color?: string
  bg?: string
  query?: string
}

interface UserState {
  token: string | undefined
  id: string | number
  name: string
  nickName: string
  avatar: string
  roles: string[]
  permissions: string[]
  frequentMenuCache: LRUCache<number, FrequentMenu>
  frequentMenus: FrequentMenu[]
  frequentMenuIds: number[]
  frequentMenuDirty: boolean
  frequentMenuSyncTimer: ReturnType<typeof setInterval> | null
}

const FREQUENT_MENU_SYNC_INTERVAL = 10000

const useUserStore = defineStore(
  'user',
  {
    state: (): UserState => ({
      token: getToken(),
      id: '',
      name: '',
      nickName: '',
      avatar: '',
      roles: [],
      permissions: [],
      frequentMenuCache: new LRUCache<number, FrequentMenu>(10),
      frequentMenus: [],
      frequentMenuIds: [],
      frequentMenuDirty: false,
      frequentMenuSyncTimer: null
    }),
    actions: {
      get_frequent_menu(): FrequentMenu[] {
        return this.frequentMenus
      },
      getFrequentMenuCacheKey(): string {
        return `frequent-menu-ids:${this.id || 'anonymous'}`
      },
      reset_frequent_menu_cache() {
        this.frequentMenuCache = new LRUCache<number, FrequentMenu>(10)
        this.frequentMenus = []
      },
      rebuild_frequent_menu(menus: FrequentMenu[], markDirty = false) {
        this.reset_frequent_menu_cache()
        menus.slice().reverse().forEach(menu => {
          if (menu && menu.menuId) {
            this.frequentMenuCache.put(menu.menuId, menu)
          }
        })
        this.frequentMenus = this.frequentMenuCache.values()
        this.frequentMenuIds = this.frequentMenus.map((menu: FrequentMenu) => menu.menuId)
        cache.local.setJSON(this.getFrequentMenuCacheKey(), this.frequentMenuIds)
        this.frequentMenuDirty = markDirty
      },
      restore_frequent_menu_from_local(menuMap: Map<number, FrequentMenu>) {
        const cachedIds = cache.local.getJSON(this.getFrequentMenuCacheKey())
        const ids = Array.isArray(cachedIds) ? cachedIds.map((id: any) => Number(id)).filter((id: number) => !Number.isNaN(id)) : []
        this.rebuild_frequent_menu(ids.map((id: number) => menuMap.get(id)).filter(Boolean) as FrequentMenu[])
      },
      async load_frequent_menu(menuMap: Map<number, FrequentMenu>) {
        try {
          const res = await getFrequentMenus()
          const ids = Array.isArray(res.data) ? res.data.map((id: any) => Number(id)).filter((id: number) => !Number.isNaN(id)) : []
          this.rebuild_frequent_menu(ids.map((id: number) => menuMap.get(id)).filter(Boolean) as FrequentMenu[])
        } catch (error) {
          this.restore_frequent_menu_from_local(menuMap)
        }
      },
      put_frequent_menu(menu: FrequentMenu) {
        if (!menu || !menu.menuId || !menu.path || !menu.title) return
        this.frequentMenuCache.put(menu.menuId, menu)
        this.frequentMenus = this.frequentMenuCache.values()
        this.frequentMenuIds = this.frequentMenus.map((menu: FrequentMenu) => menu.menuId)
        cache.local.setJSON(this.getFrequentMenuCacheKey(), this.frequentMenuIds)
        this.frequentMenuDirty = true
      },
      async sync_frequent_menu() {
        if (!this.frequentMenuDirty) return
        await updateFrequentMenus(this.frequentMenuIds)
        this.frequentMenuDirty = false
      },
      start_frequent_menu_sync() {
        if (this.frequentMenuSyncTimer) return
        this.frequentMenuSyncTimer = setInterval(() => {
          this.sync_frequent_menu().catch(() => {})
        }, FREQUENT_MENU_SYNC_INTERVAL)
      },
      stop_frequent_menu_sync() {
        if (!this.frequentMenuSyncTimer) return
        clearInterval(this.frequentMenuSyncTimer)
        this.frequentMenuSyncTimer = null
      },
      // 登录
      login(userInfo: { username: string; password: string; code: string; uuid: string }) {
        const username = userInfo.username.trim()
        const password = userInfo.password
        const code = userInfo.code
        const uuid = userInfo.uuid
        return new Promise<void>((resolve, reject) => {
          login(username, password, code, uuid).then(res => {
            setToken(res.token)
            this.token = res.token
            useLockStore().unlockScreen()
            resolve()
          }).catch(error => {
            reject(error)
          })
        })
      },
      // 获取用户信息
      getInfo() {
        return new Promise((resolve, reject) => {
          getInfo().then(res => {
            const user = res.user
            let avatar = user.avatar || ''
            if (!isHttp(avatar)) {
              avatar = (isEmpty(avatar)) ? defAva : import.meta.env.VITE_APP_BASE_API + avatar
            }
            if (res.roles && res.roles.length > 0) { // 验证返回的roles是否是一个非空数组
              this.roles = res.roles
              this.permissions = res.permissions
            } else {
              this.roles = ['ROLE_DEFAULT']
            }
            this.id = user.userId || ''
            this.name = user.userName || ''
            this.nickName = user.nickName || ''
            this.avatar = avatar
            cache.session.set('pwrChrtype', res.pwdChrtype)
            /* 初始密码提示 */
            if(res.isDefaultModifyPwd) {
              ElMessageBox.confirm('您的密码还是初始密码，请修改密码！',  '安全提示', {  confirmButtonText: '确定',  cancelButtonText: '取消',  type: 'warning' }).then(() => {
                router.push({ name: 'Profile', params: { activeTab: 'resetPwd' } })
              }).catch(() => {})
            }
            /* 过期密码提示 */
            if(!res.isDefaultModifyPwd && res.isPasswordExpired) {
              ElMessageBox.confirm('您的密码已过期，请尽快修改密码！',  '安全提示', {  confirmButtonText: '确定',  cancelButtonText: '取消',  type: 'warning' }).then(() => {
                router.push({ name: 'Profile', params: { activeTab: 'resetPwd' } })
              }).catch(() => {})
            }
            resolve(res)
          }).catch(error => {
            reject(error)
          })
        })
      },
      // 退出系统
      logOut() {
        return new Promise<void>((resolve, reject) => {
          this.stop_frequent_menu_sync()
          logout().then(() => {
            this.token = ''
            this.roles = []
            this.permissions = []
            this.frequentMenuIds = []
            this.frequentMenus = []
            this.frequentMenuDirty = false
            this.frequentMenuCache = new LRUCache<number, FrequentMenu>(10)
            removeToken()
            resolve()
          }).catch((error: any) => {
            reject(error)
          })
        })
      }
    }
  })

export default useUserStore
