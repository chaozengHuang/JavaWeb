<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPosts, updatePostStatus, batchUpdatePostStatus, hardDeletePost } from '@/api/admin'

const router = useRouter()
const loading = ref(false)
const postList = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const selectedIds = ref([])
const filters = reactive({ keyword: '', status: '', type: '', authorId: '' })

const load = async () => {
  loading.value = true
  try { const r = await getPosts({ page: page.value, size: size.value, ...filters }); postList.value = r.data?.records || []; total.value = r.data?.total || 0 } catch (e) { ElMessage.error(e.message) } finally { loading.value = false }
}

const handleStatus = (row, s) => {
  const m = { ACTIVE: '恢复', BLOCKED: '屏蔽', DELETED: '删除' }
  ElMessageBox.confirm(`确定${m[s]}帖子「${row.title?.slice(0,30)}」？`, '操作确认', { type: 'warning' }).then(async () => {
    await updatePostStatus(row.id, s); row.status = s; ElMessage.success('操作成功')
  }).catch(() => {})
}

const handleHardDelete = (row) => {
  ElMessageBox.confirm(`⚠ 确定从数据库彻底清除帖子「${row.title?.slice(0,30)}」？此操作不可恢复！`, '清理确认', { confirmButtonText: '确认清理', type: 'error' }).then(async () => {
    await hardDeletePost(row.id); ElMessage.success('已清理'); load()
  }).catch(() => {})
}

const handleBatch = (s) => {
  if (!selectedIds.value.length) return ElMessage.warning('请先选择帖��')
  const m = { ACTIVE: '恢复', BLOCKED: '屏蔽', DELETED: '删除', HARD: '清理' }
  ElMessageBox.confirm(`确定批量${m[s]} ${selectedIds.value.length} 条帖子？`, '批量操作', { type: 'warning' }).then(async () => {
    if (s === 'HARD') { for (const id of selectedIds.value) { await hardDeletePost(id) } ElMessage.success('已批量清理'); load(); return }
    await batchUpdatePostStatus(selectedIds.value, s); ElMessage.success('批量操作成功'); load()
  }).catch(() => {})
}

const sLabel = (s) => ({ ACTIVE: '正常', BLOCKED: '已屏蔽', DELETED: '已删除' }[s] || s)
const sType = (s) => ({ ACTIVE: 'success', BLOCKED: 'warning', DELETED: 'danger' }[s] || 'info')
const fmt = (t) => t ? t.replace('T', ' ').slice(0, 19) : '-'

onMounted(load)
</script>

<template>
  <div>
    <h3>帖子管理</h3>
    <div style="display:flex;gap:10px;margin-bottom:12px;flex-wrap:wrap;">
      <el-input v-model="filters.keyword" placeholder="搜索标题" clearable style="width:180px" @keyup.enter="load" />
      <el-select v-model="filters.status" placeholder="状态" clearable style="width:120px" @change="load"><el-option label="正常" value="ACTIVE" /><el-option label="已屏蔽" value="BLOCKED" /><el-option label="已删除" value="DELETED" /></el-select>
      <el-button type="primary" @click="load">搜索</el-button>
      <el-button @click="filters.keyword='';filters.status='';load()">重置</el-button>
    </div>
    <div v-if="selectedIds.length" style="display:flex;gap:8px;padding:8px 12px;background:#ecf5ff;border-radius:6px;margin-bottom:8px;">
      <span>已选 {{ selectedIds.length }} 项</span>
      <el-button size="small" type="warning" @click="handleBatch('BLOCKED')">批量屏蔽</el-button>
      <el-button size="small" type="success" @click="handleBatch('ACTIVE')">批量恢复</el-button>
      <el-button size="small" type="danger" @click="handleBatch('DELETED')">批量删除</el-button>
      <el-button size="small" type="danger" plain @click="handleBatch('HARD')">批量清理</el-button>
    </div>
    <el-table :data="postList" v-loading="loading" border stripe @selection-change="(rows) => selectedIds = rows.map(r => r.id)">
      <el-table-column type="selection" width="50" />
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="boardName" label="所属吧" width="100" />
      <el-table-column prop="authorName" label="作者" width="100" />
      <el-table-column label="状态" width="80"><template #default="{row}"><el-tag :type="sType(row.status)" size="small">{{ sLabel(row.status) }}</el-tag></template></el-table-column>
      <el-table-column label="时间" width="160"><template #default="{row}">{{ fmt(row.createdAt) }}</template></el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{row}">
          <el-button size="small" text type="primary" @click="router.push('/post/'+row.id)">跳转</el-button>
          <el-button v-if="row.status!=='ACTIVE'" size="small" text type="success" @click="handleStatus(row,'ACTIVE')">恢复</el-button>
          <el-button v-if="row.status!=='BLOCKED'" size="small" text type="warning" @click="handleStatus(row,'BLOCKED')">屏蔽</el-button>
          <el-button v-if="row.status!=='DELETED'" size="small" text type="danger" @click="handleStatus(row,'DELETED')">删除</el-button>
          <el-button size="small" text type="danger" style="margin-left:6px;border-left:1px solid #eee;padding-left:8px;" @click="handleHardDelete(row)">清理</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="text-align:center;padding:16px 0;" v-if="total > size">
      <el-pagination :current-page="page" :page-size="size" :total="total" layout="prev,pager,next" background @current-change="(p) => { page = p; load() }" />
    </div>
  </div>
</template>
