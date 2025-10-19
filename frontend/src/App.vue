<template>
  <div class="app-container">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from './stores/user'

const userStore = useUserStore()

// 应用启动时尝试获取用户信息
onMounted(async () => {
  if (userStore.token) {
    await userStore.fetchUserInfo()
  }
})
</script>

<style scoped>
.app-container {
  width: 100%;
  min-height: 100vh;
}
</style>