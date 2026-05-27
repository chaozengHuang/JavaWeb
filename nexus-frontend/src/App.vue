<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getUnreadCount } from '@/api/message'

const router = useRouter()
const route = useRoute()

const user = ref(null)
const unreadCount = ref(0)
let ws = null
let wsReconnectTimer = null
let unreadTimer = null

const isAdmin = computed(() => {
  return user.value?.globalRole === 'SYS_ADMIN'
})

const activeMenu = computed(() => {
  if (route.path.startsWith('/forum')) return 'forum'
  if (route.path.startsWith('/chat')) return 'chat'
  if (route.path.startsWith('/profile')) return 'profile'
  if (route.path.startsWith('/admin')) return 'admin'
  return ''
})

const loadUser = () => {
  const stored = localStorage.getItem('user')
  if (stored) {
    try {
      const data = JSON.parse(stored)
      user.value = data.user || null
    } catch {
      user.value = null
    }
  }
}

const fetchUnreadCount = async () => {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data?.count ?? 0
  } catch { /* ignore */ }
}

const connectWebSocket = () => {
  const stored = localStorage.getItem('user')
  if (!stored) return

  let token = null
  try {
    const data = JSON.parse(stored)
    token = data.token
  } catch { return }

  if (!token) return

  if (ws && ws.readyState === WebSocket.OPEN) return

  ws = new WebSocket(`ws://localhost:8080/ws/chat?token=${token}`)

  ws.onopen = () => {
    console.log('WebSocket 已连接')
    if (wsReconnectTimer) {
      clearTimeout(wsReconnectTimer)
      wsReconnectTimer = null
    }
  }

  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (globalThis.__onChatMessage) {
        globalThis.__onChatMessage(data)
      }
      fetchUnreadCount()
    } catch { /* ignore */ }
  }

  ws.onclose = () => {
    console.log('WebSocket 已断开，5秒后重连...')
    wsReconnectTimer = setTimeout(connectWebSocket, 5000)
  }

  ws.onerror = () => {
    ws?.close()
  }
}

const disconnectWebSocket = () => {
  if (wsReconnectTimer) {
    clearTimeout(wsReconnectTimer)
    wsReconnectTimer = null
  }
  if (ws) {
    ws.close()
    ws = null
  }
}

const handleMenuSelect = (key) => {
  router.push('/' + key)
}

const handleLogout = () => {
  disconnectWebSocket()
  if (unreadTimer) clearInterval(unreadTimer)
  localStorage.removeItem('user')
  user.value = null
  router.push('/auth')
}

const onLoginSuccess = () => {
  loadUser()
  connectWebSocket()
}

onMounted(() => {
  loadUser()
  connectWebSocket()
  fetchUnreadCount()
  unreadTimer = setInterval(fetchUnreadCount, 10000)
})

onUnmounted(() => {
  disconnectWebSocket()
  if (unreadTimer) clearInterval(unreadTimer)
})

defineExpose({ user, loadUser })
</script>

<template>
  <div v-if="!user" class="app-layout">
    <router-view @login-success="onLoginSuccess" />
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
      <el-menu-item index="chat">
        私聊
        <span v-if="unreadCount > 0" class="unread-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
      </el-menu-item>
      <el-menu-item v-if="isAdmin" index="admin">管理后台</el-menu-item>
      <el-menu-item index="profile">个人中心</el-menu-item>
      <div class="navbar-right">
        <span class="navbar-user">用户: {{ user.username }} | 积分: {{ user.points ?? 0 }}</span>
        <el-button type="danger" size="small" text @click="handleLogout">退出</el-button>
      </div>
    </el-menu>

    <div class="app-content" :class="{ 'admin-content': route.path.startsWith('/admin') }">
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

.admin-content {
  padding: 0;
}

.unread-badge {
  display: inline-block;
  background: #f56c6c;
  color: #fff;
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 10px;
  margin-left: 4px;
  vertical-align: middle;
}
</style>
