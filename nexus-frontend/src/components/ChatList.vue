<script setup>
defineProps({
  chats: {
    type: Array,
    default: () => [],
  },
  activeUserId: {
    type: Number,
    default: null,
  },
})

const emit = defineEmits(['select'])
</script>

<template>
  <div class="chat-list">
    <div class="chat-list__header">聊天列表</div>
    <div
      v-for="chat in chats"
      :key="chat.userId"
      :class="['chat-list__item', { 'chat-list__item--active': chat.userId === activeUserId }]"
      @click="emit('select', chat.userId)"
    >
      <div class="chat-list__avatar">{{ chat.username?.charAt(0) }}</div>
      <div class="chat-list__info">
        <div class="chat-list__name">{{ chat.username }}</div>
        <div class="chat-list__preview">{{ chat.lastMessage || '' }}</div>
      </div>
      <el-badge v-if="chat.unread > 0" :value="chat.unread" class="chat-list__badge" />
    </div>
    <div v-if="chats.length === 0" class="chat-list__empty">暂无聊天记录</div>
  </div>
</template>

<style scoped>
.chat-list {
  border-right: 1px solid #e4e7ed;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.chat-list__header {
  padding: 16px;
  font-size: 16px;
  font-weight: 600;
  border-bottom: 1px solid #e4e7ed;
  color: #303133;
}

.chat-list__item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  border-bottom: 1px solid #f2f3f5;
  transition: background 0.2s;
}

.chat-list__item:hover {
  background: #f5f7fa;
}

.chat-list__item--active {
  background: #ecf5ff;
}

.chat-list__avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;
  margin-right: 12px;
}

.chat-list__info {
  flex: 1;
  min-width: 0;
}

.chat-list__name {
  font-size: 14px;
  color: #303133;
  margin-bottom: 4px;
}

.chat-list__preview {
  font-size: 12px;
  color: #909399;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.chat-list__badge {
  flex-shrink: 0;
}

.chat-list__empty {
  padding: 24px;
  text-align: center;
  color: #909399;
  font-size: 14px;
}
</style>
