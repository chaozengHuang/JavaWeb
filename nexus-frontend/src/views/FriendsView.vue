<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getFriendList, getPendingRequests, acceptFriendRequest,
  rejectFriendRequest, deleteFriend, getPendingCount,
} from '@/api/friend'

const router = useRouter()
const friends = ref([])
const pendingRequests = ref([])
const pendingCount = ref(0)
const activeTab = ref('friends')

const loadFriends = async () => {
  try {
    const res = await getFriendList()
    friends.value = res.data || []
  } catch { friends.value = [] }
}

const loadRequests = async () => {
  try {
    const res = await getPendingRequests()
    pendingRequests.value = res.data || []
    const countRes = await getPendingCount()
    pendingCount.value = countRes.data?.count || 0
  } catch { pendingRequests.value = [] }
}

const handleAccept = async (requestId) => {
  try {
    await acceptFriendRequest(requestId)
    ElMessage.success('已接受好友请求')
    loadRequests()
    loadFriends()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

const handleReject = async (requestId) => {
  try {
    await rejectFriendRequest(requestId)
    ElMessage.success('已拒绝')
    loadRequests()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

const handleDelete = async (friendId) => {
  try {
    const { ElMessageBox } = await import('element-plus')
    await ElMessageBox.confirm('确定删除该好友吗？', '提示', { type: 'warning' })
    await deleteFriend(friendId)
    ElMessage.success('已删除好友')
    loadFriends()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '删除失败')
    }
  }
}

const startChat = (userId) => {
  router.push(`/chat/${userId}`)
}

const goToProfile = (userId) => {
  router.push(`/user/${userId}`)
}

onMounted(() => {
  loadFriends()
  loadRequests()
})
</script>

<template>
  <div class="friends-page">
    <h2>好友</h2>

    <el-tabs v-model="activeTab" @tab-change="(t) => { if (t === 'friends') loadFriends(); else loadRequests(); }">
      <el-tab-pane label="我的好友" name="friends">
        <div v-if="friends.length === 0" class="empty-hint">
          <el-empty description="暂无好友" />
        </div>
        <div v-else class="friend-grid">
          <el-card
            v-for="f in friends"
            :key="f.userId"
            shadow="hover"
            class="friend-card"
          >
            <div class="friend-info" @click="goToProfile(f.userId)">
              <el-avatar :size="48" :src="f.avatar ? 'http://localhost:8081' + f.avatar : ''">
                {{ f.username?.charAt(0) }}
              </el-avatar>
              <div class="friend-meta">
                <div class="friend-name">
                  {{ f.username }}
                  <span v-if="f.online" class="online-dot" title="在线" />
                  <span v-else class="offline-dot" title="离线" />
                </div>
                <div class="friend-status-text">{{ f.online ? '在线' : '离线' }}</div>
              </div>
            </div>
            <div class="friend-actions">
              <el-button size="small" @click="startChat(f.userId)">发消息</el-button>
              <el-button size="small" type="danger" text @click="handleDelete(f.userId)" v-if="f.globalRole !== 'NOTIFY_ADMIN' && !f.isSelf">删除</el-button>
            </div>
          </el-card>
        </div>
      </el-tab-pane>

      <el-tab-pane :label="`好友请求 (${pendingCount})`" name="requests">
        <div v-if="pendingRequests.length === 0" class="empty-hint">
          <el-empty description="暂无待处理的好友请求" />
        </div>
        <div v-else class="request-list">
          <el-card
            v-for="req in pendingRequests"
            :key="req.id"
            shadow="hover"
            class="request-card"
          >
            <div class="request-info" @click="goToProfile(req.userId)">
              <el-avatar :size="40" :src="req.avatar ? 'http://localhost:8081' + req.avatar : ''">
                {{ req.username?.charAt(0) }}
              </el-avatar>
              <span class="request-username">{{ req.username }}</span>
            </div>
            <div class="request-actions">
              <el-button type="success" size="small" @click="handleAccept(req.id)">接受</el-button>
              <el-button type="danger" size="small" @click="handleReject(req.id)">拒绝</el-button>
            </div>
          </el-card>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.friends-page {
  max-width: 800px;
  margin: 0 auto;
}

.friends-page h2 {
  margin-bottom: 20px;
  color: #303133;
}

.empty-hint {
  padding: 40px 0;
}

.friend-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 12px;
}

.friend-card {
  border-radius: 8px;
}

.friend-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  flex: 1;
}

.friend-meta {
  flex: 1;
}

.friend-name {
  font-size: 15px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}

.online-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #67c23a;
  display: inline-block;
}

.offline-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #c0c4cc;
  display: inline-block;
}

.friend-status-text {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}

.friend-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  justify-content: flex-end;
}

.request-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.request-card {
  border-radius: 8px;
}

.request-info {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.request-username {
  font-size: 15px;
  font-weight: 500;
}

.request-actions {
  display: flex;
  gap: 8px;
  margin-top: 12px;
  justify-content: flex-end;
}

:deep(.friend-info .el-avatar),
:deep(.request-info .el-avatar) {
  background: #409eff;
  color: #fff;
  font-weight: 600;
  font-size: 18px;
}
</style>
