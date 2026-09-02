<template>
  <!-- 个人中心 -->
  <div class="personal-section">
    <div class="top-box">
      <div class="left">
        <svg-icon icon-class="user" class="title-icon" />
        <span class="title">个人中心</span>
      </div>
      <div class="more" @click="goToProfile">
        详情<el-icon class="more-icon"><ArrowRight /></el-icon>
      </div>
    </div>

    <div v-loading="loading" class="content-box">
      <img class="avatar" :src="avatarUrl" alt="用户头像" @error="handleAvatarError" />
      <div class="content">
        <p class="welcome">你好，{{ state.user.nickName || state.user.userName }}</p>
        <p class="position">角色：{{ state.roleGroup || '暂无' }}</p>
        <p v-if="state.postGroup" class="dept">岗位：{{ state.postGroup }}</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ArrowRight } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'
import { getUserProfile } from '@/api/system/user'
import { isEmpty, isHttp } from '@/utils/validate'
import defAva from '@/assets/images/profile.png'
import type { SysUser } from '@/types/api/system/user'

interface UserProfileState {
  user: SysUser
  roleGroup: string
  postGroup: string
}

const router = useRouter()

const state = reactive<UserProfileState>({
  user: {} as SysUser,
  roleGroup: '',
  postGroup: ''
})
const loading = ref<boolean>(false)

const avatarUrl = computed(() => {
  const avatar = state.user.avatar || ''
  if (isEmpty(avatar)) return defAva
  return isHttp(avatar) ? avatar : import.meta.env.VITE_APP_BASE_API + avatar
})

function getUser() {
  loading.value = true
  getUserProfile().then(response => {
    state.user = response.data || ({} as SysUser)
    state.roleGroup = response.roleGroup || ''
    state.postGroup = response.postGroup || ''
  }).finally(() => {
    loading.value = false
  })
}

function handleAvatarError(event: Event) {
  (event.target as HTMLImageElement).src = defAva
}

function goToProfile() {
  router.push({ path: '/user/profile' })
}

onMounted(() => {
  getUser()
})
</script>

<style lang="scss" scoped>
.personal-section {
  padding-bottom: 20px;
  background-color: #fff;
  border-radius: 6px;
  .top-box {
    display: flex;
    justify-content: space-between;
    padding: 20px 23px;
    align-items: center;
    .left {
      display: flex;
      align-items: center;
      gap: 10px;
      .title-icon {
        width: 20px;
        height: 20px;
        color: #3d7fff;
      }
      .title {
        color: #333333;
        font-weight: 600;
      }
    }
    .more {
      display: flex;
      align-items: center;
      font-size: 12px;
      color: #a5a8ab;
      letter-spacing: 1px;
      cursor: pointer;
      transition: all 100ms;
      &:hover {
        color: #3d7fff;
      }
      .more-icon {
        margin-left: -2px;
      }
    }
  }
  .content-box {
    margin: 0px 23px;
    padding: 15.5px 20px;
    background: linear-gradient(0deg, #e7f2ff, #c8deff);
    display: flex;
    gap: 19px;
    border-radius: 6px;
    height: 151px;
    transition: all 200ms;
    &:hover {
      transform: translateY(-0.5px);
      box-shadow: 0 6px 12px rgba(#000, 0.04);
      background: linear-gradient(0deg, #e0ecfb, #c4dcfe);
    }
    .avatar {
      width: 120px;
      height: 120px;
      border: 3px solid #ffffff;
      border-radius: 50%;
      object-fit: cover;
      transition: all 300ms;
    }
    .content {
      transition: all 300ms;

      .welcome {
        font-size: 20px;
        color: #333333;
        margin: 0;
        margin-top: 7px;
      }
      .position {
        margin: 0;
        margin-top: 18px;
        font-size: 16px;
        color: #333333;
      }
      .dept {
        margin: 0;
        margin-top: 10px;
        font-size: 16px;
        color: #333333;
      }
    }
  }
}
</style>
