<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPostDetail, updatePost, deletePost } from '@/api/post'
import { toggleLike, toggleFavorite, isLiked, isFavorited, recordBrowse } from '@/api/profile'
import { getComments, createComment, deleteComment, acceptComment } from '@/api/comment'
import { getMyRole, joinBoard } from '@/api/board'

const route = useRoute()
const router = useRouter()

const postId = computed(() => route.params.postId)
const post = ref(null)
const loading = ref(false)
const commentLoading = ref(false)
const comments = ref([])
const commentInput = ref('')
const commentSubmitting = ref(false)
const replyTarget = ref(null)  // { id, username }
const liked = ref(false)
const favorited = ref(false)
const editDialogVisible = ref(false)
const editForm = ref({ title: '', content: '' })

const currentUser = computed(() => {
  const stored = localStorage.getItem('user')
  if (stored) {
    try {
      return JSON.parse(stored).user || {}
    } catch { return {} }
  }
  return {}
})

const isAuthor = computed(() => {
  if (!post.value || !currentUser.value.id) return false
  return post.value.authorId === currentUser.value.id
})

const isBoardManager = ref(false)

const canManage = computed(() => isAuthor.value || isBoardManager.value)

const BASE_URL = 'http://localhost:8081'

const avatarUrl = computed(() => {
  // Post entity does not have avatar field; author info is limited to username
  return ''
})

const fetchPost = async () => {
  loading.value = true
  try {
    const res = await getPostDetail(postId.value)
    post.value = res.data || null
    // 加载点赞收藏状态
    if (!isAuthor.value) {
      try {
        liked.value = await isLiked(postId.value).then(r => r.data)
      } catch { liked.value = false }
      try {
        favorited.value = await isFavorited(postId.value).then(r => r.data)
      } catch { favorited.value = false }
    }
  } catch (err) {
    ElMessage.error(err.message || '加载帖子失败')
    router.back()
  } finally {
    loading.value = false
  }
}

const fetchComments = async () => {
  commentLoading.value = true
  try {
    const res = await getComments(postId.value)
    comments.value = res.data || []
  } catch {
    comments.value = []
  } finally {
    commentLoading.value = false
  }
}

const handleLike = async () => {
  if (!currentUser.value.id) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await toggleLike(postId.value)
    liked.value = !liked.value
    post.value.likeCount = (post.value.likeCount || 0) + (liked.value ? 1 : -1)
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

const handleFavorite = async () => {
  if (!currentUser.value.id) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    await toggleFavorite(postId.value)
    favorited.value = !favorited.value
    post.value.favoriteCount = (post.value.favoriteCount || 0) + (favorited.value ? 1 : -1)
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

const handleSubmitComment = async () => {
  if (!commentInput.value.trim()) return
  commentSubmitting.value = true
  try {
    const body = { postId: Number(postId.value), content: commentInput.value }
    if (replyTarget.value) {
      body.parentCommentId = replyTarget.value.id
    }
    await createComment(body)
    ElMessage.success(replyTarget.value ? '回复成功' : '评论成功')
    commentInput.value = ''
    replyTarget.value = null
    fetchComments()
    post.value.commentCount = (post.value.commentCount || 0) + 1
  } catch (err) {
    const msg = err.message || '评论失败'
    if (msg.includes('REQUIRE_JOIN')) {
      ElMessageBox.confirm(
        '请先加入该吧后再评论', '提示',
        { confirmButtonText: '立即加入', cancelButtonText: '取消', type: 'warning' }
      ).then(async () => {
        try {
          const boardId = post.value?.boardId
          if (boardId) {
            await joinBoard(boardId)
            ElMessage.success('已加入贴吧，现在可以评论了')
          }
        } catch (e) { ElMessage.error(e.message || '加入失败') }
      }).catch(() => {})
    } else {
      ElMessage.error(msg)
    }
  } finally {
    commentSubmitting.value = false
  }
}

const startReply = (comment) => {
  replyTarget.value = { id: comment.id, username: comment.authorUsername }
  commentInput.value = ''
}

const handleDeleteComment = async (commentId) => {
  try {
    await ElMessageBox.confirm('确定删除这条评论吗？', '提示', { type: 'warning' })
    await deleteComment(commentId)
    ElMessage.success('删除成功')
    fetchComments()
    post.value.commentCount = Math.max(0, (post.value.commentCount || 0) - 1)
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '删除失败')
    }
  }
}

const handleAcceptComment = async (commentId) => {
  try {
    await acceptComment(commentId)
    ElMessage.success('采纳成功，积分已转移')
    fetchPost()
    fetchComments()
    // 通知 App.vue 获取最新积分
    window.dispatchEvent(new CustomEvent('points-updated'))
  } catch (err) {
    ElMessage.error(err.message || '采纳失败')
  }
}

const openEditDialog = () => {
  editForm.value = { title: post.value.title, content: post.value.content }
  editDialogVisible.value = true
}

const handleEdit = async () => {
  if (!editForm.value.title || !editForm.value.content) {
    ElMessage.warning('请填写完整')
    return
  }
  try {
    await updatePost(postId.value, { title: editForm.value.title, content: editForm.value.content })
    ElMessage.success('修改成功')
    editDialogVisible.value = false
    fetchPost()
  } catch (err) {
    ElMessage.error(err.message || '修改失败')
  }
}

const handleDelete = async () => {
  try {
    await ElMessageBox.confirm('确定删除这篇帖子吗？', '提示', { type: 'warning' })
    await deletePost(postId.value)
    ElMessage.success('删除成功')
    router.push('/forum')
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '删除失败')
    }
  }
}

const goBack = () => {
  router.back()
}

const goToUserProfile = (userId) => {
  if (!userId) return
  if (userId === currentUser.value.id) {
    router.push('/profile')
  } else {
    router.push(`/user/${userId}`)
  }
}

const formatDate = (date) => {
  if (!date) return ''
  const d = new Date(date)
  return d.toLocaleString()
}

onMounted(async () => {
  fetchPost()
  fetchComments()
  recordBrowse(postId.value).catch(() => {})
  // 检查吧管理权限
  try {
    const postData = await getPostDetail(postId.value)
    const boardId = postData.data?.boardId
    if (boardId) {
      const roleRes = await getMyRole(boardId)
      const role = roleRes.data?.role
      isBoardManager.value = role === 'OWNER' || role === 'ADMIN'
    }
  } catch { isBoardManager.value = false }
})
</script>

<template>
  <div class="post-detail-container">
    <div class="post-detail-header">
      <el-button text @click="goBack">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
          <path d="M15 18l-6-6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        返回
      </el-button>
    </div>

    <div v-loading="loading" class="post-content">
      <template v-if="post">
        <div class="post-badges">
          <span v-if="post.isPinned" class="badge badge-pin">置顶</span>
          <span v-if="post.isGlobalPinned" class="badge badge-global">全局置顶</span>
          <span v-if="post.isFeatured" class="badge badge-featured">精华</span>
          <span v-if="post.type === 'REWARD' && post.rewardPoints > 0" class="badge badge-reward">悬赏 {{ post.rewardPoints }} 积分</span>
          <span v-if="post.type === 'REWARD' && post.rewardPoints === 0" class="badge badge-resolved">悬赏已采纳</span>
        </div>

        <h1 class="post-title">{{ post.title }}</h1>

        <div class="post-meta">
          <span class="author" style="cursor:pointer;color:#409eff" @click="goToUserProfile(post.authorId)">
            {{ post.authorUsername || '匿名' }}
          </span>
          <span class="time">{{ formatDate(post.createdAt) }}</span>
        </div>

        <div class="post-body">
          {{ post.content }}
        </div>

        <div class="post-actions">
          <div class="action-left">
            <el-button :type="liked ? 'primary' : 'default'" text @click="handleLike">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" style="margin-right:4px">
                <path d="M14 9V5a3 3 0 00-3-3l-4 9v11h11.28a2 2 0 002-1.7l1.38-9a2 2 0 00-2-2.3H14z" :stroke="liked ? '#409eff' : 'currentColor'" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M7 22H4a2 2 0 01-2-2v-7a2 2 0 012-2h3" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              {{ post.likeCount || 0 }}
            </el-button>
            <el-button :type="favorited ? 'primary' : 'default'" text @click="handleFavorite">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" style="margin-right:4px">
                <path d="M12 21l-1.45-1.32C5.43 15.36 2 12.28 2 8.5 2 5.42 4.42 3 7.5 3c1.74 0 3.41.81 4.5 2.09C13.09 3.81 14.76 3 16.5 3 19.58 3 22 5.42 22 8.5c0 3.78-3.43 6.86-8.55 11.18L12 21z" :fill="favorited ? '#409eff' : 'none'" :stroke="favorited ? '#409eff' : 'currentColor'" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              {{ post.favoriteCount || 0 }}
            </el-button>
            <span class="comment-count">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" style="margin-right:4px">
                <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2v10z" stroke="currentColor" stroke-width="1.5"/>
              </svg>
              {{ post.commentCount || 0 }}
            </span>
          </div>
          <div v-if="canManage" class="action-right">
            <el-button type="primary" text @click="openEditDialog">编辑</el-button>
            <el-button type="danger" text @click="handleDelete">删除</el-button>
          </div>
        </div>

        <!-- 编辑弹窗 -->
        <el-dialog v-model="editDialogVisible" title="编辑帖子" width="600px">
          <el-form :model="editForm" label-width="80px">
            <el-form-item label="帖子标题">
              <el-input v-model="editForm.title" placeholder="请输入标题" maxlength="150" show-word-limit />
            </el-form-item>
            <el-form-item label="帖子内容">
              <el-input v-model="editForm.content" type="textarea" :rows="6" placeholder="请输入内容" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="editDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="handleEdit">保存</el-button>
          </template>
        </el-dialog>

        <!-- 评论区 -->
        <div class="comments-section">
          <h3 class="comments-title">评论 ({{ comments.length }})</h3>

          <div class="comment-input-area">
            <div style="flex:1;">
              <div v-if="replyTarget" class="reply-bar">
                回复 <strong>@{{ replyTarget.username }}</strong>
                <el-button type="danger" size="small" text @click="replyTarget = null">取消</el-button>
              </div>
              <el-input
                v-model="commentInput"
                type="textarea"
                :rows="3"
                :placeholder="replyTarget ? '回复 @' + replyTarget.username + '...' : '写下你的评论...'"
                resize="none"
              />
            </div>
            <el-button type="primary" :loading="commentSubmitting" @click="handleSubmitComment">
              {{ replyTarget ? '回复' : '发表评论' }}
            </el-button>
          </div>

          <div v-loading="commentLoading" class="comment-list">
            <div v-if="comments.length === 0" class="comment-empty">暂无评论，快来发表第一楼吧</div>
            <!-- 递归评论组件 -->
            <template v-for="comment in comments" :key="comment.id">
              <div class="comment-item">
                <div class="comment-header">
                  <span class="comment-author" style="cursor:pointer;color:#409eff" @click="goToUserProfile(comment.authorId)">
                    {{ comment.authorUsername || '匿名' }}
                  </span>
                  <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
                  <el-tag v-if="comment.isAccepted === 1" size="small" type="success">已采纳</el-tag>
                </div>
                <div class="comment-body">{{ comment.content }}</div>
                <div class="comment-actions">
                  <el-button size="small" text @click="startReply(comment)">回复</el-button>
                  <template v-if="isAuthor && comment.isAccepted !== 1 && post.type === 'REWARD' && post.rewardPoints > 0">
                    <el-button type="success" size="small" text @click="handleAcceptComment(comment.id)">采纳</el-button>
                  </template>
                  <template v-if="comment.authorId === currentUser.id || currentUser.globalRole === 'SYS_ADMIN'">
                    <el-button type="danger" size="small" text @click="handleDeleteComment(comment.id)">删除</el-button>
                  </template>
                </div>
                <!-- 楼中楼 -->
                <div v-if="comment.children && comment.children.length" class="comment-replies">
                  <div v-for="child in comment.children" :key="child.id" class="reply-item">
                    <div class="comment-header">
                      <span class="comment-author" style="cursor:pointer;color:#409eff" @click="goToUserProfile(child.authorId)">
                        {{ child.authorUsername || '匿名' }}
                      </span>
                      <span v-if="child.parentAuthorUsername" style="font-size:12px;color:#909399;"> 回复 @{{ child.parentAuthorUsername }}</span>
                      <span class="comment-time">{{ formatDate(child.createdAt) }}</span>
                    </div>
                    <div class="comment-body">{{ child.content }}</div>
                    <div class="comment-actions">
                      <el-button size="small" text @click="startReply(comment)">回复</el-button>
                      <template v-if="child.authorId === currentUser.id || currentUser.globalRole === 'SYS_ADMIN'">
                        <el-button type="danger" size="small" text @click="handleDeleteComment(child.id)">删除</el-button>
                      </template>
                    </div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>
      </template>
    </div>
  </div>
</template>

<style scoped>
.post-detail-container {
  max-width: 900px;
  margin: 0 auto;
}

.post-detail-header {
  margin-bottom: 16px;
}

.post-content {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
}

.post-badges {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
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
.badge-resolved { background: #f5f5f5; color: #909399; }

.post-title {
  margin: 0 0 12px 0;
  font-size: 24px;
  font-weight: 600;
  color: #303133;
}

.post-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #909399;
  margin-bottom: 20px;
}

.post-body {
  font-size: 15px;
  line-height: 1.8;
  color: #303133;
  white-space: pre-wrap;
  margin-bottom: 20px;
  min-height: 100px;
}

.post-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-top: 1px solid #f0f0f0;
}

.action-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-right {
  display: flex;
  gap: 8px;
}

.comment-count {
  display: flex;
  align-items: center;
  color: #909399;
  font-size: 14px;
}

.comments-section {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.comments-title {
  margin: 0 0 16px 0;
  font-size: 16px;
  color: #303133;
}

.comment-input-area {
  display: flex;
  gap: 12px;
  align-items: flex-end;
  margin-bottom: 20px;
}

.comment-input-area :deep(.el-textarea__inner) {
  resize: none;
}

.comment-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.comment-empty {
  text-align: center;
  color: #909399;
  padding: 40px 0;
}

.comment-item {
  padding: 16px;
  background: #f9f9f9;
  border-radius: 8px;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.comment-author {
  font-size: 14px;
  font-weight: 500;
}

.comment-time {
  font-size: 12px;
  color: #909399;
}

.comment-body {
  font-size: 14px;
  line-height: 1.6;
  color: #303133;
  margin-bottom: 8px;
  white-space: pre-wrap;
}

.comment-actions {
  display: flex;
  gap: 8px;
}

/* 回复提示条 */
.reply-bar {
  padding: 6px 10px;
  background: #ecf5ff;
  border-radius: 4px 4px 0 0;
  font-size: 13px;
  color: #409eff;
  margin-bottom: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

/* 楼中楼 */
.comment-replies {
  margin-top: 12px;
  padding-left: 24px;
  border-left: 3px solid #e4e7ed;
}

.reply-item {
  padding: 12px;
  background: #fff;
  border-radius: 6px;
  margin-bottom: 8px;
}

.reply-item:last-child {
  margin-bottom: 0;
}
</style>