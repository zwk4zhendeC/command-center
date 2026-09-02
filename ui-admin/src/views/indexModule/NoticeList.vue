<template>
  <section class="home-card notice-list-card">
    <div class="home-card__header">
      <div class="home-card__title">
        <svg-icon icon-class="bell" class="home-card__title-icon" />
        <span>通知列表</span>
      </div>
      <el-badge v-if="unreadCount > 0" :value="unreadCount" type="danger" />
    </div>

    <div v-if="loading" class="notice-state">
      <el-icon class="is-loading"><Loading /></el-icon>
      <span>加载中...</span>
    </div>
    <div v-else-if="noticeList.length === 0" class="notice-state notice-empty">
      <el-icon><Postcard /></el-icon>
      <span>暂无通知公告</span>
    </div>
    <div v-else class="notice-list">
      <button v-for="item in noticeList" :key="item.noticeId" class="notice-item" :class="{ 'is-read': item.isRead }" type="button" @click="openNotice(item)">
        <span class="notice-dot" />
        <span class="notice-main">
          <span class="notice-title">{{ item.noticeTitle || '未命名公告' }}</span>
          <span class="notice-time">{{ item.createTime || '暂无时间' }}</span>
        </span>
        <el-tag size="small" :type="item.noticeType === '1' ? 'warning' : 'success'">
          {{ item.noticeType === '1' ? '通知' : '公告' }}
        </el-tag>
      </button>
    </div>

    <notice-detail-view ref="noticeViewRef" />
  </section>
</template>

<script setup lang="ts">
import NoticeDetailView from '@/layout/components/HeaderNotice/DetailView.vue'
import { listNoticeTop, markNoticeRead } from '@/api/system/notice'
import type { SysNotice } from '@/types/api/system/notice'

const noticeViewRef = ref<InstanceType<typeof NoticeDetailView> | null>(null)
const noticeList = ref<SysNotice[]>([])
const unreadCount = ref<number>(0)
const loading = ref<boolean>(false)

function loadNoticeTop() {
  loading.value = true
  listNoticeTop().then(res => {
    noticeList.value = res.data || []
    unreadCount.value = res.unreadCount !== undefined ? res.unreadCount : noticeList.value.filter((item: SysNotice) => !item.isRead).length
  }).finally(() => {
    loading.value = false
  })
}

function openNotice(item: SysNotice) {
  if (!item.isRead && item.noticeId != null) {
    markNoticeRead(item.noticeId).catch(() => {})
    const index = noticeList.value.findIndex((notice: SysNotice) => notice.noticeId === item.noticeId)
    if (index !== -1) {
      noticeList.value[index] = { ...item, isRead: true }
    }
    unreadCount.value = Math.max(0, unreadCount.value - 1)
  }
  noticeViewRef.value?.open(item.noticeId)
}

onMounted(() => {
  loadNoticeTop()
})
</script>

<style lang="scss" scoped>
.notice-list-card {
  min-height: 260px;
}

.notice-state {
  height: 174px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #a0a7b2;
  font-size: 13px;
}

.notice-empty {
  flex-direction: column;

  .el-icon {
    font-size: 28px;
  }
}

.notice-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.notice-item {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-height: 48px;
  padding: 10px 0;
  border: 0;
  border-bottom: 1px solid #f0f2f5;
  background: transparent;
  text-align: left;
  cursor: pointer;

  &:last-child {
    border-bottom: 0;
  }

  &:hover .notice-title {
    color: var(--el-color-primary);
  }

  &.is-read {
    .notice-dot {
      background: #cbd5e1;
    }

    .notice-title,
    .notice-time {
      color: #a0a7b2;
    }
  }
}

.notice-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
  flex-shrink: 0;
}

.notice-main {
  min-width: 0;
  flex: 1;
}

.notice-title,
.notice-time {
  display: block;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.notice-title {
  color: #273142;
  font-size: 14px;
  transition: color 0.16s ease;
}

.notice-time {
  margin-top: 5px;
  color: #8b95a5;
  font-size: 12px;
}
</style>
