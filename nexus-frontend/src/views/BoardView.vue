<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBoardList } from '@/api/board'
import { getPostList, createPost } from '@/api/post'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const boards = ref([])
const currentBoard = ref(null)
const postLoading = ref(false)
const dialogVisible = ref(false)
const createLoading = ref(false)

const boardId = computed(() => route.params.boardId)

// 分页参数
const pagination = ref({
  current: 1,
  size: 20,
  total: 0,
})

// 帖子列表
const posts = ref([])

// 创建帖子表单
const createForm = ref({
  title: '',
  content: '',
  type: 'NORMAL',
  rewardPoints: 0,
})

const fetchBoards = async () => {
  try {
    const res = await getBoardList()
    boards.value = res || []
    currentBoard.value = boards.value.find(b => b.id === Number(boardId.value))
    if (!currentBoard.value) {
      ElMessage.warning('贴吧不存在')
      router.push('/forum')
    }
  } catch (error) {
    ElMessage({ type: 'error', message: error.message || '获取贴吧信息失败' })
  }
}

const fetchPosts = async () => {
  postLoading.value = true
  try {
    const res = await getPostList({
      boardId: boardId.value,
      page: pagination.value.current,
      size: pagination.value.size,
    })
    // PostController 返回 Result<IPage>
    const pageData = (res && res.data && res.data.records) ? res.data : { records: [], total: 0 }
    posts.value = pageData.records || []
    pagination.value.total = pageData.total || 0
  } catch (error) {
    ElMessage({ type: 'error', message: error.message || '获取帖子列表失败' })
  } finally {
    postLoading.value = false
  }
}

const goBack = () => {
  router.push('/forum')
}

const openCreateDialog = () => {
  createForm.value = { title: '', content: '', type: 'NORMAL', rewardPoints: 0 }
  dialogVisible.value = true
}

const handleCreate = async () => {
  if (!createForm.value.title) {
    ElMessage.warning('请输入帖子标题')
    return
  }
  if (!createForm.value.content) {
    ElMessage.warning('请输入帖子内容')
    return
  }
  createLoading.value = true
  try {
    const stored = localStorage.getItem('user')
    const resp = stored ? JSON.parse(stored) : null
    const user = resp?.user
    if (!user || !user.id) {
      ElMessage.error('请先登录')
      return
    }
    await createPost({
      boardId: Number(boardId.value),
      authorId: user.id,
      title: createForm.value.title,
      content: createForm.value.content,
      type: createForm.value.type,
      rewardPoints: createForm.value.type === 'REWARD' ? createForm.value.rewardPoints : 0,
    })
    ElMessage.success('发帖成功')
    dialogVisible.value = false
    fetchPosts()
  } catch (error) {
    ElMessage({ type: 'error', message: error.message || '发帖失败' })
  } finally {
    createLoading.value = false
  }
}

const handlePageChange = (page) => {
  pagination.value.current = page
  fetchPosts()
}

const viewPost = (post) => {
  router.push(`/post/${post.id}`)
}

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  return d.toLocaleDateString()
}

onMounted(() => {
  fetchBoards()
  fetchPosts()
})
</script>

<template>
  <div class="board-container">
    <div class="board-header">
      <el-button text @click="goBack">
        <svg t="Back" width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path d="M15 18l-6-6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        返回贴吧列表
      </el-button>
      <div v-if="currentBoard" class="board-title">
        <h2>{{ currentBoard.name }}</h2>
        <p class="board-desc">{{ currentBoard.description || '暂无描述' }}</p>
      </div>
    </div>

    <div class="board-actions">
      <el-button type="primary" @click="openCreateDialog">
        <svg t="Edit" width="16" height="16" viewBox="0 0 24 24" fill="none" style="margin-right:4px">
          <path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
        </svg>
        发帖
      </el-button>
    </div>

    <div v-loading="postLoading" class="post-list">
      <div v-if="posts.length === 0" class="empty-state">
        <svg t="Empty" width="60" height="60" viewBox="0 0 24 24" fill="none">
          <rect x="3" y="3" width="18" height="18" rx="2" stroke="#dcdfe6" stroke-width="1.5"/>
          <path d="M8 12h8M8 8h8M8 16h4" stroke="#dcdfe6" stroke-width="1.5" stroke-linecap="round"/>
        </svg>
        <p>暂无帖子，快来发表第一篇吧</p>
      </div>

      <div v-else class="post-items">
        <div
          v-for="post in posts"
          :key="post.id"
          class="post-item"
          @click="viewPost(post)"
        >
          <div class="post-left">
            <div class="post-badges">
              <span v-if="post.isPinned" class="badge badge-pin">置顶</span>
              <span v-if="post.isGlobalPinned" class="badge badge-global">全局置顶</span>
              <span v-if="post.isFeatured" class="badge badge-featured">精华</span>
              <span v-if="post.type === 'REWARD'" class="badge badge-reward">悬赏</span>
            </div>
            <h3 class="post-title">{{ post.title }}</h3>
            <div class="post-meta">
              <span class="author">{{ post.authorUsername || '匿名' }}</span>
              <span class="time">{{ formatDate(post.createdAt) }}</span>
            </div>
          </div>
          <div class="post-stats">
            <span class="stat">
              <svg t="Comment" width="14" height="14" viewBox="0 0 24 24" fill="none">
                <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2v10z" stroke="currentColor" stroke-width="1.5"/>
              </svg>
              {{ post.commentCount || 0 }}
            </span>
          </div>
        </div>
      </div>

      <el-pagination
        v-if="pagination.total > 0"
        class="pagination"
        :current-page="pagination.current"
        :page-size="pagination.size"
        :total="pagination.total"
        layout="prev, pager, next"
        @current-change="handlePageChange"
      />
    </div>

    <!-- 发帖弹窗 -->
    <el-dialog v-model="dialogVisible" title="发表帖子" width="600px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="帖子标题">
          <el-input v-model="createForm.title" placeholder="请输入标题" maxlength="150" show-word-limit />
        </el-form-item>
        <el-form-item label="帖子类型">
          <el-select v-model="createForm.type" style="width: 100%">
            <el-option label="普通帖" value="NORMAL" />
            <el-option label="悬赏帖" value="REWARD" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="createForm.type === 'REWARD'" label="悬赏积分">
          <el-input-number v-model="createForm.rewardPoints" :min="1" :max="9999" />
        </el-form-item>
        <el-form-item label="帖子内容">
          <el-input
            v-model="createForm.content"
            type="textarea"
            placeholder="请输入内容"
            :rows="6"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">发表</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.board-container {
  max-width: 1200px;
  margin: 0 auto;
}

.board-header {
  margin-bottom: 20px;
}

.board-title h2 {
  margin: 16px 0 4px 0;
  font-size: 22px;
  color: #303133;
}

.board-desc {
  margin: 0;
  font-size: 14px;
  color: #909399;
}

.board-actions {
  margin-bottom: 16px;
}

.post-list {
  background: #fff;
  border-radius: 12px;
  min-height: 400px;
}

.empty-state {
  padding: 80px 20px;
  text-align: center;
  color: #909399;
}

.empty-state p {
  margin: 16px 0 0 0;
}

.post-items {
  padding: 0 16px;
}

.post-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 8px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  transition: background 0.2s;
}

.post-item:hover {
  background: #f9f9f9;
}

.post-item:last-child {
  border-bottom: none;
}

.post-left {
  flex: 1;
  min-width: 0;
}

.post-badges {
  display: flex;
  gap: 6px;
  margin-bottom: 6px;
}

.badge {
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
}

.badge-pin {
  background: #e6f0ff;
  color: #409eff;
}

.badge-global {
  background: #fff3e6;
  color: #ff9900;
}

.badge-featured {
  background: #fff1e6;
  color: #ff6600;
}

.badge-reward {
  background: #e6fff0;
  color: #52c41a;
}

.post-title {
  margin: 0 0 8px 0;
  font-size: 16px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.post-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #909399;
}

.post-stats {
  display: flex;
  gap: 16px;
  color: #909399;
  font-size: 13px;
}

.stat {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pagination {
  padding: 16px;
  justify-content: center;
}
</style>