<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getComments, getCommentDetail, updateCommentStatus, batchUpdateCommentStatus } from '@/api/admin'

const router = useRouter()

// ==================== 列表状态 ====================
const loading = ref(false)
const commentList = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)
const selectedIds = ref([])

const filters = reactive({
  keyword: '',
  status: '',
  postId: '',
  authorId: '',
})

// ==================== 详情弹窗 ====================
const detailVisible = ref(false)
const detailComment = ref(null)

// ==================== 数据加载 ====================
const loadComments = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: pageSize.value }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.status) params.status = filters.status
    if (filters.postId) params.postId = filters.postId
    if (filters.authorId) params.authorId = filters.authorId
    const res = await getComments(params)
    commentList.value = res.data?.records || []
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
  loadComments()
}

const handleReset = () => {
  filters.keyword = ''
  filters.status = ''
  filters.postId = ''
  filters.authorId = ''
  page.value = 1
  selectedIds.value = []
  loadComments()
}

const handlePageChange = (p) => {
  page.value = p
  loadComments()
}

const handleSelectionChange = (rows) => {
  selectedIds.value = rows.map(r => r.id)
}

// ==================== 查看详情 ====================
const handleViewDetail = async (comment) => {
  try {
    const res = await getCommentDetail(comment.id)
    detailComment.value = res.data
    detailVisible.value = true
  } catch (err) {
    ElMessage({ type: 'error', message: err.message || '加载失败' })
  }
}

// ==================== 状态操作 ====================
const handleUpdateStatus = (comment, status) => {
  const labels = { ACTIVE: '恢复为正常', BLOCKED: '屏蔽', DELETED: '删除' }
  const action = labels[status] || status

  ElMessageBox.confirm(
    `确定要${action}该评论吗？`,
    '操作确认',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      await updateCommentStatus(comment.id, status)
      comment.status = status
      ElMessage({ type: 'success', message: '操作成功' })
    } catch (err) {
      ElMessage({ type: 'error', message: err.message || '操作失败' })
    }
  }).catch(() => {})
}

const handleBatchAction = (status) => {
  if (selectedIds.value.length === 0) {
    ElMessage({ type: 'warning', message: '请先选择评论' })
    return
  }
  const labels = { ACTIVE: '恢复', BLOCKED: '屏蔽', DELETED: '删除' }
  const action = labels[status] || status

  ElMessageBox.confirm(
    `确定要批量${action}已选中的 ${selectedIds.value.length} 条评论吗？`,
    '批量操作确认',
    { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      await batchUpdateCommentStatus(selectedIds.value, status)
      ElMessage({ type: 'success', message: '批量操作成功' })
      selectedIds.value = []
      loadComments()
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

const truncateContent = (content, maxLen = 80) => {
  if (!content) return ''
  return content.length > maxLen ? content.substring(0, maxLen) + '...' : content
}

onMounted(() => {
  loadComments()
})
</script>

<template>
  <div class="comment-manage">
    <h3>评论管理</h3>

    <!-- 搜索筛选 -->
    <div class="filter-bar">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索评论内容"
        clearable
        style="width: 220px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="filters.status" placeholder="状态筛选" clearable style="width: 140px">
        <el-option label="正常" value="ACTIVE" />
        <el-option label="已屏蔽" value="BLOCKED" />
        <el-option label="已删除" value="DELETED" />
      </el-select>
      <el-input
        v-model="filters.postId"
        placeholder="帖子ID"
        clearable
        style="width: 120px"
      />
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
      <el-button size="small" type="success" @click="handleBatchAction('ACTIVE')">批量恢复</el-button>
      <el-button size="small" type="danger" @click="handleBatchAction('DELETED')">批量删除</el-button>
    </div>

    <!-- 评论表格 -->
    <el-table
      v-loading="loading"
      :data="commentList"
      border
      stripe
      style="width: 100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column prop="content" label="内容" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ truncateContent(row.content) }}</template>
      </el-table-column>
      <el-table-column prop="boardName" label="所属吧" width="100" show-overflow-tooltip />
      <el-table-column prop="authorName" label="作者" width="100" />
      <el-table-column prop="postTitle" label="所属帖子" min-width="140" show-overflow-tooltip />
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
          <el-button size="small" text type="warning" @click="router.push('/post/' + row.postId)">跳转到帖</el-button>
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

    <!-- 评论详情弹窗 -->
    <el-dialog v-model="detailVisible" title="评论详情" width="550px">
      <template v-if="detailComment">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(detailComment.status)" size="small">
              {{ statusLabel(detailComment.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="是否采纳">{{ detailComment.isAccepted === 1 ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="发布时间">{{ formatTime(detailComment.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="更新时间">{{ formatTime(detailComment.updatedAt) }}</el-descriptions-item>
          <el-descriptions-item label="内容" :span="2">
            <div class="content-preview">{{ detailComment.content }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.comment-manage h3 {
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
