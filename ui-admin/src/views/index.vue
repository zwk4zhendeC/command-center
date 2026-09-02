<template>
  <main class="home-page">
    <el-row :gutter="20" class="home-row">
      <el-col :sm="24" :md="24" :lg="7">
        <PersonalCenter />
      </el-col>
      <el-col :sm="24" :md="24" :lg="17">
        <CommonFunction />
      </el-col>
    </el-row>

    <el-row :gutter="20" class="home-row">
      <el-col :sm="24" :md="24" :lg="7">
        <NoticeList />
      </el-col>
      <el-col :sm="24" :md="24" :lg="17">
        <SystemFunction />
      </el-col>
    </el-row>
  </main>
</template>

<script setup lang="ts" name="Index">
import PersonalCenter from './indexModule/PersonalCenter.vue'
import CommonFunction from './indexModule/CommonFunction.vue'
import NoticeList from './indexModule/NoticeList.vue'
import SystemFunction from './indexModule/SystemFunction.vue'
</script>


<style scoped lang="scss">
.home-page {
  min-height: calc(100vh - 84px);
  padding: 20px;
  background: #f5f7fb;
}

.home-row {
  margin-bottom: 20px;

  &:last-child {
    margin-bottom: 0;
  }
}

:deep(.home-card) {
  height: 100%;
  padding: 20px 22px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 1px 3px rgba(18, 31, 56, 0.06);
}

:deep(.home-card__header) {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

:deep(.home-card__title) {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #273142;
  font-size: 16px;
  font-weight: 600;
}

:deep(.home-card__title-icon) {
  width: 20px;
  height: 20px;
  color: var(--el-color-primary);
}

:deep(.home-card__hint) {
  color: #a0a7b2;
  font-size: 12px;
}

@media (max-width: 1199px) {
  .home-row {
    margin-bottom: 0;
  }

  .home-row :deep(.el-col) {
    margin-bottom: 20px;
  }
}

@media (max-width: 640px) {
  .home-page {
    padding: 12px;
  }
}
</style>
