<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getHistory, markAsRead, getChatList } from '@/api/message'
import { uploadImage, uploadFile } from '@/api/upload'
import { isFriend } from '@/api/friend'
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
const showEmoji = ref(false)
const emojis = ['😀','😃','😄','😁','😅','😂','🤣','😊','😇','🙂','😉','😌','😍','🥰','😘','😗','😋','😛','😜','🤪','😝','🤑','🤗','🤭','🤫','🤔','🤐','😏','😒','😔','😕','🙁','☹️','😣','😖','😫','😩','🥺','😢','😭','😤','😠','😡','🤬','💀','☠️','👍','👎','👏','🙌','💪','🤝','🎉','🎊','❤️','🔥','⭐','✅','❌','💯']
const isNotFriend = ref(false)

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
    // ignore
  }
}

const selectChat = async (userId) => {
  activeUserId.value = userId
  router.replace(`/chat/${userId}`)
  await loadChatList()
  await loadHistory(userId)
  checkFriendStatus()
  try { await markAsRead(userId) } catch { /* ignore */ }
}

const failedMsgsKey = computed(() =>
  `failed_msgs_${currentUser.value.id}_${activeUserId.value}`
)

const loadHistory = async (userId) => {
  loading.value = true
  try {
    const res = await getHistory(userId)
    const fromServer = (res.data || []).map(m => ({ ...m, sendStatus: 'sent' }))
    // 加载本地失败消息（仅发送人可见）
    const key = `failed_msgs_${currentUser.value.id}_${userId}`
    const saved = localStorage.getItem(key)
    const failedMsgs = saved ? JSON.parse(saved) : []
    // 合并并按时间排序
    messages.value = [...fromServer, ...failedMsgs].sort(
      (a, b) => new Date(a.createTime || 0) - new Date(b.createTime || 0)
    )
    await nextTick()
    scrollToBottom()
  } catch (error) {
    ElMessage({ type: 'error', message: error.message || '加载消息失败' })
  } finally {
    loading.value = false
  }
}

const saveFailedMessages = () => {
  if (!activeUserId.value) return
  const key = `failed_msgs_${currentUser.value.id}_${activeUserId.value}`
  const failed = messages.value.filter(
    m => m.sendStatus === 'failed' && m.senderId === currentUser.value.id
  )
  if (failed.length > 0) {
    localStorage.setItem(key, JSON.stringify(failed))
  } else {
    localStorage.removeItem(key)
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
    const tempId = Date.now()
    messages.value.push({
      id: tempId,
      senderId: currentUser.value.id,
      receiverId: activeUserId.value,
      content: msgText,
      type: 1,
      status: 0,
      sendStatus: 'sending',
      createTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
    })
    inputText.value = ''
    nextTick(() => scrollToBottom())
    loadChatList()
    window.chatWs.send(JSON.stringify(wsMessage))

    // 5秒超时标记失败
    setTimeout(() => {
      const msg = messages.value.find(m => m.id === tempId)
      if (msg && msg.sendStatus === 'sending') {
        msg.sendStatus = 'failed'
        saveFailedMessages()
      }
    }, 5000)
  } else {
    // WebSocket 断开，直接记录为失败
    ElMessage({ type: 'warning', message: '连接已断开，正在尝试重连...' })
    const tempId = Date.now()
    messages.value.push({
      id: tempId,
      senderId: currentUser.value.id,
      receiverId: activeUserId.value,
      content: msgText,
      type: 1,
      status: 0,
      sendStatus: 'failed',
      createTime: new Date().toISOString().replace('T', ' ').slice(0, 19),
    })
    nextTick(() => scrollToBottom())
    saveFailedMessages()
  }
}

const retryMessage = (msg) => {
  messages.value = messages.value.filter(m => m.id !== msg.id)
  saveFailedMessages()
  sendMessage(msg.content)
}

const handleChatImage = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) { ElMessage.warning('请选择图片文件'); return }
  try {
    const res = await uploadImage(file)
    const url = res.data?.url || ''
    const msgText = `![图片](${url})`
    sendMessage(msgText)
  } catch (err) { ElMessage.error('图片上传失败') }
  e.target.value = ''
}

const handleChatFile = async (e) => {
  e.target.value = ''
  ElMessage.info('文件发送功能正在开发中，敬请期待')
}

const appendMessage = (msg) => {
  const relatedUserId = msg.senderId === currentUser.value.id ? msg.receiverId : msg.senderId
  if (activeUserId.value === relatedUserId || activeUserId.value === msg.senderId) {
    // 优先查找发送中的本地消息进行替换
    const optIdx = messages.value.findIndex(
      m => m.sendStatus === 'sending' && m.senderId === msg.senderId && m.content === msg.content
    )
    if (optIdx >= 0) {
      messages.value.splice(optIdx, 1, { ...msg, sendStatus: 'sent' })
      saveFailedMessages()
    } else {
      messages.value.push({ ...msg, sendStatus: 'sent' })
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

const getPeerAvatar = () => {
  return chats.value.find(c => c.userId === activeUserId.value)?.avatar || null
}

const getPeerUsername = () => {
  return chats.value.find(c => c.userId === activeUserId.value)?.username || null
}

const checkFriendStatus = async () => {
  if (!activeUserId.value || activeUserId.value === currentUser.value.id) { isNotFriend.value = false; return }
  try { const res = await isFriend(Number(activeUserId.value)); isNotFriend.value = res.data?.status !== 'accepted' } catch { isNotFriend.value = false }
}

onMounted(async () => {
  window.__onChatMessage = (msg) => {
    if (msg.type === 'new_message' || msg.type === 'message_sent') {
      appendMessage(msg.message)
    }
    if (msg.type === 'chat_blocked') {
      ElMessage.warning(msg.message || '请添加好友后继续私聊')
      const failed = messages.value.findLast(m => m.senderId === currentUser.value.id && m.sendStatus === 'sending')
      if (failed) {
        failed.sendStatus = 'failed'
        saveFailedMessages()
      }
    }
  }

  await loadChatList()

  const targetUserId = route.params.userId
  if (targetUserId) {
    activeUserId.value = Number(targetUserId)
    await loadHistory(Number(targetUserId))
    checkFriendStatus()
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
            :my-username="currentUser.username"
            :user-avatar="getPeerAvatar()"
            :peer-username="getPeerUsername()"
            @retry="retryMessage"
          />
          <div v-if="messages.length === 0 && !loading" class="chat-empty">暂无消息，发送第一条消息吧</div>
        </div>
        <div v-if="isNotFriend" class="nonfriend-hint">⚠ 当前双方不是好友，每人最多发送一条消息</div>
        <div class="chat-input-area">
          <div class="chat-input-left">
            <div class="chat-toolbar">
              <label class="chat-tool-btn" title="发送图片">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" stroke-width="1.5"/><circle cx="8.5" cy="8.5" r="1.5" fill="currentColor"/><path d="M21 15l-5-5L5 21h16v-6z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>
                <input type="file" accept="image/*" @change="handleChatImage" />
              </label>
              <label class="chat-tool-btn" title="发送文件">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M13 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V9z" stroke="currentColor" stroke-width="1.5"/><path d="M13 2v7h7" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>
                <input type="file" @change="handleChatFile" />
              </label>
              <button class="chat-tool-btn" :class="{ active: showEmoji }" title="表情" @click="showEmoji = !showEmoji">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.5"/><path d="M8 14s1.5 2 4 2 4-2 4-2M9 9h.01M15 9h.01" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/></svg>
              </button>
            </div>
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="2"
              placeholder="输入消息..."
              resize="none"
              class="chat-textarea"
              @keyup.enter.exact="sendMessage"
            />
          </div>
          <el-button type="primary" class="chat-send-btn" @click="sendMessage" :disabled="!inputText.trim()">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
          </el-button>
        </div>
        <!-- 表情面板 -->
        <transition name="emoji-fade">
          <div v-if="showEmoji" class="emoji-panel">
            <div class="emoji-grid">
              <button v-for="e in emojis" :key="e" class="emoji-item" @click="inputText += e; showEmoji = false">{{ e }}</button>
            </div>
          </div>
        </transition>
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

.chat-sidebar { width: 280px; flex-shrink: 0; }

.chat-main { flex: 1; display: flex; flex-direction: column; }

.chat-placeholder {
  flex: 1; display: flex; align-items: center; justify-content: center;
  color: #909399; font-size: 16px;
}

.chat-header {
  padding: 16px 20px; font-size: 16px; font-weight: 600;
  border-bottom: 1px solid #e4e7ed; color: #303133;
}

.chat-messages {
  flex: 1; padding: 20px; overflow-y: auto; display: flex; flex-direction: column;
}

.chat-empty { margin: auto; color: #909399; font-size: 14px; }

.chat-input-area {
  padding: 10px 16px; border-top: 1px solid #e4e7ed;
  display: flex; gap: 10px; align-items: flex-end;
  background: #fafafa;
}

.chat-input-left { flex: 1; display: flex; flex-direction: column; gap: 6px; }

.chat-toolbar { display: flex; gap: 4px; padding-left: 2px; }

.chat-tool-btn {
  display: flex; align-items: center; justify-content: center;
  width: 32px; height: 32px; border: none; border-radius: 8px;
  background: transparent; color: #909399; cursor: pointer;
  transition: all 0.15s; position: relative;
}
.chat-tool-btn:hover { background: #e8e8e8; color: #606266; }
.chat-tool-btn.active { background: #ecf5ff; color: #409eff; }
.chat-tool-btn input { position: absolute; inset: 0; opacity: 0; cursor: pointer; }

.chat-textarea :deep(.el-textarea__inner) {
  background: #fff; border-radius: 10px; border-color: #e4e7ed;
  padding: 8px 12px; font-size: 14px; transition: border-color 0.2s; resize: none;
}
.chat-textarea :deep(.el-textarea__inner):focus { border-color: #409eff; }

.chat-send-btn {
  width: 38px; height: 38px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; padding: 0;
}

.emoji-panel {
  background: #fff; border-top: 1px solid #e4e7ed;
  padding: 10px 14px; max-height: 200px; overflow-y: auto;
}
.emoji-grid { display: grid; grid-template-columns: repeat(12, 1fr); gap: 4px; }
.emoji-item {
  width: 100%; aspect-ratio: 1; display: flex; align-items: center; justify-content: center;
  font-size: 20px; cursor: pointer; border: none; background: transparent;
  border-radius: 8px; transition: background 0.12s;
}
.emoji-item:hover { background: #f0f0f0; transform: scale(1.15); }
.emoji-fade-enter-active, .emoji-fade-leave-active { transition: all 0.2s ease; }
.emoji-fade-enter-from, .emoji-fade-leave-to { opacity: 0; max-height: 0; padding-top: 0; padding-bottom: 0; }
.nonfriend-hint { padding: 6px 16px; background: #fef3e6; color: #e6a23c; font-size: 12px; text-align: center; border-top: 1px solid #faecd8; }
</style>
