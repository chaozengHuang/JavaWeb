<script setup>
import { ref, onMounted } from 'vue'
import { getBoards, updateBoardStatus } from '@/api/admin'
import { ElMessage, ElMessageBox } from 'element-plus'

const boards = ref([])
const loading = ref(false)
const pagination = ref({ current: 1, size: 10, total: 0 })
const keyword = ref('')
const statusFilter = ref('')

const fetchBoards = async (page = 1) => {
  loading.value = true
  pagination.value.current = page
  try {
    const res = await getBoards({
      keyword: keyword.value,
      status: statusFilter.value,
      page,
      size: pagination.value.size,
    })
    boards.value = res.data?.records || []
    pagination.value.total = res.data?.total || 0
  } catch (err) {
    ElMessage.error(err.message || '获取吧列表失败')
  } finally {
    loading.value = false
  }
}

const handleDelete = async (board) => {
  try {
    await ElMessageBox.confirm(`确定删除贴吧「${board.name}」吗？所有成员将收到通知。`, '危险操作', { type: 'error' })
    await updateBoardStatus(board.id, 'DELETED')
    ElMessage.success('已删除贴吧')
    fetchBoards(pagination.value.current)
  } catch (err) {
    if (err !== 'cancel') ElMessage.error(err.message || '操作失败')
  }
}

const handleRestore = async (board) => {
  try {
    await updateBoardStatus(board.id, 'ACTIVE')
    ElMessage.success('已恢复贴吧')
    fetchBoards(pagination.value.current)
  } catch (err) {
    ElMessage.error(err.message || '恢复失败')
  }
}

onMounted(() => fetchBoards())
</script>

<template>
  <div class="board-manage">
    <h3>吧管理</h3>

    <div class="filter-bar">
      <el-input v-model="keyword" placeholder="搜索吧名" clearable style="width:200px;margin-right:10px;" @clear="fetchBoards()" @keyup.enter="fetchBoards()" />
      <el-select v-model="statusFilter" placeholder="状态筛选" clearable style="width:120px;margin-right:10px;" @change="fetchBoards()">
        <el-option label="正常" value="ACTIVE" />
        <el-option label="已删除" value="DELETED" />
      </el-select>
      <el-button type="primary" @click="fetchBoards()">查询</el-button>
    </div>

    <el-table :data="boards" v-loading="loading" stripe border style="margin-top:12px;">
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="name" label="吧名" min-width="150" />
      <el-table-column prop="description" label="描述" min-width="160" show-overflow-tooltip />
      <el-table-column prop="creatorId" label="创建者ID" width="100" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag v-if="row.status === 'ACTIVE'" type="success">正常</el-tag>
          <el-tag v-else-if="row.status === 'HIDDEN'" type="warning">隐藏</el-tag>
          <el-tag v-else type="danger">已删除</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170">
        <template #default="{ row }">{{ row.createdAt?.replace('T',' ').substring(0,19) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button v-if="row.status !== 'DELETED'" type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          <el-button v-else type="success" size="small" @click="handleRestore(row)">恢复</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div style="display:flex;justify-content:center;padding:20px 0;" v-if="pagination.total > pagination.size">
      <el-pagination
        :current-page="pagination.current"
        :page-size="pagination.size"
        :total="pagination.total"
        layout="prev, pager, next"
        @current-change="fetchBoards"
      />
    </div>
  </div>
</template>

<style scoped>
.board-manage h3 {
  margin: 0 0 12px 0;
  color: #303133;
}
.filter-bar {
  display: flex;
  align-items: center;
}
</style>
