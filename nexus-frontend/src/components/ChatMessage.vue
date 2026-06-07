<script setup>
import { useRouter } from 'vue-router'

const props = defineProps({
  message: { type: Object, required: true },
  isMine: { type: Boolean, default: false },
  myAvatar: { type: String, default: null },
  myUsername: { type: String, default: null },
  userAvatar: { type: String, default: null },
  peerUsername: { type: String, default: null },
})
const emit = defineEmits(['retry'])
const router = useRouter()
</script>

<template>
  <div :class="['chat-message', isMine ? 'chat-message--mine' : 'chat-message--other']">
    <img v-if="isMine && props.myAvatar" :src="'http://localhost:8081' + props.myAvatar" class="chat-message__avatar clickable" @click="router.push('/profile')" />
    <div v-else-if="isMine && !props.myAvatar" class="chat-message__avatar clickable" @click="router.push('/profile')">
      {{ (myUsername || '').toString().charAt(0) }}
    </div>
    <img v-else-if="!isMine && props.userAvatar" :src="'http://localhost:8081' + props.userAvatar" class="chat-message__avatar clickable" @click="router.push('/user/' + message.senderId)" />
    <div v-else class="chat-message__avatar clickable" @click="router.push('/user/' + message.senderId)">
      {{ (peerUsername || message.senderId || '').toString().charAt(0) }}
    </div>
    <div class="chat-message__body">
      <div class="chat-message__content">
        <div class="chat-message__text">{{ message.content }}</div>
        <!-- 发送失败红感叹号 -->
        <span
          v-if="isMine && message.sendStatus === 'failed'"
          class="chat-message__fail"
          title="发送失败，点击重发"
          @click="emit('retry', message)"
        >❗</span>
        <!-- 发送中指示 -->
        <span v-if="isMine && message.sendStatus === 'sending'" class="chat-message__pending" title="发送中">⏳</span>
      </div>
      <div class="chat-message__time">
        {{ message.createTime || '' }}
        <span v-if="isMine && message.sendStatus === 'failed'" class="chat-message__fail-text">发送失败</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chat-message {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  max-width: 75%;
}

.chat-message--mine {
  flex-direction: row-reverse;
  align-self: flex-end;
  margin-left: auto;
}

.chat-message--other {
  align-self: flex-start;
}

.chat-message__avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
  object-fit: cover;
}

.clickable {
  cursor: pointer;
}

.chat-message--other .chat-message__avatar {
  background: #409eff;
  font-weight: 600;
}

.chat-message__body {
  display: flex;
  flex-direction: column;
}

.chat-message--mine .chat-message__body {
  align-items: flex-end;
}

.chat-message__content {
  display: flex;
  align-items: center;
  gap: 6px;
}

.chat-message--mine .chat-message__content {
  flex-direction: row-reverse;
}

.chat-message__text {
  padding: 10px 14px;
  border-radius: 8px;
  background: #e9ecef;
  color: #303133;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.chat-message--mine .chat-message__text {
  background: #409eff;
  color: #fff;
}

.chat-message__fail {
  font-size: 18px;
  cursor: pointer;
  flex-shrink: 0;
  user-select: none;
}

.chat-message__fail:hover {
  transform: scale(1.2);
}

.chat-message__pending {
  font-size: 14px;
  flex-shrink: 0;
}

.chat-message__time {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}

.chat-message__fail-text {
  color: #f56c6c;
  margin-left: 4px;
}
</style>
