<script setup>
import { computed } from 'vue'
import { useRouter as vueRouter } from 'vue-router'

const props = defineProps({
  message: { type: Object, required: true },
  isMine: { type: Boolean, default: false },
  myAvatar: { type: String, default: null },
  myUsername: { type: String, default: null },
  userAvatar: { type: String, default: null },
  peerUsername: { type: String, default: null },
})
const emit = defineEmits(['retry'])
const router = vueRouter()

const BASE = ''

// 解析消息内容：提取图片、链接、纯文本
const parts = computed(() => {
  const text = props.message.content || ''
  const all = []

  // 匹配 ![alt](/uploads/...) 图片
  let m
  const imgRegex = /!\[([^\]]*)\]\((\/uploads\/[^)]+)\)/g
  while ((m = imgRegex.exec(text)) !== null) {
    all.push({ idx: m.index, end: m.index + m[0].length, type: 'image', url: m[2], alt: m[1] })
  }

  // 匹配文本中的 /post/数字(?comment=数字) 链接
  const linkRegex = /(\/post\/\d+(?:\?comment=\d+)?)/g
  while ((m = linkRegex.exec(text)) !== null) {
    // 避免和图片中的 url 重叠
    const insideImage = all.some(img => img.idx <= m.index && img.end >= m.index + m[0].length)
    if (!insideImage) {
      all.push({ idx: m.index, end: m.index + m[0].length, type: 'postLink', url: m[1] })
    }
  }

  all.sort((a, b) => a.idx - b.idx)
  const result = []
  let lastIdx = 0
  for (const item of all) {
    if (item.idx > lastIdx) {
      result.push({ type: 'text', content: text.slice(lastIdx, item.idx) })
    }
    result.push(item)
    lastIdx = item.end
  }
  if (lastIdx < text.length) {
    result.push({ type: 'text', content: text.slice(lastIdx) })
  }
  return result.length > 0 ? result : [{ type: 'text', content: text }]
})
</script>

<template>
  <div :class="['chat-message', isMine ? 'chat-message--mine' : 'chat-message--other']">
    <img v-if="isMine && props.myAvatar" :src="BASE + props.myAvatar" class="chat-message__avatar clickable" @click="router.push('/profile')" />
    <div v-else-if="isMine && !props.myAvatar" class="chat-message__avatar clickable" @click="router.push('/profile')">
      {{ (myUsername || '').charAt(0) }}
    </div>
    <img v-else-if="!isMine && props.userAvatar" :src="BASE + props.userAvatar" class="chat-message__avatar clickable" @click="router.push('/user/' + message.senderId)" />
    <div v-else class="chat-message__avatar clickable" @click="router.push('/user/' + message.senderId)">
      {{ (peerUsername || '').charAt(0) }}
    </div>
    <div class="chat-message__body">
      <div class="chat-message__content">
        <div class="chat-message__text">
          <template v-for="(part, i) in parts" :key="i">
            <span v-if="part.type === 'text'">{{ part.content }}</span>
            <img v-else-if="part.type === 'image'" :src="BASE + part.url" class="chat-image" />
            <a v-else-if="part.type === 'postLink'" class="chat-post-link" @click="router.push(part.url)">点击查看帖子</a>
          </template>
        </div>
        <span v-if="isMine && message.sendStatus === 'failed'" class="chat-message__fail" title="发送失败，点击重发" @click="emit('retry', message)">❗</span>
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
  display: flex; gap: 10px; margin-bottom: 16px; max-width: 75%;
}
.chat-message--mine { flex-direction: row-reverse; align-self: flex-end; margin-left: auto; }
.chat-message--other { align-self: flex-start; }
.chat-message__avatar {
  width: 36px; height: 36px; border-radius: 50%; background: #409eff; color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 14px;
  font-weight: 600; flex-shrink: 0; object-fit: cover;
}
.clickable { cursor: pointer; }
.chat-message--other .chat-message__avatar { background: #409eff; font-weight: 600; }
.chat-message__body { display: flex; flex-direction: column; }
.chat-message--mine .chat-message__body { align-items: flex-end; }
.chat-message__content { display: flex; align-items: flex-end; gap: 6px; }
.chat-message--mine .chat-message__content { flex-direction: row-reverse; }
.chat-message__text {
  padding: 10px 14px; border-radius: 8px; background: #e9ecef; color: #303133;
  font-size: 14px; line-height: 1.6; word-break: break-word; white-space: pre-wrap;
}
.chat-message--mine .chat-message__text { background: #409eff; color: #fff; }
.chat-image { max-width: 200px; max-height: 200px; border-radius: 6px; display: block; margin: 4px 0; }
.chat-post-link {
  display: inline-block; padding: 3px 10px; background: rgba(64,158,255,.15);
  color: #409eff; border-radius: 6px; font-size: 13px; cursor: pointer;
  text-decoration: none; margin: 4px 0;
}
.chat-post-link:hover { background: rgba(64,158,255,.25); }
.chat-file-link {
  display: inline-flex; align-items: center; gap: 4px; font-size: 12px; color: #409eff;
  text-decoration: underline; cursor: pointer; margin: 4px 0;
}
.chat-message__fail { font-size: 18px; cursor: pointer; flex-shrink: 0; }
.chat-message__fail:hover { transform: scale(1.2); }
.chat-message__pending { font-size: 14px; flex-shrink: 0; }
.chat-message__time { font-size: 11px; color: #909399; margin-top: 4px; }
.chat-message__fail-text { color: #f56c6c; margin-left: 4px; }
</style>
