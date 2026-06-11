<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getBoardList, createBoard } from '@/api/board'
import { ElMessage } from 'element-plus'

const router = useRouter()
const boards = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const createLoading = ref(false)

const createForm = ref({
  name: '',
  description: '',
})

const fetchBoards = async () => {
  loading.value = true
  try {
    const res = await getBoardList()
    boards.value = res.data || []
  } catch (error) {
    ElMessage({ type: 'error', message: error.message || '获取贴吧列表失败' })
  } finally {
    loading.value = false
  }
}

const enterBoard = (board) => {
  router.push(`/forum/${board.id}`)
}

const openCreateDialog = () => {
  createForm.value = { name: '', description: '' }
  dialogVisible.value = true
}

const handleCreate = async () => {
  if (!createForm.value.name) {
    ElMessage.warning('请输入贴吧名称')
    return
  }
  createLoading.value = true
  try {
    // creatorId 从 localStorage 获取当前登录用户
    // localStorage 结构: {token, user}
    const stored = localStorage.getItem('user')
    const resp = stored ? JSON.parse(stored) : null
    const user = resp?.user
    if (!user || !user.id) {
      ElMessage.error('请先登录')
      return
    }
    await createBoard(createForm.value.name, createForm.value.description, user.id)
    ElMessage.success('创建成功')
    dialogVisible.value = false
    fetchBoards()
  } catch (error) {
    ElMessage({ type: 'error', message: error.message || '创建失败' })
  } finally {
    createLoading.value = false
  }
}

onMounted(() => {
  fetchBoards()
})
</script>

<template>
  <div class="forum-container">
    <div class="forum-header">
      <div class="header-left">
        <h2>贴吧列表</h2>
        <span class="board-count">共 {{ boards.length }} 个贴吧</span>
      </div>
      <div class="header-right">
        <el-button size="default" @click="router.push('/profile')">
          <svg t="User" width="18" height="18" viewBox="0 0 24 24" fill="none" style="margin-right:4px">
            <circle cx="12" cy="8" r="4" stroke="currentColor" stroke-width="1.5"/>
            <path d="M4 20c0-4 3.6-7 8-7s8 3 8 7" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          个人中心
        </el-button>
        <el-button type="primary" size="default" @click="openCreateDialog">
          创建贴吧
        </el-button>
      </div>
    </div>

    <div v-loading="loading" class="board-grid">
      <div
        v-for="board in boards"
        :key="board.id"
        class="board-card"
        @click="enterBoard(board)"
      >
        <div class="board-icon">
          <el-avatar
            :size="48"
            shape="square"
            :src="board.avatar ? '' + board.avatar : ''"
          >
            {{ board.name?.charAt(0) }}
          </el-avatar>
        </div>
        <div class="board-info">
          <h3 class="board-name">{{ board.name }}</h3>
          <p class="board-desc">{{ board.description || '暂无描述' }}</p>
        </div>
        <div class="board-arrow">
          <svg t="Arrow" width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M9 6l6 6-6 6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </div>
      </div>
    </div>

    <el-empty v-if="!loading && boards.length === 0" description="暂无贴吧，去创建一个吧" />

    <!-- 创建贴吧弹窗 -->
    <el-dialog v-model="dialogVisible" title="创建贴吧" width="450px">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="贴吧名称">
          <el-input v-model="createForm.name" placeholder="请输入贴吧名称" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="贴吧描述">
          <el-input
            v-model="createForm.description"
            type="textarea"
            placeholder="请输入贴吧描述（选填）"
            :rows="3"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.forum-container {
  max-width: 1200px;
  margin: 0 auto;
}

.forum-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.forum-header h2 {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.board-count {
  color: #909399;
  font-size: 14px;
}

.board-header :deep(.el-button) {
  margin-left: auto;
}

.board-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

.board-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid #e4e7ed;
}

.board-card:hover {
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.15);
  border-color: #409eff;
  transform: translateY(-2px);
}

.board-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.board-info {
  flex: 1;
  min-width: 0;
}

.board-name {
  margin: 0 0 4px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.board-desc {
  margin: 0;
  font-size: 13px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.board-arrow {
  color: #c0c4cc;
  transition: color 0.3s;
}

.board-card:hover .board-arrow {
  color: #409eff;
}

:deep(.board-icon .el-avatar) {
  background: #409eff;
  color: #fff;
  font-weight: 600;
  font-size: 19px;
}
</style>