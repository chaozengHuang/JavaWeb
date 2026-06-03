<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getHistory, markAsRead, getChatList } from '@/api/message'
import ChatList from '@/components/ChatList.vue'
import ChatMessage from '@/components/ChatMessage.vue'

const route = useRoute()
const router = useRouter()

const chats = ref([])
const messages = ref([])
const activeUserId = ref(null)
const inputText = ref('')
const messagesContainer = ref(null)
const loading = ref(false)

const currentUser = computed(() => {
  const stored = localStorage.getItem('user')
  if (stored) {
    try {
      const data = JSON.parse(stored)
      return data.user || {}
    } catch { return {} }
  }
  return {}
})

const loadChatList = async () => {
  try {
    const res = await getChatList()
    chats.value = res.data || []
  } catch {
    // 静默失败
  }
}

const selectChat = async (userId) => {
  activeUserId.value = userId
  router.replace(`/chat/${userId}`)
  await loadHistory(userId)
  try {
    await markAsRead(userId)
  } catch { /* ignore */ }
  loadChatList()
}

const loadHistory = async (userId) => {
  loading.value = true
  try {
    const res = await getHistory(userId)
    messages.value = res.data || []
    await nextTick()
    scrollToBottom()
  } catch (error) {
    ElMessage({ type: 'error', message: error.message || '加载消息失败' })
  } finally {
    loading.value = false
  }
}

const sendMessage = (text) => {
  const msgText = (typeof text === 'string' ? text : inputText.value).trim()
  if (!msgText || !activeUserId.value) return

  const wsMessage = {
    receiverId: activeUserId.value,
    content: msgText,
  }

  if (window.chatWs && window.chatWs.readyState === WebSocket.OPEN) {
    // 乐观更新：立刻在本地显示消息
    const tempId = Date.now()
    messages.value.push({
      id: tempId,
      senderId: currentUser.value.id,
      senderAvatar: currentUser.value.avatar,
      receiverId: activeUserId.value,
      content: msgText,
      type: 1,
      status: 0,
      sendStatus: 'sending',
      createTime: new Date().toISOString().replace("T", " ").slice(0, 19),
    })
    inputText.value = ''
    nextTick(() => scrollToBottom())
    loadChatList()
    window.chatWs.send(JSON.stringify(wsMessage))

    // 5秒超时：如果还没收到 ACK 就标记失败
    setTimeout(() => {
      const msg = messages.value.find(m => m.id === tempId)
      if (msg && msg.sendStatus === 'sending') {
        msg.sendStatus = 'failed'
      }
    }, 5000)
  } else {
    ElMessage({ type: 'warning', message: '连接已断开，正在尝试重连...' })
  }
}

const retryMessage = (msg) => {
  // 重置状态并发起重发
  messages.value = messages.value.filter(m => m.id !== msg.id)
  sendMessage(msg.content)
}

const appendMessage = (msg) => {
  const relatedUserId = msg.senderId === currentUser.value.id ? msg.receiverId : msg.senderId
  if (activeUserId.value === relatedUserId || activeUserId.value === msg.senderId) {
    // 找到乐观更新的临时消息并替换为服务器版本
    const optIdx = messages.value.findIndex(
      m => m.id > 1000000000000 && m.senderId === msg.senderId && m.content === msg.content
    )
    if (optIdx >= 0) {
      messages.value.splice(optIdx, 1, { ...msg, sendStatus: 'sent' })
    } else {
      messages.value.push(msg)
    }
    nextTick(() => scrollToBottom())
  }
  loadChatList()
}

const scrollToBottom = () => {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

onMounted(async () => {
  // 先注册回调，防止错过 WebSocket 消息
  window.__onChatMessage = (msg) => {
    if (msg.type === 'new_message' || msg.type === 'message_sent') {
      appendMessage(msg.message)
    }
    if (msg.type === 'chat_blocked') {
      ElMessage.warning(msg.message || '请添加好友后继续私聊')
      // 标记最新一条自己发送中的消息为失败
      const failed = messages.value.findLast(m => m.senderId === currentUser.value.id && m.sendStatus === 'sending')
      if (failed) failed.sendStatus = 'failed'
    }
  }

  await loadChatList()

  const targetUserId = route.params.userId
  if (targetUserId) {
    activeUserId.value = Number(targetUserId)
    await loadHistory(Number(targetUserId))
    try { await markAsRead(Number(targetUserId)) } catch { /* ignore */ }
  }
})

watch(() => route.params.userId, (newVal) => {
  if (newVal && Number(newVal) !== activeUserId.value) {
    selectChat(Number(newVal))
  }
})
</script>

<template>
  <div class="chat-container">
    <div class="chat-sidebar">
      <ChatList
        :chats="chats"
        :active-user-id="activeUserId"
        @select="selectChat"
      />
    </div>
    <div class="chat-main">
      <div v-if="!activeUserId" class="chat-placeholder">
        <p>选择一个用户开始聊天</p>
      </div>
      <template v-else>
        <div class="chat-header">
          {{ chats.find(c => c.userId === activeUserId)?.username || '聊天' }}
        </div>
        <div ref="messagesContainer" class="chat-messages" v-loading="loading">
          <ChatMessage
            v-for="msg in messages"
            :key="msg.id"
            :message="msg"
            :is-mine="msg.senderId === currentUser.id"
            :my-avatar="currentUser.avatar"
            @retry="retryMessage"
          />
          <div v-if="messages.length === 0 && !loading" class="chat-empty">暂无消息，发送第一条消息吧</div>
        </div>
        <div class="chat-input-area">
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="3"
            placeholder="输入消息..."
            resize="none"
            @keyup.enter.exact="sendMessage"
          />
          <el-button type="primary" @click="sendMessage" :disabled="!inputText.trim()">发送</el-button>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.chat-container {
  display: flex;
  height: calc(100vh - 100px);
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.chat-sidebar {
  width: 280px;
  flex-shrink: 0;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 16px;
}

.chat-header {
  padding: 16px 20px;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid #e4e7ed;
  color: #303133;
}

.chat-messages {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.chat-empty {
  margin: auto;
  color: #909399;
  font-size: 14px;
}

.chat-input-area {
  padding: 12px 20px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.chat-input-area :deep(.el-textarea__inner) {
  resize: none;
}
</style>
