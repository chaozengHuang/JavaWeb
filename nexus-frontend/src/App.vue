<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getUnreadCount } from '@/api/message'
import { getCheckInStatus, checkIn } from '@/api/points'
import { getPendingCount } from '@/api/friend'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()

const user = ref(null)
const unreadCount = ref(0)
const checkedIn = ref(false)
const friendReqCount = ref(0)
const checkInLoading = ref(false)
let ws = null
let wsReconnectTimer = null
let unreadTimer = null

const isAdmin = computed(() => {
  return user.value?.globalRole === 'SYS_ADMIN'
})

const navItems = computed(() => {
  const items = [
    { key: 'forum', label: '论坛大厅' },
    { key: 'chat', label: '消息' },
    { key: 'friends', label: '好友' },
    { key: 'points', label: '积分中心' },
  ]
  if (isAdmin.value) {
    items.push({ key: 'admin', label: '管理后台' })
  }
  return items
})

const activeMenu = computed(() => {
  if (route.path.startsWith('/forum')) return 'forum'
  if (route.path.startsWith('/chat')) return 'chat'
  if (route.path.startsWith('/friends')) return 'friends'
  if (route.path.startsWith('/points')) return 'points'
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

const fetchCheckInStatus = async () => {
  try {
    const res = await getCheckInStatus()
    checkedIn.value = res.data?.checkedIn || false
  } catch { checkedIn.value = false }
}

const handleCheckIn = async () => {
  checkInLoading.value = true
  try {
    const res = await checkIn()
    const data = res.data
    ElMessage.success(`签到成功！+${data.pointsAwarded} 积分`)
    checkedIn.value = true
    if (user.value) {
      user.value.points = data.totalPoints
    }
    const stored = JSON.parse(localStorage.getItem('user') || '{}')
    if (stored.user) {
      stored.user.points = data.totalPoints
      localStorage.setItem('user', JSON.stringify(stored))
    }
  } catch (err) {
    ElMessage.error(err.message || '签到失败')
  } finally {
    checkInLoading.value = false
  }
}

const fetchFriendReqCount = async () => {
  try {
    const res = await getPendingCount()
    friendReqCount.value = res.data?.count || 0
  } catch { friendReqCount.value = 0 }
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

  ws = new WebSocket(`ws://localhost:8081/ws/chat?token=${token}`)

  ws.onopen = () => {
    console.log('WebSocket 已连接')
    window.chatWs = ws
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
    window.chatWs = null
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
  window.chatWs = null
}

const handleLogout = () => {
  disconnectWebSocket()
  if (unreadTimer) clearInterval(unreadTimer)
  localStorage.removeItem('user')
  user.value = null
  router.push('/auth')
}

const handleUserMenu = (command) => {
  if (command === 'logout') handleLogout()
}

const onLoginSuccess = () => {
  loadUser()
  connectWebSocket()
  fetchCheckInStatus()
  fetchUnreadCount()
  fetchFriendReqCount()
}

onMounted(() => {
  loadUser()
  connectWebSocket()
  fetchUnreadCount()
  fetchCheckInStatus()
  fetchFriendReqCount()
  unreadTimer = setInterval(() => {
    fetchUnreadCount()
    fetchCheckInStatus()
    fetchFriendReqCount()
  }, 10000)
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
    <!-- 顶部导航栏 -->
    <div class="app-navbar">
      <div class="navbar-left">
        <div class="navbar-brand">Nexus</div>
        <div
          v-for="item in navItems"
          :key="item.key"
          class="navbar-item"
          :class="{ active: activeMenu === item.key }"
          @click="router.push('/' + item.key)"
        >
          {{ item.label }}
          <span v-if="item.key === 'chat' && unreadCount > 0" class="unread-badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
          <span v-if="item.key === 'friends' && friendReqCount > 0" class="unread-badge">{{ friendReqCount > 99 ? '99+' : friendReqCount }}</span>
        </div>
      </div>
      <div class="navbar-right">
        <el-button v-if="!checkedIn" type="success" size="small" :loading="checkInLoading" @click="handleCheckIn">签到</el-button>
        <span v-else class="checked-in-text">已签到</span>
        <span class="points-display">积分: {{ user.points ?? 0 }}</span>
        <el-popover placement="bottom-end" :width="200" trigger="click">
          <template #reference>
            <el-avatar
              :size="34"
              class="user-avatar-btn"
              :src="(user.avatar && user.avatar.startsWith('/')) ? 'http://localhost:8081' + user.avatar : ''"
            >{{ user.username?.charAt(0) }}</el-avatar>
          </template>
          <div style="text-align:center;padding:8px 0 12px;">
            <div style="font-weight:600;font-size:15px;color:#303133;">{{ user.username }}</div>
            <div style="font-size:12px;color:#909399;margin-top:4px;">
              {{ user.globalRole === 'SYS_ADMIN' ? '系统管理员' : user.globalRole === 'NOTIFY_ADMIN' ? '通知管理员' : '普通用户' }}
            </div>
            <div style="font-size:12px;color:#e6a23c;margin-top:4px;">积分: {{ user.points ?? 0 }}</div>
          </div>
          <el-divider style="margin:0;" />
          <el-button text style="width:100%;" @click="router.push('/user/' + user.id)">个人主页</el-button>
          <el-divider style="margin:8px 0;" />
          <el-button type="danger" text style="width:100%;" @click="handleLogout">退出登录</el-button>
        </el-popover>
      </div>
    </div>

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
  justify-content: space-between;
  height: 56px;
  padding: 0 24px;
  border-bottom: 1px solid #e4e7ed;
  background: #fff;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 4px;
}

.navbar-brand {
  font-size: 20px;
  font-weight: 700;
  color: #409eff;
  margin-right: 24px;
}

.navbar-item {
  padding: 8px 14px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  border-radius: 4px;
  position: relative;
  transition: all 0.2s;
  user-select: none;
}

.navbar-item:hover {
  background: #f5f7fa;
  color: #409eff;
}

.navbar-item.active {
  color: #409eff;
  font-weight: 500;
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

.checked-in-text {
  font-size: 12px;
  color: #67c23a;
  margin-right: 8px;
}

.points-display {
  font-size: 13px;
  color: #e6a23c;
  margin-right: 12px;
  font-weight: 500;
}

.user-avatar-btn {
  cursor: pointer;
  border: 2px solid #e4e7ed;
  transition: border-color 0.2s;
}

.user-avatar-btn:hover {
  border-color: #409eff;
}
</style>
