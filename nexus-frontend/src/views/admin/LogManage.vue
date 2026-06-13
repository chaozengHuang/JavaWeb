<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminLogs, deleteLog } from '@/api/admin'

const logs = ref([])
const loading = ref(false)
const pag = ref({ current: 1, size: 20, total: 0 })

const load = async (p = 1) => {
  loading.value = true; pag.value.current = p
  try { const r = await getAdminLogs({ page: p, size: pag.value.size }); logs.value = r.data?.records || []; pag.value.total = r.data?.total || 0 } catch (e) { ElMessage.error(e.message) } finally { loading.value = false }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定删除该条日志？', '提示', { type: 'warning' }).then(async () => {
    await deleteLog(row.id); ElMessage.success('已删除'); load(pag.value.current)
  }).catch(() => {})
}

const aLabel = (a) => ({ RESET_PASSWORD: '重置密码', BAN_USER: '封禁', UNBAN_USER: '解封', UPDATE_POST_STATUS: '改帖状态', UPDATE_COMMENT_STATUS: '改评状态', HARD_DELETE_USER: '清理用户', HARD_DELETE_POST: '清理帖子', HARD_DELETE_COMMENT: '清理评论', HARD_DELETE_BOARD: '清理吧', DELETE_BOARD: '删吧', RESTORE_BOARD: '恢复吧', BATCH_UPDATE_POST_STATUS: '批量改帖', BATCH_UPDATE_COMMENT_STATUS: '批量改评' }[a] || a)
const fmt = (t) => t ? t.replace('T', ' ').slice(0, 19) : '-'

onMounted(load)
</script>

<template>
  <div>
    <h3>操作日志</h3>
    <el-table :data="logs" v-loading="loading" stripe border>
      <el-table-column prop="adminUsername" label="操作人" width="100" />
      <el-table-column label="操作" width="110"><template #default="{row}"><el-tag size="small">{{ aLabel(row.action) }}</el-tag></template></el-table-column>
      <el-table-column prop="targetType" label="目标类型" width="80" />
      <el-table-column prop="detail" label="详情" min-width="200" show-overflow-tooltip />
      <el-table-column label="时间" width="160"><template #default="{row}">{{ fmt(row.createdAt) }}</template></el-table-column>
      <el-table-column label="操作" width="60" fixed="right">
        <template #default="{row}"><el-button size="small" text type="danger" @click="handleDelete(row)">删除</el-button></template>
      </el-table-column>
    </el-table>
    <div style="text-align:center;padding:16px 0;" v-if="pag.total > pag.size">
      <el-pagination :current-page="pag.current" :page-size="pag.size" :total="pag.total" layout="prev,pager,next" background @current-change="load" />
    </div>
  </div>
</template>
