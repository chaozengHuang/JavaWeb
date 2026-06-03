<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getBoardDetail, hidePost, showPost, deleteBoardPost, getMyRole, joinBoard, leaveBoard, getLeaderboard } from '@/api/board'
import { getPostList, createPost, setPin, setFeature } from '@/api/post'
import { ElMessage, ElMessageBox } from 'element-plus'
import BoardManage from '@/components/BoardManage.vue'

const route = useRoute()
const router = useRouter()
const boards = ref([])
const currentBoard = ref(null)
const boardDetail = ref(null)
const postLoading = ref(false)
const dialogVisible = ref(false)
const createLoading = ref(false)
const manageDialogVisible = ref(false)
const myBoardRole = ref(null)
const leaderboardVisible = ref(false)
const leaderboard = ref([])
const joinLoading = ref(false)

const boardId = computed(() => route.params.boardId)

const isSysAdmin = computed(() => {
  const stored = localStorage.getItem('user')
  if (stored) {
    try { return JSON.parse(stored).user?.globalRole === 'SYS_ADMIN' } catch { return false }
  }
  return false
})
const isOwner = computed(() => myBoardRole.value === 'OWNER' || isSysAdmin.value)
const isManager = computed(() => myBoardRole.value === 'OWNER' || myBoardRole.value === 'ADMIN' || isSysAdmin.value)

const pagination = ref({
  current: 1,
  size: 20,
  total: 0,
})

const posts = ref([])

const createForm = ref({
  title: '',
  content: '',
  type: 'NORMAL',
  rewardPoints: 0,
})

const currentUserId = computed(() => {
  const stored = localStorage.getItem('user')
  if (stored) {
    try { return JSON.parse(stored).user?.id } catch { return null }
  }
  return null
})

const fetchBoardDetail = async () => {
  const bid = Number(boardId.value)
  if (!bid || isNaN(bid)) {
    router.push('/forum')
    return
  }
  try {
    const res = await getBoardDetail(bid)
    boardDetail.value = res.data || null
    currentBoard.value = boardDetail.value?.board || null
    myBoardRole.value = boardDetail.value?.currentUserRole || null
    if (!currentBoard.value) {
      ElMessage.warning('贴吧不存在')
      router.push('/forum')
    }
  } catch (error) {
    ElMessage({ type: 'error', message: error.message || '获取贴吧信息失败' })
    router.push('/forum')
  }
}

const fetchMyRole = async () => {
  const bid = Number(boardId.value)
  if (!bid || isNaN(bid)) return
  try {
    const res = await getMyRole(bid)
    myBoardRole.value = res.data?.role || null
  } catch { myBoardRole.value = null }
}

const fetchPosts = async () => {
  const bid = Number(boardId.value)
  if (!bid || isNaN(bid)) return
  postLoading.value = true
  try {
    const res = await getPostList({
      boardId: bid,
      page: pagination.value.current,
      size: pagination.value.size,
    })
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
    fetchBoardDetail()
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

const goToUserProfile = (authorId) => {
  if (!authorId) return
  if (currentUserId.value && Number(authorId) === currentUserId.value) {
    router.push('/profile')
  } else {
    router.push(`/user/${authorId}`)
  }
}

// 吧管理操作
const handleHidePost = async (postId) => {
  try {
    await hidePost(Number(boardId.value), postId)
    ElMessage.success('已隐藏帖子')
    fetchPosts()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

const handleShowPost = async (postId) => {
  try {
    await showPost(Number(boardId.value), postId)
    ElMessage.success('已取消隐藏')
    fetchPosts()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

const handleDeletePost = async (postId) => {
  try {
    await ElMessageBox.confirm('确定删除这条帖子吗？', '提示', { type: 'warning' })
    await deleteBoardPost(Number(boardId.value), postId)
    ElMessage.success('已删除帖子')
    fetchPosts()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '操作失败')
    }
  }
}

const handlePin = async (postId, currentPinned) => {
  try {
    await setPin(postId, currentPinned ? 0 : 1)
    ElMessage.success(currentPinned ? '已取消置顶' : '已置顶')
    fetchPosts()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

const handleFeature = async (postId, currentFeatured) => {
  try {
    await setFeature(postId, currentFeatured ? 0 : 1)
    ElMessage.success(currentFeatured ? '已取消加精' : '已加精')
    fetchPosts()
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

const openManageDialog = () => {
  manageDialogVisible.value = true
}

const onManageClose = () => {
  manageDialogVisible.value = false
  fetchBoardDetail()
  fetchMyRole()
}

const onBoardDeleted = () => {
  manageDialogVisible.value = false
}

const handleJoin = async () => {
  joinLoading.value = true
  try {
    await joinBoard(Number(boardId.value))
    ElMessage.success('已加入贴吧')
    fetchBoardDetail()
  } catch (err) {
    ElMessage.error(err.message || '加入失败')
  } finally {
    joinLoading.value = false
  }
}

const handleLeave = async () => {
  try {
    await ElMessageBox.confirm('确定退出该吧吗？', '提示', { type: 'warning' })
    await leaveBoard(Number(boardId.value))
    ElMessage.success('已退出贴吧')
    fetchBoardDetail()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '退出失败')
    }
  }
}

const fetchLeaderboard = async () => {
  try {
    const res = await getLeaderboard(Number(boardId.value))
    leaderboard.value = res.data || []
    leaderboardVisible.value = true
  } catch (err) {
    ElMessage.error(err.message || '获取排行榜失败')
  }
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
  fetchBoardDetail()
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
    </div>

    <!-- 吧信息卡片 -->
    <div v-if="boardDetail" class="board-info-card">
      <div class="board-info-header">
        <div style="display:flex;align-items:center;gap:14px;">
          <el-avatar :size="56" :src="boardDetail.board?.avatar ? 'http://localhost:8081' + boardDetail.board.avatar : ''" shape="square">
            {{ boardDetail.board?.name?.charAt(0) || '吧' }}
          </el-avatar>
          <h2 style="margin:0;">{{ boardDetail.board?.name }}</h2>
        </div>
        <div class="board-info-actions">
          <el-button v-if="!myBoardRole && !isSysAdmin" type="success" size="small" :loading="joinLoading" @click="handleJoin">加入贴吧</el-button>
          <el-button v-if="myBoardRole && !isOwner && !isSysAdmin" type="warning" size="small" @click="handleLeave">退出贴吧</el-button>
          <el-button v-if="isOwner" type="primary" size="small" @click="openManageDialog">管理吧</el-button>
          <el-button size="small" @click="fetchLeaderboard">🏆 活跃榜</el-button>
        </div>
      </div>
      <p class="board-desc">{{ boardDetail.board?.description || '暂无描述' }}</p>
      <div class="board-info-meta">
        <span>吧主：
          <a class="link-user" @click="goToUserProfile(boardDetail.owner?.userId)">
            {{ boardDetail.owner?.username || '未知' }}
          </a>
        </span>
        <span v-if="boardDetail.admins?.length">
          管理员：
          <a
            v-for="admin in boardDetail.admins"
            :key="admin.userId"
            class="link-user"
            @click="goToUserProfile(admin.userId)"
          >{{ admin.username }} </a>
        </span>
        <span>帖子：{{ boardDetail.postCount }}</span>
        <span>成员：{{ boardDetail.memberCount }}</span>
        <span v-if="isSysAdmin" class="role-badge sysadmin">系统管理员</span>
        <span v-if="myBoardRole === 'OWNER'" class="role-badge owner">吧主</span>
        <span v-else-if="myBoardRole === 'ADMIN'" class="role-badge admin">管理员</span>
        <span v-else-if="myBoardRole === 'MUTED'" class="role-badge muted">已禁言</span>
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
              <span v-if="post.type === 'REWARD' && post.rewardPoints > 0" class="badge badge-reward">悬赏 {{ post.rewardPoints }}分</span>
              <span v-if="post.type === 'REWARD' && post.rewardPoints === 0" class="badge badge-resolved">已采纳</span>
            </div>
            <h3 class="post-title">{{ post.title }}</h3>
            <div class="post-meta">
              <span class="author" style="cursor:pointer;color:#409eff" @click.stop="goToUserProfile(post.authorId)">{{ post.authorUsername || '匿名' }}</span>
              <span class="time">{{ formatDate(post.createdAt) }}</span>
            </div>
          </div>
          <div class="post-right">
            <div class="post-stats">
              <span class="stat">
                <svg t="Comment" width="14" height="14" viewBox="0 0 24 24" fill="none">
                  <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2v10z" stroke="currentColor" stroke-width="1.5"/>
                </svg>
                {{ post.commentCount || 0 }}
              </span>
            </div>
            <!-- 吧管理按钮 -->
            <div v-if="isManager" class="post-manage-actions" @click.stop>
              <el-dropdown trigger="click">
                <el-button size="small" text>
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                    <circle cx="12" cy="5" r="2" fill="currentColor"/>
                    <circle cx="12" cy="12" r="2" fill="currentColor"/>
                    <circle cx="12" cy="19" r="2" fill="currentColor"/>
                  </svg>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handlePin(post.id, post.isPinned)">
                      {{ post.isPinned ? '取消置顶' : '置顶' }}
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleFeature(post.id, post.isFeatured)">
                      {{ post.isFeatured ? '取消加精' : '加精' }}
                    </el-dropdown-item>
                    <el-dropdown-item divided @click="handleHidePost(post.id)" v-if="post.status === 'ACTIVE'">隐藏帖子</el-dropdown-item>
                    <el-dropdown-item @click="handleShowPost(post.id)" v-if="post.status === 'HIDDEN'">取消隐藏</el-dropdown-item>
                    <el-dropdown-item divided @click="handleDeletePost(post.id)" style="color:#f56c6c">删除帖子</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
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

    <!-- 吧管理弹窗 -->
    <BoardManage
      v-if="manageDialogVisible"
      :board-id="Number(boardId)"
      :board-detail="boardDetail?.board"
      :current-user-role="myBoardRole"
      @close="onManageClose"
      @board-deleted="onBoardDeleted"
      @refresh="fetchBoardDetail(); fetchPosts()"
    />

    <!-- 活跃度排行榜 -->
    <el-dialog v-model="leaderboardVisible" title="🏆 活跃度排行榜" width="480px">
      <el-table :data="leaderboard" stripe max-height="400">
        <el-table-column prop="rank" label="排名" width="60">
          <template #default="{ row }">
            <span v-if="row.rank === 1">🥇</span>
            <span v-else-if="row.rank === 2">🥈</span>
            <span v-else-if="row.rank === 3">🥉</span>
            <span v-else>{{ row.rank }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="用户">
          <template #default="{ row }">
            <span style="cursor:pointer;color:#409eff" @click="goToUserProfile(row.userId)">{{ row.username }}</span>
            <el-tag v-if="row.boardRole === 'OWNER'" size="small" type="danger" style="margin-left:6px">吧主</el-tag>
            <el-tag v-else-if="row.boardRole === 'ADMIN'" size="small" type="warning" style="margin-left:6px">管理</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="activityPoints" label="活跃度" width="80">
          <template #default="{ row }">
            <strong>{{ row.activityPoints }}</strong>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="leaderboard.length === 0" style="text-align:center;padding:30px;color:#909399;">暂无数据</div>
    </el-dialog>
  </div>
</template>

<style scoped>
.board-container {
  max-width: 1200px;
  margin: 0 auto;
}

.board-header {
  margin-bottom: 16px;
}

.board-info-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 16px;
  border: 1px solid #e4e7ed;
}

.board-info-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.board-info-header h2 {
  margin: 0;
  font-size: 22px;
  color: #303133;
}

.board-desc {
  margin: 10px 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.6;
}

.board-info-meta {
  display: flex;
  gap: 24px;
  font-size: 13px;
  color: #909399;
  flex-wrap: wrap;
  align-items: center;
}

.link-user {
  color: #409eff;
  cursor: pointer;
}

.link-user:hover {
  text-decoration: underline;
}

.role-badge {
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 500;
}

.role-badge.owner { background: #fef0f0; color: #f56c6c; }
.role-badge.admin { background: #fdf6ec; color: #e6a23c; }
.role-badge.sysadmin { background: #f0f0ff; color: #7c3aed; }
.role-badge.muted { background: #f4f4f5; color: #909399; }

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

.badge-pin { background: #e6f0ff; color: #409eff; }
.badge-global { background: #fff3e6; color: #ff9900; }
.badge-featured { background: #fff1e6; color: #ff6600; }
.badge-reward { background: #e6fff0; color: #52c41a; }
.badge-resolved { background: #f0f0f0; color: #909399; }

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

.post-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
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

.post-manage-actions {
  margin-left: 8px;
}

.pagination {
  padding: 16px;
  justify-content: center;
}
</style>
