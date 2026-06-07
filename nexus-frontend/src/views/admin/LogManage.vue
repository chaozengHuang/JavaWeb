<script setup>
import { ref, onMounted } from 'vue'
import { getAdminLogs } from '@/api/admin'
import { ElMessage } from 'element-plus'

const logs = ref([])
const loading = ref(false)
const pagination = ref({ current: 1, size: 20, total: 0 })

const fetchLogs = async (page = 1) => {
  loading.value = true
  pagination.value.current = page
  try {
    const res = await getAdminLogs({ page, size: pagination.value.size })
    logs.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } catch (err) {
    ElMessage.error(err.message || '获取日志失败')
  } finally {
    loading.value = false
  }
}

const actionLabel = (action) => {
  const map = {
    RESET_PASSWORD: '重置密码',
    BAN_USER: '封禁用户',
    UNBAN_USER: '解封用户',
    UPDATE_POST_STATUS: '修改帖子状态',
    UPDATE_COMMENT_STATUS: '修改评论状态',
    BATCH_UPDATE_POST_STATUS: '批量修改帖子',
    BATCH_UPDATE_COMMENT_STATUS: '批量修改评论',
  }
  return map[action] || action
}

const targetLabel = (type) => {
  return type === 'USER' ? '用户' : type === 'POST' ? '帖子' : type === 'COMMENT' ? '评论' : type
}

const formatTime = (t) => {
  if (!t) return '-'
  return t.replace('T', ' ').substring(0, 19)
}

onMounted(() => fetchLogs())
</script>

<template>
  <div class="log-page">
    <h3>操作日志</h3>
    <el-table :data="logs" v-loading="loading" stripe border>
      <el-table-column prop="adminUsername" label="操作人" width="120" />
      <el-table-column label="操作" width="130">
        <template #default="{ row }">
          <el-tag size="small">{{ actionLabel(row.action) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip />
      <el-table-column label="时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
    </el-table>
    <div style="display:flex;justify-content:center;padding:20px 0;" v-if="pagination.total > pagination.size">
      <el-pagination
        :current-page="pagination.current"
        :page-size="pagination.size"
        :total="pagination.total"
        layout="prev, pager, next"
        @current-change="fetchLogs"
      />
    </div>
  </div>
</template>

<style scoped>
.log-page h3 {
  margin: 0 0 16px 0;
  color: #303133;
}
</style>
