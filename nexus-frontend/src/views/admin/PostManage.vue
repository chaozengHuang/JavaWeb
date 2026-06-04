<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPosts, getPostDetail, updatePostStatus, batchUpdatePostStatus } from '@/api/admin'

const router = useRouter()

// ==================== 列表状态 ====================
const loading = ref(false)
const postList = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const selectedIds = ref([])

const filters = reactive({
  keyword: '',
  status: '',
  type: '',
  authorId: '',
})

// ==================== 详情弹窗 ====================
const detailVisible = ref(false)
const detailPost = ref(null)

// ==================== 数据加载 ====================
const loadPosts = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: pageSize.value }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.status) params.status = filters.status
    if (filters.type) params.type = filters.type
    if (filters.authorId) params.authorId = filters.authorId
    const res = await getPosts(params)
    postList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (err) {
    ElMessage({ type: 'error', message: err.message || '加载失败' })
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  selectedIds.value = []
  loadPosts()
}

const handleReset = () => {
  filters.keyword = ''
  filters.status = ''
  filters.type = ''
  filters.authorId = ''
  page.value = 1
  selectedIds.value = []
  loadPosts()
}

const handlePageChange = (p) => {
  page.value = p
  loadPosts()
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map(r => r.id)
}

// ==================== 查看详情 ====================
const handleViewDetail = async (post) => {
  try {
    const res = await getPostDetail(post.id)
    detailPost.value = res.data
    detailVisible.value = true
  } catch (err) {
    ElMessage({ type: 'error', message: err.message || '加载失败' })
  }
}

// ==================== 状态操作 ====================
const handleUpdateStatus = (post, status) => {
  const labels = { ACTIVE: '恢复为正常', BLOCKED: '屏蔽', DELETED: '删除' }
  const action = labels[status] || status

  ElMessageBox.confirm(
    `确定要${action}帖子「${post.title?.substring(0, 30)}」吗？`,
    '操作确认',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      await updatePostStatus(post.id, status)
      post.status = status
      ElMessage({ type: 'success', message: '操作成功' })
    } catch (err) {
      ElMessage({ type: 'error', message: err.message || '操作失败' })
    }
  }).catch(() => {})
}

const handleBatchAction = (status) => {
  if (selectedIds.value.length === 0) {
    ElMessage({ type: 'warning', message: '请先选择帖子' })
    return
  }
  const labels = { ACTIVE: '恢复', BLOCKED: '屏蔽', DELETED: '删除' }
  const action = labels[status] || status

  ElMessageBox.confirm(
    `确定要批量${action}已选中的 ${selectedIds.value.length} 个帖子吗？`,
    '批量操作确认',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      await batchUpdatePostStatus(selectedIds.value, status)
      ElMessage({ type: 'success', message: '批量操作成功' })
      selectedIds.value = []
      loadPosts()
    } catch (err) {
      ElMessage({ type: 'error', message: err.message || '操作失败' })
    }
  }).catch(() => {})
}

// ==================== 格式化 ====================
const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

const statusLabel = (status) => {
  const map = { ACTIVE: '正常', BLOCKED: '已屏蔽', DELETED: '已删除' }
  return map[status] || status || '正常'
}

const statusType = (status) => {
  const map = { ACTIVE: 'success', BLOCKED: 'warning', DELETED: 'danger' }
  return map[status] || 'info'
}

onMounted(() => {
  loadPosts()
})
</script>

<template>
  <div class="post-manage">
    <h3>帖子管理</h3>

    <!-- 搜索筛选 -->
    <div class="filter-bar">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索标题/内容"
        clearable
        style="width: 220px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="filters.status" placeholder="状态筛选" clearable style="width: 140px">
        <el-option label="正常" value="ACTIVE" />
        <el-option label="已屏蔽" value="BLOCKED" />
        <el-option label="已删除" value="DELETED" />
      </el-select>
      <el-select v-model="filters.type" placeholder="类型筛选" clearable style="width: 140px">
        <el-option label="普通帖" value="NORMAL" />
        <el-option label="悬赏帖" value="REWARD" />
      </el-select>
      <el-input
        v-model="filters.authorId"
        placeholder="作者ID"
        clearable
        style="width: 120px"
      />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <!-- 批量操作 -->
    <div class="batch-bar" v-if="selectedIds.length > 0">
      <span>已选 {{ selectedIds.length }} 项</span>
      <el-button size="small" type="warning" @click="handleBatchAction('BLOCKED')">批量屏蔽</el-button>
      <el-button size="small" type="success" @click="handleBatchAction('NORMAL')">批量恢复</el-button>
      <el-button size="small" type="danger" @click="handleBatchAction('DELETED')">批量删除</el-button>
    </div>

    <!-- 帖子表格 -->
    <el-table
      v-loading="loading"
      :data="postList"
      border
      stripe
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
      <el-table-column prop="boardName" label="所属吧" width="120" show-overflow-tooltip />
      <el-table-column prop="authorName" label="作者" width="120" />
      <el-table-column label="类型" width="90">
        <template #default="{ row }">
          <el-tag :type="row.type === 'REWARD' ? 'warning' : ''" size="small">
            {{ row.type === 'REWARD' ? '悬赏' : '普通' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="发布时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" text @click="handleViewDetail(row)">详情</el-button>
          <el-button size="small" text type="warning" @click="router.push('/post/' + row.id)">跳转</el-button>
          <el-button
            v-if="row.status !== 'ACTIVE'"
            type="success"
            size="small"
            text
            @click="handleUpdateStatus(row, 'ACTIVE')"
          >恢复</el-button>
          <el-button
            v-if="row.status !== 'BLOCKED'"
            type="warning"
            size="small"
            text
            @click="handleUpdateStatus(row, 'BLOCKED')"
          >屏蔽</el-button>
          <el-button
            v-if="row.status !== 'DELETED'"
            type="danger"
            size="small"
            text
            @click="handleUpdateStatus(row, 'DELETED')"
          >删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next, total"
        background
        @current-change="handlePageChange"
      />
    </div>

    <!-- 帖子详情弹窗 -->
    <el-dialog v-model="detailVisible" title="帖子详情" width="650px">
      <template v-if="detailPost">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ detailPost.id }}</el-descriptions-item>
          <el-descriptions-item label="作者ID">{{ detailPost.authorId }}</el-descriptions-item>
          <el-descriptions-item label="标题" :span="2">{{ detailPost.title }}</el-descriptions-item>
          <el-descriptions-item label="板块ID">{{ detailPost.boardId }}</el-descriptions-item>
          <el-descriptions-item label="类型">{{ detailPost.type }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detailPost.status)" size="small">
              {{ statusLabel(detailPost.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="悬赏积分">{{ detailPost.rewardPoints ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ formatTime(detailPost.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatTime(detailPost.updatedAt) }}</el-descriptions-item>
          <el-descriptions-item label="内容" :span="2">
            <div class="content-preview">{{ detailPost.content }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.post-manage h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  color: #303133;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.batch-bar {
  display: flex;
  gap: 10px;
  align-items: center;
  margin-bottom: 12px;
  padding: 8px 12px;
  background: #ecf5ff;
  border-radius: 6px;
  font-size: 13px;
  color: #409eff;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.content-preview {
  max-height: 300px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
  font-size: 13px;
  color: #606266;
  line-height: 1.7;
}
</style>
