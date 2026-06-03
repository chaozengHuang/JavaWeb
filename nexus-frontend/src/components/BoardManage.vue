<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdmins, getMembers, addAdmin, removeAdmin, muteUser, unmuteUser, updateBoard, deleteBoard, uploadBoardAvatar, getTrashPosts, recoverPost, getPostCommentsManage, deleteCommentManage, recoverComment, getTrashComments } from '@/api/board'
import { useRouter } from 'vue-router'

const props = defineProps({
  boardId: { type: Number, required: true },
  boardDetail: { type: Object, default: null },
  currentUserRole: { type: String, default: null },
})
const emit = defineEmits(['close', 'boardDeleted', 'refresh'])
const router = useRouter()

const activeTab = ref('info')
const adminList = ref([])
const memberList = ref([])
const memberTotal = ref(0)
const memberPage = ref(1)
const adminLoading = ref(false)
const memberLoading = ref(false)
const descriptionInput = ref('')
const descSaving = ref(false)
const avatarUploading = ref(false)

const isOwner = computed(() => props.currentUserRole === 'OWNER')

// ==================== 回收站 ====================
const trashPosts = ref([])
const trashLoading = ref(false)
const trashComments = ref([])
const trashCommentsLoading = ref(false)
const currentPostComments = ref([])
const commentDialogVisible = ref(false)
const currentPostId = ref(null)

const loadTrash = async () => {
  trashLoading.value = true
  try {
    const res = await getTrashPosts(props.boardId)
    trashPosts.value = res.data?.records || []
  } catch { trashPosts.value = [] }
  finally { trashLoading.value = false }
}

const loadTrashComments = async () => {
  trashCommentsLoading.value = true
  try {
    const res = await getTrashComments(props.boardId)
    trashComments.value = res.data || []
  } catch { trashComments.value = [] }
  finally { trashCommentsLoading.value = false }
}

const handleRecoverPost = async (postId) => {
  try {
    await recoverPost(props.boardId, postId)
    ElMessage.success('已恢复帖子')
    loadTrash()
    emit('refresh')
  } catch (err) { ElMessage.error(err.message || '恢复失败') }
}

const handleRecoverTrashComment = async (commentId) => {
  try {
    await recoverComment(props.boardId, commentId)
    ElMessage.success('已恢复评论')
    loadTrashComments()
    emit('refresh')
  } catch (err) { ElMessage.error(err.message || '恢复失败') }
}

const openCommentManage = async (postId) => {
  currentPostId.value = postId
  try {
    const res = await getPostCommentsManage(props.boardId, postId)
    currentPostComments.value = res.data || []
    commentDialogVisible.value = true
  } catch (err) { ElMessage.error(err.message || '获取评论失败') }
}

const handleDeleteComment = async (commentId) => {
  try {
    await deleteCommentManage(props.boardId, commentId)
    ElMessage.success('已删除评论')
    openCommentManage(currentPostId.value)
    loadTrashComments()
    emit('refresh')
  } catch (err) { ElMessage.error(err.message || '删除失败') }
}

const handleRecoverComment = async (commentId) => {
  try {
    await recoverComment(props.boardId, commentId)
    ElMessage.success('已恢复评论')
    openCommentManage(currentPostId.value)
    loadTrashComments()
  } catch (err) { ElMessage.error(err.message || '恢复失败') }
}

const loadAdmins = async () => {
  adminLoading.value = true
  try {
    const res = await getAdmins(props.boardId)
    adminList.value = res.data || []
  } catch { adminList.value = [] }
  finally { adminLoading.value = false }
}

const loadMembers = async (page = 1) => {
  memberLoading.value = true
  memberPage.value = page
  try {
    const res = await getMembers(props.boardId, '', page, 20)
    memberList.value = res.data?.records || []
    memberTotal.value = res.data?.total || 0
  } catch { memberList.value = [] }
  finally { memberLoading.value = false }
}

const handleAddAdmin = async (userId) => {
  try {
    await addAdmin(props.boardId, userId)
    ElMessage.success('已任命为管理员')
    loadAdmins()
    loadMembers(memberPage.value)
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

const handleRemoveAdmin = async (userId) => {
  try {
    await removeAdmin(props.boardId, userId)
    ElMessage.success('已移除管理员')
    loadAdmins()
    loadMembers(memberPage.value)
  } catch (err) {
    ElMessage.error(err.message || '操作失败')
  }
}

const handleMuteUser = async (userId) => {
  try {
    await muteUser(props.boardId, userId)
    ElMessage.success('已禁言用户')
    loadMembers(memberPage.value)
  } catch (err) {
    ElMessage.error(err.message || '禁言失败')
  }
}

const handleUnmuteUser = async (userId) => {
  try {
    await unmuteUser(props.boardId, userId)
    ElMessage.success('已取消禁言')
    loadMembers(memberPage.value)
  } catch (err) {
    ElMessage.error(err.message || '取消禁言失败')
  }
}

const handleUpdateDesc = async () => {
  descSaving.value = true
  try {
    await updateBoard(props.boardId, descriptionInput.value)
    ElMessage.success('吧描述已更新')
  } catch (err) {
    ElMessage.error(err.message || '更新失败')
  } finally {
    descSaving.value = false
  }
}

const handleBoardAvatarChange = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.warning('仅支持 JPG/PNG/GIF/WebP 格式')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 5MB')
    return
  }
  avatarUploading.value = true
  try {
    await uploadBoardAvatar(props.boardId, file)
    ElMessage.success('吧头像已更新')
    emit('refresh')
  } catch (err) {
    ElMessage.error(err.message || '上传失败')
  } finally {
    avatarUploading.value = false
  }
}

const handleDeleteBoard = async () => {
  try {
    await ElMessageBox.confirm('确定删除该贴吧吗？此操作不可撤销！', '危险操作', {
      confirmButtonText: '确认删除',
      cancelButtonText: '取消',
      type: 'error',
    })
    await deleteBoard(props.boardId)
    ElMessage.success('贴吧已删除')
    emit('boardDeleted')
    router.push('/forum')
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error(err.message || '删除失败')
    }
  }
}

const currentUserId = computed(() => {
  const stored = localStorage.getItem('user')
  if (stored) {
    try { return JSON.parse(stored).user?.id } catch { return null }
  }
  return null
})

const init = () => {
  if (activeTab.value === 'admins') loadAdmins()
  if (activeTab.value === 'members') loadMembers(1)
  if (activeTab.value === 'trashPosts') loadTrash()
  if (activeTab.value === 'trashComments') loadTrashComments()
  if (activeTab.value === 'info') {
    descriptionInput.value = props.boardDetail?.description || ''
  }
}

defineExpose({ init })

watch(activeTab, (tab) => {
  if (tab === 'admins') loadAdmins()
  if (tab === 'members') loadMembers(1)
  if (tab === 'trashPosts') loadTrash()
  if (tab === 'trashComments') loadTrashComments()
  if (tab === 'info') {
    descriptionInput.value = props.boardDetail?.description || ''
  }
}, { immediate: true })
</script>

<template>
  <el-dialog
    title="吧管理"
    width="650px"
    @opened="init"
    @update:model-value="(val) => !val && emit('close')"
    model-value
  >
    <el-tabs v-model="activeTab">
      <el-tab-pane label="吧信息" name="info">
        <el-form label-width="80px">
          <el-form-item label="吧头像">
            <div v-loading="avatarUploading" style="display:flex;align-items:center;gap:12px;">
              <el-avatar :size="64" :src="boardDetail?.avatar ? 'http://localhost:8081' + boardDetail.avatar : ''" shape="square">
                {{ boardDetail?.name?.charAt(0) || '吧' }}
              </el-avatar>
              <label v-if="isOwner" style="cursor:pointer;color:#409eff;font-size:13px;">
                <input type="file" accept="image/*" style="display:none;" @change="handleBoardAvatarChange" />
                更换头像
              </label>
            </div>
          </el-form-item>
          <el-form-item label="吧名称">
            <el-input :model-value="boardDetail?.name" disabled />
          </el-form-item>
          <el-form-item label="吧描述">
            <el-input
              v-model="descriptionInput"
              type="textarea"
              :rows="3"
              placeholder="请输入吧描述"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :loading="descSaving" @click="handleUpdateDesc">保存描述</el-button>
            <el-button v-if="isOwner" type="danger" style="margin-left:12px;" @click="handleDeleteBoard">删除贴吧</el-button>
          </el-form-item>
        </el-form>
      </el-tab-pane>

      <el-tab-pane label="管理员" name="admins" v-if="isOwner">
        <p style="color:#909399;font-size:13px;">管理员拥有管理帖子和禁言用户的权限</p>
        <el-table :data="adminList" stripe v-loading="adminLoading">
          <el-table-column prop="userId" label="用户ID" width="80" />
          <el-table-column prop="username" label="用户名" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button type="danger" size="small" text @click="handleRemoveAdmin(row.userId)">移除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="adminList.length === 0" style="text-align:center;padding:20px;color:#909399;">
          暂无管理员
        </div>
      </el-tab-pane>

      <el-tab-pane label="成员管理" name="members">
        <el-table :data="memberList" stripe v-loading="memberLoading">
          <el-table-column prop="userId" label="用户ID" width="80" />
          <el-table-column prop="username" label="用户名" width="150" />
          <el-table-column prop="boardRole" label="角色" width="100">
            <template #default="{ row }">
              <el-tag v-if="row.boardRole === 'OWNER'" type="danger" size="small">吧主</el-tag>
              <el-tag v-else-if="row.boardRole === 'ADMIN'" type="warning" size="small">管理员</el-tag>
              <el-tag v-else-if="row.boardRole === 'MUTED'" type="info" size="small">已禁言</el-tag>
              <el-tag v-else type="default" size="small">成员</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="200">
            <template #default="{ row }">
              <template v-if="row.userId !== currentUserId">
                <el-button
                  v-if="isOwner && row.boardRole === 'MEMBER'"
                  type="warning"
                  size="small"
                  text
                  @click="handleAddAdmin(row.userId)"
                >任命管理</el-button>
                <el-button
                  v-if="row.boardRole === 'MEMBER'"
                  type="danger"
                  size="small"
                  text
                  @click="handleMuteUser(row.userId)"
                >禁言</el-button>
                <el-button
                  v-if="row.boardRole === 'MUTED'"
                  type="success"
                  size="small"
                  text
                  @click="handleUnmuteUser(row.userId)"
                >取消禁言</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination-wrapper" v-if="memberTotal > 20">
          <el-pagination
            :current-page="memberPage"
            :page-size="20"
            :total="memberTotal"
            layout="prev, pager, next"
            @current-change="loadMembers"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="帖子回收站" name="trashPosts">
        <div v-loading="trashLoading">
          <div v-if="trashPosts.length === 0" style="text-align:center;padding:20px;color:#909399;">没有被隐藏或删除的帖子</div>
          <el-table :data="trashPosts" stripe v-else>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
            <el-table-column prop="status" label="状态" width="80">
              <template #default="{ row }">
                <el-tag v-if="row.status === 'HIDDEN'" size="small" type="warning">隐藏</el-tag>
                <el-tag v-else size="small" type="danger">删除</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="140">
              <template #default="{ row }">
                <el-button type="primary" size="small" text @click="handleRecoverPost(row.id)">恢复</el-button>
                <el-button size="small" text @click="openCommentManage(row.id)">管理评论</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>

      <el-tab-pane label="评论回收站" name="trashComments">
        <div v-loading="trashCommentsLoading">
          <div v-if="trashComments.length === 0" style="text-align:center;padding:20px;color:#909399;">没有被删除的评论</div>
          <el-table :data="trashComments" stripe v-else>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="content" label="评论内容" min-width="180" show-overflow-tooltip />
            <el-table-column prop="authorName" label="评论人" width="100" />
            <el-table-column prop="postTitle" label="所属帖子" width="140" show-overflow-tooltip />
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <el-button type="primary" size="small" text @click="handleRecoverTrashComment(row.id)">恢复</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 评论管理弹窗 -->
    <el-dialog v-model="commentDialogVisible" title="帖子评论管理" width="550px" append-to-body>
      <el-table :data="currentPostComments" stripe max-height="350">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="content" label="内容" />
        <el-table-column prop="status" label="状态" width="70">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'DELETED'" size="small" type="danger">删除</el-tag>
            <el-tag v-else size="small" type="default">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'DELETED'" type="danger" size="small" text @click="handleDeleteComment(row.id)">删除</el-button>
            <el-button v-else type="primary" size="small" text @click="handleRecoverComment(row.id)">恢复</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div v-if="currentPostComments.length === 0" style="text-align:center;padding:20px;color:#909399;">暂无评论</div>
      <template #footer>
        <el-button @click="commentDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
    <template #footer>
      <el-button @click="emit('close')">关闭</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 16px 0 0;
}
</style>
