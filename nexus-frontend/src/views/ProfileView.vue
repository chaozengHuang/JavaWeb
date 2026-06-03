<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getProfile, updateBio, updateProfile, uploadAvatar,
  getFavorites, removeFavorite,
  getLikes, removeLike,
  getHistory, deleteHistory, clearHistory,
} from '@/api/profile'
import { getUserInfoById, getPublicProfile } from '@/api/user'
import { sendFriendRequest, isFriend, deleteFriend } from '@/api/friend'
import { getMyBoards } from '@/api/board'
import { getPostsByUser } from '@/api/post'

const BASE_URL = 'http://localhost:8081'

const route = useRoute()
const router = useRouter()

// ==================== 状态 ====================
const activeTab = ref('info')
const profile = ref(null)
const stats = ref({ favoriteCount: 0, likeCount: 0, postCount: 0 })
const bioInput = ref('')
const bioEditing = ref(false)
const profileEditing = ref(false)
const profileSaving = ref(false)
const editForm = ref({ bio: '', phone: '', email: '', jobNature: '', location: '' })
const avatarUploading = ref(false)

const targetUserId = computed(() => route.params.userId)
const isOtherUser = computed(() => {
  if (!targetUserId.value) return false
  return Number(targetUserId.value) !== currentUser.value.id
})

// 列表
const loading = ref(false)
const listData = ref([])
const listTotal = ref(0)
const listPage = ref(1)
const listPageSize = ref(10)
const friendStatus = ref(false)
const friendLoading = ref(false)
const myBoards = ref([])

// ==================== 计算属性 ====================
const currentUser = computed(() => {
  const stored = localStorage.getItem('user')
  if (stored) {
    try {
      return JSON.parse(stored).user || {}
    } catch { return {} }
  }
  return {}
})

const avatarUrl = computed(() => {
  if (profile.value?.avatar) {
    if (profile.value.avatar.startsWith('http')) return profile.value.avatar
    return BASE_URL + profile.value.avatar
  }
  return ''
})

const listTitle = computed(() => {
  const titles = { favorites: '我的收藏', likes: '我的点赞', history: '浏览历史' }
  return titles[activeTab.value] || ''
})

const pageKey = computed(() => `profile_${activeTab.value}`)

// ==================== 加载逻辑 ====================
const loadProfile = async () => {
  try {
    let res
    if (isOtherUser.value) {
      res = await getPublicProfile(targetUserId.value)
      profile.value = res.data?.user || null
      stats.value = res.data?.stats || { favoriteCount: 0, likeCount: 0, postCount: 0 }
    } else {
      res = await getProfile()
      profile.value = res.data?.user || null
      stats.value = res.data?.stats || { favoriteCount: 0, likeCount: 0, postCount: 0 }
    }
    bioInput.value = profile.value?.bio || ''
  } catch (err) {
    ElMessage({ type: 'error', message: err.message || '加载失败' })
  }
}

const loadList = async (page = 1) => {
  loading.value = true
  listPage.value = page
  try {
    let res
    if (activeTab.value === 'favorites') {
      res = await getFavorites(page, listPageSize.value)
    } else if (activeTab.value === 'likes') {
      res = await getLikes(page, listPageSize.value)
    } else if (activeTab.value === 'history') {
      res = await getHistory(page, listPageSize.value)
    } else if (activeTab.value === 'posts') {
      res = await getPostsByUser(targetUserId.value, page, listPageSize.value)
    } else if (activeTab.value === 'myPosts') {
      res = await getPostsByUser(currentUser.value.id, page, listPageSize.value)
    } else if (activeTab.value === 'myBoards') {
      res = await getMyBoards()
    }
    if (res) {
      if (activeTab.value === 'myBoards') {
        listData.value = res.data || []
        listTotal.value = (res.data || []).length
      } else {
        listData.value = res.data?.records || []
        listTotal.value = res.data?.total || 0
      }
    }
  } catch (err) {
    ElMessage({ type: 'error', message: err.message || '加载失败' })
  } finally {
    loading.value = false
  }
}

// ==================== 操作 ====================
const startConversation = () => {
  if (!targetUserId.value) return
  router.push(`/chat/${targetUserId.value}`)
}

const handleSaveBio = async () => {
  try {
    const res = await updateBio(bioInput.value)
    profile.value = res.data
    bioEditing.value = false
    ElMessage({ type: 'success', message: '简介已更新' })
  } catch (err) {
    ElMessage({ type: 'error', message: err.message || '保存失败' })
  }
}

const startEditProfile = () => {
  editForm.value = {
    bio: profile.value?.bio || '',
    phone: profile.value?.phone || '',
    email: profile.value?.email || '',
    jobNature: profile.value?.jobNature || '',
    location: profile.value?.location || '',
  }
  profileEditing.value = true
}

const cancelEditProfile = () => {
  profileEditing.value = false
}

const handleSaveProfile = async () => {
  profileSaving.value = true
  try {
    const res = await updateProfile(editForm.value)
    profile.value = res.data
    profileEditing.value = false
    ElMessage({ type: 'success', message: '资料已更新' })
  } catch (err) {
    ElMessage({ type: 'error', message: err.message || '保存失败' })
  } finally {
    profileSaving.value = false
  }
}

const handleAvatarChange = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return

  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
  if (!allowedTypes.includes(file.type)) {
    ElMessage({ type: 'warning', message: '仅支持 JPG/PNG/GIF/WebP 格式' })
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage({ type: 'warning', message: '图片大小不能超过 5MB' })
    return
  }

  avatarUploading.value = true
  try {
    const res = await uploadAvatar(file)
    profile.value.avatar = res.data
    // 同步更新 localStorage 中的用户信息
    const stored = JSON.parse(localStorage.getItem('user') || '{}')
    if (stored.user) {
      stored.user.avatar = res.data
      localStorage.setItem('user', JSON.stringify(stored))
    }
    ElMessage({ type: 'success', message: '头像已更新' })
  } catch (err) {
    ElMessage({ type: 'error', message: err.message || '上传失败' })
  } finally {
    avatarUploading.value = false
  }
}

const handleRemoveFavorite = async (postId) => {
  try {
    await removeFavorite(postId)
    ElMessage({ type: 'success', message: '已取消收藏' })
    loadList(listPage.value)
    stats.value.favoriteCount = Math.max(0, stats.value.favoriteCount - 1)
  } catch (err) {
    ElMessage({ type: 'error', message: err.message || '操作失败' })
  }
}

const handleRemoveLike = async (postId) => {
  try {
    await removeLike(postId)
    ElMessage({ type: 'success', message: '已取消点赞' })
    loadList(listPage.value)
    stats.value.likeCount = Math.max(0, stats.value.likeCount - 1)
  } catch (err) {
    ElMessage({ type: 'error', message: err.message || '操作失败' })
  }
}

const handleTabChange = (tab) => {
  activeTab.value = tab
  if (tab !== 'info') {
    listPage.value = 1
    loadList(1)
  }
}

const handlePageChange = (page) => {
  loadList(page)
}

const viewPost = (postId) => {
  router.push(`/post/${postId}`)
}

const handleDeleteHistory = async (postId) => {
  try {
    await deleteHistory(postId)
    ElMessage({ type: 'success', message: '已删除浏览记录' })
    loadList(listPage.value)
  } catch (err) {
    ElMessage({ type: 'error', message: err.message || '操作失败' })
  }
}

const handleClearHistory = async () => {
  try {
    await clearHistory()
    ElMessage({ type: 'success', message: '已清空浏览历史' })
    loadList(1)
  } catch (err) {
    ElMessage({ type: 'error', message: err.message || '操作失败' })
  }
}

const handleAddFriend = async () => {
  if (!targetUserId.value) return
  friendLoading.value = true
  try {
    await sendFriendRequest(Number(targetUserId.value))
    ElMessage({ type: 'success', message: '好友请求已发送' })
    friendStatus.value = 'pending'
  } catch (err) {
    ElMessage({ type: 'error', message: err.message || '操作失败' })
  } finally {
    friendLoading.value = false
  }
}

const checkFriendStatus = async () => {
  if (!isOtherUser.value || !targetUserId.value) return
  try {
    const res = await isFriend(Number(targetUserId.value))
    friendStatus.value = res.data?.isFriend || false
  } catch { friendStatus.value = false }
}

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 19)
}

// ==================== 生命周期 ====================
onMounted(async () => {
  loadProfile()
  await checkFriendStatus()
  if (activeTab.value !== 'info') {
    loadList(1)
  }
})
</script>

<template>
  <div class="profile-page">
    <!-- 选项卡 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange">
      <el-tab-pane label="基本信息" name="info" />
      <el-tab-pane v-if="!isOtherUser" label="我的收藏" name="favorites" />
      <el-tab-pane v-if="!isOtherUser" label="我的点赞" name="likes" />
      <el-tab-pane v-if="!isOtherUser" label="我的帖子" name="myPosts" />
      <el-tab-pane v-if="isOtherUser" :label="profile?.username + '的帖子'" name="posts" />
      <el-tab-pane v-if="!isOtherUser" label="浏览历史" name="history" />
      <el-tab-pane v-if="!isOtherUser" label="我的贴吧" name="myBoards" />
    </el-tabs>

    <!-- ==================== 基本信息 ==================== -->
    <div v-if="activeTab === 'info'" class="tab-content">
      <div class="profile-header">
        <!-- 头像 -->
        <div class="avatar-section" v-loading="avatarUploading">
          <el-avatar :size="100" :src="avatarUrl" class="profile-avatar">
            {{ profile?.username?.charAt(0) }}
          </el-avatar>
          <label v-if="!isOtherUser" class="avatar-upload-label">
            <input
              type="file"
              accept="image/*"
              class="avatar-upload-input"
              @change="handleAvatarChange"
            />
            <span class="avatar-upload-text">更换头像</span>
          </label>
        </div>

        <!-- 基本信息卡片 -->
        <div class="info-cards">
          <el-card shadow="hover" class="info-card">
            <div class="info-row"><strong>用户名：</strong>{{ profile?.username || '-' }}</div>
            <div class="info-row"><strong>角色：</strong>{{ profile?.globalRole === 'SYS_ADMIN' ? '管理员' : '普通用户' }}</div>
            <div class="info-row"><strong>积分：</strong>{{ profile?.points ?? 0 }}</div>
            <div class="info-row"><strong>位置：</strong>{{ profile?.location || '未设置' }}</div>
            <div class="info-row"><strong>身份：</strong>{{ profile?.jobNature || '未设置' }}</div>
          </el-card>
          <!-- 联系方式卡片 -->
          <el-card shadow="hover" class="info-card">
            <div class="info-row"><strong>邮箱：</strong>{{ profile?.email || '未设置' }}</div>
            <div class="info-row"><strong>电话：</strong>{{ profile?.phone || '未设置' }}</div>
          </el-card>
          <el-card v-if="isOtherUser" shadow="hover" class="info-card">
            <div style="display:flex;gap:8px;">
              <el-button type="primary" @click="startConversation">发起对话</el-button>
              <el-button
                v-if="!friendStatus"
                type="success"
                :loading="friendLoading"
                @click="handleAddFriend"
              >
                {{ friendStatus === 'pending' ? '已发送请求' : '添加好友' }}
              </el-button>
              <el-tag v-else type="success">已是好友</el-tag>
            </div>
          </el-card>
          <el-card shadow="hover" class="info-card stats-card">
            <div class="stats-grid">
              <div class="stat-item">
                <div class="stat-num">{{ stats.postCount }}</div>
                <div class="stat-label">发帖</div>
              </div>
              <div v-if="!isOtherUser" class="stat-item">
                <div class="stat-num">{{ stats.favoriteCount }}</div>
                <div class="stat-label">收藏</div>
              </div>
              <div v-if="!isOtherUser" class="stat-item">
                <div class="stat-num">{{ stats.likeCount }}</div>
                <div class="stat-label">点赞</div>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 个人资料编辑 -->
      <el-card v-if="!isOtherUser" shadow="hover" class="bio-card">
        <template #header>
          <div class="card-header">
            <span>个人资料</span>
            <el-button
              v-if="!profileEditing"
              type="primary"
              size="small"
              text
              @click="startEditProfile"
            >
              编辑
            </el-button>
          </div>
        </template>
        <div v-if="profileEditing" class="profile-edit-form">
          <el-form label-width="80px" label-position="left">
            <el-form-item label="个人简介">
              <el-input v-model="editForm.bio" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="写一段自我介绍..." />
            </el-form-item>
            <el-form-item label="联系方式">
              <el-input v-model="editForm.phone" placeholder="手机号码" />
            </el-form-item>
            <el-form-item label="电子邮箱">
              <el-input v-model="editForm.email" placeholder="邮箱地址" />
            </el-form-item>
            <el-form-item label="工作身份">
              <el-input v-model="editForm.jobNature" placeholder="如：学生/前端工程师/设计师" />
            </el-form-item>
            <el-form-item label="所在地区">
              <el-input v-model="editForm.location" placeholder="如：北京/上海/广州" />
            </el-form-item>
          </el-form>
          <div class="bio-edit-actions">
            <el-button @click="cancelEditProfile">取消</el-button>
            <el-button type="primary" @click="handleSaveProfile" :loading="profileSaving">保存</el-button>
          </div>
        </div>
        <div v-else>
          <div class="info-row"><strong>个人简介：</strong>{{ profile?.bio || '暂无简介，点击编辑添加' }}</div>
          <div class="info-row"><strong>联系方式：</strong>{{ profile?.phone || '未设置' }}</div>
          <div class="info-row"><strong>电子邮箱：</strong>{{ profile?.email || '未设置' }}</div>
          <div class="info-row"><strong>工作身份：</strong>{{ profile?.jobNature || '未设置' }}</div>
          <div class="info-row"><strong>所在地区：</strong>{{ profile?.location || '未设置' }}</div>
        </div>
      </el-card>

      <!-- 他人查看：只读资料 -->
      <el-card v-if="isOtherUser" shadow="hover" class="bio-card">
        <template #header><span>个人资料</span></template>
        <div class="info-row"><strong>个人简介：</strong>{{ profile?.bio || '暂无' }}</div>
        <div class="info-row"><strong>联系方式：</strong>{{ profile?.phone || '未设置' }}</div>
        <div class="info-row"><strong>电子邮箱：</strong>{{ profile?.email || '未设置' }}</div>
        <div class="info-row"><strong>工作身份：</strong>{{ profile?.jobNature || '未设置' }}</div>
        <div class="info-row"><strong>所在地区：</strong>{{ profile?.location || '未设置' }}</div>
      </el-card>
    </div>

    <!-- ==================== 我的贴吧 ==================== -->
    <div v-if="activeTab === 'myBoards'" class="tab-content">
      <div v-loading="loading" class="board-list">
        <div v-if="listData.length === 0 && !loading" style="text-align:center;padding:40px;color:#909399;">你还未加入任何贴吧</div>
        <el-card v-for="b in listData" :key="b.boardId" shadow="hover" class="myboard-card" @click="router.push('/forum/' + b.boardId)">
          <div class="myboard-info">
            <el-avatar :size="40" :src="b.boardAvatar ? 'http://localhost:8081' + b.boardAvatar : ''" shape="square">{{ b.boardName?.charAt(0) }}</el-avatar>
            <div class="myboard-meta">
              <div class="myboard-name">{{ b.boardName }}</div>
              <div class="myboard-role">
                <el-tag v-if="b.boardRole === 'OWNER'" size="small" type="danger">吧主</el-tag>
                <el-tag v-else-if="b.boardRole === 'ADMIN'" size="small" type="warning">管理员</el-tag>
                <el-tag v-else size="small" type="default">成员</el-tag>
                <span style="margin-left:8px;font-size:12px;color:#909399;">活跃度：{{ b.activityPoints }}</span>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- ==================== 列表页（收藏 / 点赞 / 历史） ==================== -->
    <div v-else-if="activeTab !== 'info'" class="tab-content">
      <!-- 浏览历史操作栏 -->
      <div v-if="activeTab === 'history' && listData.length > 0" style="margin-bottom:12px;text-align:right;">
        <el-button type="danger" size="small" @click="handleClearHistory">清空全部</el-button>
      </div>
      <!-- 有数据 -->
      <template v-if="listData.length > 0">
        <div v-loading="loading" class="post-list">
          <el-card
            v-for="item in listData"
            :key="item.id"
            shadow="hover"
            class="post-card clickable"
            @click="viewPost(item.id)"
          >
            <div class="post-card-body">
              <div class="post-card-main">
                <h3 class="post-card-title">{{ item.title }}</h3>
                <div class="post-card-meta">
                  <span>{{ formatTime(activeTab === 'history' ? item.createdAt : item.createdAt) }}</span>
                  <span v-if="item.boardId">板块 #{{ item.boardId }}</span>
                </div>
              </div>
              <div class="post-card-actions">
                <el-button
                  v-if="activeTab === 'favorites'"
                  type="danger"
                  size="small"
                  text
                  @click.stop="handleRemoveFavorite(item.id)"
                >
                  取消收藏
                </el-button>
                <el-button
                  v-if="activeTab === 'likes'"
                  type="danger"
                  size="small"
                  text
                  @click.stop="handleRemoveLike(item.id)"
                >
                  取消点赞
                </el-button>
                <el-button
                  v-if="activeTab === 'history'"
                  type="danger"
                  size="small"
                  text
                  @click.stop="handleDeleteHistory(item.id)"
                >
                  删除
                </el-button>
              </div>
            </div>
          </el-card>
        </div>

        <!-- 分页 -->
        <div class="pagination-wrapper" v-if="listTotal > listPageSize">
          <el-pagination
            :current-page="listPage"
            :page-size="listPageSize"
            :total="listTotal"
            layout="prev, pager, next"
            background
            @current-change="handlePageChange"
          />
        </div>
      </template>

      <!-- 空状态 -->
      <el-empty v-else-if="!loading" :description="`暂无${listTitle}`" />
    </div>
  </div>
</template>

<style scoped>
.profile-page {
  max-width: 900px;
  margin: 0 auto;
}

.tab-content {
  padding-top: 16px;
}

/* ---- 基本信息 ---- */
.profile-header {
  display: flex;
  gap: 24px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.profile-avatar {
  border: 3px solid #e4e7ed;
}

.avatar-upload-label {
  cursor: pointer;
  font-size: 13px;
  color: #409eff;
  user-select: none;
}

.avatar-upload-label:hover {
  color: #66b1ff;
}

.avatar-upload-input {
  display: none;
}

.info-cards {
  flex: 1;
  min-width: 280px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.info-card {
  border-radius: 8px;
}

.info-row {
  padding: 4px 0;
  font-size: 14px;
  color: #303133;
}

.stats-grid {
  display: flex;
  justify-content: space-around;
  text-align: center;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #409eff;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 4px;
}

/* ---- 个人简介 ---- */
.bio-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.bio-text {
  font-size: 14px;
  color: #606266;
  line-height: 1.8;
  min-height: 40px;
}

.bio-edit-actions {
  margin-top: 12px;
  display: flex;
  gap: 8px;
  justify-content: flex-end;
}

/* ---- 列表 ---- */
.post-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.post-card {
  border-radius: 8px;
}

.clickable {
  cursor: pointer;
}

.post-card-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.post-card-main {
  flex: 1;
  min-width: 0;
}

.post-card-title {
  font-size: 15px;
  margin: 0 0 6px 0;
  color: #303133;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.post-card-meta {
  font-size: 12px;
  color: #909399;
  display: flex;
  gap: 16px;
}

.post-card-actions {
  flex-shrink: 0;
  margin-left: 16px;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 24px 0;
}

/* ---- 我的贴吧 ---- */
.board-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.myboard-card {
  border-radius: 8px;
  cursor: pointer;
}

.myboard-card:hover {
  border-color: #409eff;
}

.myboard-info {
  display: flex;
  align-items: center;
  gap: 14px;
}

.myboard-meta {
  flex: 1;
}

.myboard-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.myboard-role {
  display: flex;
  align-items: center;
  margin-top: 4px;
}
</style>
