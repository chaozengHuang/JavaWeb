<script setup>
defineProps({
  message: {
    type: Object,
    required: true,
  },
  isMine: {
    type: Boolean,
    default: false,
  },
})
</script>

<template>
  <div :class="['chat-message', isMine ? 'chat-message--mine' : 'chat-message--other']">
    <div class="chat-message__avatar">
      {{ isMine ? '我' : (message.senderId || '').toString().slice(-2) }}
    </div>
    <div class="chat-message__body">
      <div class="chat-message__text">{{ message.content }}</div>
      <div class="chat-message__time">{{ message.createTime || '' }}</div>
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
  font-size: 12px;
  flex-shrink: 0;
}

.chat-message--other .chat-message__avatar {
  background: #67c23a;
}

.chat-message__body {
  display: flex;
  flex-direction: column;
}

.chat-message--mine .chat-message__body {
  align-items: flex-end;
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

.chat-message__time {
  font-size: 11px;
  color: #909399;
  margin-top: 4px;
}
</style>
