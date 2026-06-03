<script setup>
import { ref, onMounted } from 'vue'
import { getNotifyAdmin } from '@/api/admin'
import { ElMessage } from 'element-plus'

const notifyInfo = ref(null)

onMounted(async () => {
  try {
    const res = await getNotifyAdmin()
    notifyInfo.value = res.data || {}
  } catch (err) {
    ElMessage.error(err.message || '获取通知管理员信息失败')
  }
})

const copyToClipboard = (text) => {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.warning('复制失败，请手动复制')
  })
}
</script>

<template>
  <div class="notify-page">
    <h3>通知管理员</h3>

    <el-alert
      title="说明"
      type="info"
      :closable="false"
      style="margin-bottom:20px;"
    >
      <p style="margin:4px 0;">"系统通知"是一个独立的通知账号，所有系统管理员共用。</p>
      <p style="margin:4px 0;">该账号已自动与所有用户成为好友，所有系统操作通知均由该账号发送。</p>
      <p style="margin:4px 0;">请勿泄露该账号密码给非管理员人员。</p>
    </el-alert>

    <el-card shadow="hover" v-if="notifyInfo">
      <el-descriptions title="账号信息" :column="2" border>
        <el-descriptions-item label="用户名">
          {{ notifyInfo.username }}
        </el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag type="danger">通知管理员</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="用户ID">
          {{ notifyInfo.userId }}
        </el-descriptions-item>
        <el-descriptions-item label="密码">
          <span>{{ notifyInfo.password || '已加密' }}</span>
          <el-button
            v-if="notifyInfo.password && notifyInfo.password !== '已加密'"
            size="small"
            text
            type="primary"
            style="margin-left:8px;"
            @click="copyToClipboard(notifyInfo.password)"
          >复制</el-button>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-empty v-else description="暂无信息" />
  </div>
</template>

<style scoped>
.notify-page h3 {
  margin: 0 0 16px 0;
  color: #303133;
}
</style>
