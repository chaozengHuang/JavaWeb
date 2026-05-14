<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const user = ref(null)

const activeMenu = computed(() => {
  if (route.path.startsWith('/forum')) return 'forum'
  if (route.path.startsWith('/profile')) return 'profile'
  return ''
})

const loadUser = () => {
  const stored = localStorage.getItem('user')
  if (stored) {
    user.value = JSON.parse(stored)
  }
}

const handleMenuSelect = (key) => {
  router.push('/' + key)
}

const handleLogout = () => {
  localStorage.removeItem('user')
  user.value = null
  router.push('/auth')
}

onMounted(() => {
  loadUser()
})

defineExpose({ user, loadUser })
</script>

<template>
  <div v-if="!user" class="app-layout">
    <router-view @login-success="loadUser" />
  </div>

  <div v-else class="app-layout">
    <el-menu
      :default-active="activeMenu"
      mode="horizontal"
      class="app-navbar"
      @select="handleMenuSelect"
    >
      <div class="navbar-brand">Nexus</div>
      <el-menu-item index="forum">论坛大厅</el-menu-item>
      <el-menu-item index="profile">个人中心</el-menu-item>
      <div class="navbar-right">
        <span class="navbar-user">用户: {{ user.username }} | 积分: {{ user.points ?? 0 }}</span>
        <el-button type="danger" size="small" text @click="handleLogout">退出</el-button>
      </div>
    </el-menu>

    <div class="app-content">
      <router-view :key="$route.fullPath" />
    </div>
  </div>
</template>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', Arial, sans-serif;
  background: #f5f7fa;
}

.app-navbar {
  display: flex;
  align-items: center;
  padding: 0 24px;
  border-bottom: 1px solid #e4e7ed;
}

.navbar-brand {
  font-size: 20px;
  font-weight: 700;
  color: #409eff;
  margin-right: 32px;
}

.navbar-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

.navbar-user {
  font-size: 14px;
  color: #606266;
}

.app-content {
  min-height: calc(100vh - 60px);
  padding: 20px;
}
</style>
