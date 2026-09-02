<template>
  <section class="home-card square-module" :class="`square-module--${type}`">
    <div class="home-card__header">
      <div class="home-card__title">
        <svg-icon :icon-class="icon" class="home-card__title-icon" />
        <span>{{ title }}</span>
      </div>
      <span class="home-card__hint">{{ menus.length }} 个入口</span>
    </div>

    <el-scrollbar class="square-scrollbar">
      <div class="square-list">
        <button v-for="item in menus" :key="item.path || item.title" class="square-item" :class="`square-item--${type}`" type="button" @click="goTo(item)">
          <span class="square-icon" :style="{ background: item.bg || defaultBg, color: item.color || defaultColor }">
            <svg-icon :icon-class="item.icon || 'list'" />
          </span>
          <span class="square-content">
            <span class="square-title">{{ item.title }}</span>
          </span>
        </button>
      </div>
    </el-scrollbar>
  </section>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
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

type SquareModuleType = 'round' | 'square'

withDefaults(defineProps<{
  title: string
  menus: SquareMenuItem[]
  icon?: string
  type?: SquareModuleType
}>(), {
  icon: 'guide',
  type: 'square'
})

const router = useRouter()
const defaultColor = '#2563eb'
const defaultBg = '#e8f1ff'

function goTo(item: SquareMenuItem) {
  if (!item.path) return
  if (isHttp(item.path)) {
    window.open(item.path, '_blank')
    return
  }
  if (item.query) {
    router.push({ path: item.path, query: JSON.parse(item.query) })
    return
  }
  router.push(item.path)
}
</script>

<style lang="scss" scoped>
.square-module {
  min-height: 0;
  overflow: visible;
}

.square-module :deep(.home-card__header) {
  padding-bottom: 14px;
  border-bottom: 1px solid #edf0f5;
  margin-bottom: 16px;
}

.square-scrollbar {
  height: auto;
  padding-bottom: 2px;
}

.square-module--round .square-scrollbar {
  padding-bottom: 8px;
}

.square-list {
  display: flex;
  gap: 2px;
  min-width: max-content;
  padding: 2px 2px 10px;
}

.square-item {
  display: flex;
  padding: 16px;
  border: 1px solid #edf0f5;
  border-radius: 12px;
  background: linear-gradient(180deg, #ffffff 0%, #fbfcff 100%);
  text-align: left;
  cursor: pointer;
  transition: all 0.18s ease;

  &:hover {
    transform: translateY(-2px);
    border-color: #cfe0ff;
    box-shadow: 0 12px 24px rgba(31, 41, 55, 0.08);
  }
}

.square-item--round {
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 132px;
  min-height: 142px;
  background: #f8faff;
  text-align: center;
}

.square-item--square {
  flex-direction: row;
  align-items: center;
  gap: 14px;
  width: 238px;
  min-height: 72px;
  padding: 12px 16px;
}

.square-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 52px;
  height: 52px;
  border-radius: 12px;
  flex-shrink: 0;

  .svg-icon {
    width: 24px;
    height: 24px;
  }
}

.square-item--round .square-icon {
  width: 58px;
  height: 58px;
  border-radius: 50%;

  .svg-icon {
    width: 28px;
    height: 28px;
  }
}

.square-content {
  min-width: 0;
}

.square-item--round .square-content {
  width: 100%;
}

.square-title {
  display: block;
  width: 100%;
  color: #1f2937;
  font-size: 14px;
  font-weight: 600;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.square-item--round .square-title {
  margin-top: 16px;
  font-size: 15px;
}

.square-item--square .square-title {
  font-size: 16px;
}
</style>
