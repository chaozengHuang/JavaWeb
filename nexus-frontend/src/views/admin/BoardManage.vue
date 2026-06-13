<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getBoards, updateBoardStatus, hardDeleteBoard } from '@/api/admin'

const list = ref([])
const loading = ref(false)
const pag = ref({ current: 1, size: 10, total: 0 })
const keyword = ref('')
const statusFilter = ref('')

const load = async (p = 1) => {
  loading.value = true; pag.value.current = p
  try { const r = await getBoards({ keyword: keyword.value, status: statusFilter.value, page: p, size: pag.value.size }); list.value = r.data?.records || []; pag.value.total = r.data?.total || 0 } catch (e) { ElMessage.error(e.message) } finally { loading.value = false }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除贴吧「${row.name}」？`, '操作确认', { type: 'warning' }).then(async () => {
    await updateBoardStatus(row.id, 'DELETED'); row.status = 'DELETED'; ElMessage.success('已删除')
  }).catch(() => {})
}
const handleRestore = (row) => {
  updateBoardStatus(row.id, 'ACTIVE').then(() => { row.status = 'ACTIVE'; ElMessage.success('已恢复') })
}
const handleHardDelete = (row) => {
  ElMessageBox.confirm(`⚠ 确定从数据库彻底清除贴吧「${row.name}」？不可恢复！`, '清理确认', { confirmButtonText: '确认清理', type: 'error' }).then(async () => {
    await hardDeleteBoard(row.id); ElMessage.success('已清理'); load(pag.value.current)
  }).catch(() => {})
}

onMounted(() => load())
</script>

<template>
  <div>
    <h3>吧管理</h3>
    <div style="display:flex;gap:10px;margin-bottom:12px;">
      <el-input v-model="keyword" placeholder="搜索吧名" clearable style="width:180px" @keyup.enter="load()" />
      <el-select v-model="statusFilter" placeholder="状态" clearable style="width:120px" @change="load()"><el-option label="正常" value="ACTIVE" /><el-option label="已删除" value="DELETED" /></el-select>
      <el-button type="primary" @click="load()">查询</el-button>
    </div>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="name" label="吧名" min-width="140" />
      <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" width="90"><template #default="{row}"><el-tag :type="row.status==='ACTIVE'?'success':'danger'">{{ row.status==='ACTIVE'?'正常':'已删除' }}</el-tag></template></el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="160"><template #default="{row}">{{ (row.createdAt||'').replace('T',' ').slice(0,19) }}</template></el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{row}">
          <el-button v-if="row.status!=='DELETED'" size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          <el-button v-else size="small" type="success" @click="handleRestore(row)">恢复</el-button>
          <el-button size="small" type="danger" text style="margin-left:6px;" @click="handleHardDelete(row)">清理</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div style="text-align:center;padding:16px 0;" v-if="pag.total > pag.size">
      <el-pagination :current-page="pag.current" :page-size="pag.size" :total="pag.total" layout="prev,pager,next" background @current-change="load" />
    </div>
  </div>
</template>
