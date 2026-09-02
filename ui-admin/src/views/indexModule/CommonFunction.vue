<template>
  <SquareModule title="常用功能" icon="guide" :menus="commonMenus" type="round" />
</template>

<script setup lang="ts">
import SquareModule from './SquareModule.vue'
import useUserStore from '@/store/modules/user'

const userStore = useUserStore()
const commonMenus = computed(() => userStore.get_frequent_menu())
</script>
