<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getProfile, updateBio, uploadAvatar,
  getFavorites, removeFavorite,
  getLikes, removeLike,
  getHistory,
} from '@/api/profile'

const BASE_URL = 'http://localhost:8080'

// ==================== 状态 ====================
const activeTab = ref('info')
const profile = ref(null)
const stats = ref({ favoriteCount: 0, likeCount: 0, postCount: 0 })
const bioInput = ref('')
const bioEditing = ref(false)
const avatarUploading = ref(false)

// 列表
const loading = ref(false)
const listData = ref([])
const listTotal = ref(0)
const listPage = ref(1)
const listPageSize = ref(10)

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
    const res = await getProfile()
    profile.value = res.data?.user || null
    stats.value = res.data?.stats || { favoriteCount: 0, likeCount: 0, postCount: 0 }
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
    }
    if (res) {
      listData.value = res.data?.records || []
      listTotal.value = res.data?.total || 0
    }
  } catch (err) {
    ElMessage({ type: 'error', message: err.message || '加载失败' })
  } finally {
    loading.value = false
  }
}

// ==================== 操作 ====================
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

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 19)
}

// ==================== 生命周期 ====================
onMounted(() => {
  loadProfile()
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
      <el-tab-pane label="我的收藏" name="favorites" />
      <el-tab-pane label="我的点赞" name="likes" />
      <el-tab-pane label="浏览历史" name="history" />
    </el-tabs>

    <!-- ==================== 基本信息 ==================== -->
    <div v-if="activeTab === 'info'" class="tab-content">
      <div class="profile-header">
        <!-- 头像 -->
        <div class="avatar-section" v-loading="avatarUploading">
          <el-avatar :size="100" :src="avatarUrl" class="profile-avatar">
            {{ currentUser.username?.charAt(0) }}
          </el-avatar>
          <label class="avatar-upload-label">
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
            <div class="info-row"><strong>用户名：</strong>{{ currentUser.username || '-' }}</div>
            <div class="info-row"><strong>角色：</strong>{{ currentUser.globalRole === 'SYS_ADMIN' ? '管理员' : '普通用户' }}</div>
            <div class="info-row"><strong>积分：</strong>{{ currentUser.points ?? 0 }}</div>
          </el-card>
          <el-card shadow="hover" class="info-card stats-card">
            <div class="stats-grid">
              <div class="stat-item">
                <div class="stat-num">{{ stats.postCount }}</div>
                <div class="stat-label">发帖</div>
              </div>
              <div class="stat-item">
                <div class="stat-num">{{ stats.favoriteCount }}</div>
                <div class="stat-label">收藏</div>
              </div>
              <div class="stat-item">
                <div class="stat-num">{{ stats.likeCount }}</div>
                <div class="stat-label">点赞</div>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 个人简介 -->
      <el-card shadow="hover" class="bio-card">
        <template #header>
          <div class="card-header">
            <span>个人简介</span>
            <el-button
              v-if="!bioEditing"
              type="primary"
              size="small"
              text
              @click="bioEditing = true"
            >
              编辑
            </el-button>
          </div>
        </template>
        <div v-if="bioEditing" class="bio-edit">
          <el-input
            v-model="bioInput"
            type="textarea"
            :rows="4"
            maxlength="500"
            show-word-limit
            placeholder="写一段自我介绍..."
          />
          <div class="bio-edit-actions">
            <el-button @click="bioEditing = false; bioInput = profile?.bio || ''">取消</el-button>
            <el-button type="primary" @click="handleSaveBio">保存</el-button>
          </div>
        </div>
        <div v-else class="bio-text">
          {{ profile?.bio || '暂无简介，点击编辑添加' }}
        </div>
      </el-card>
    </div>

    <!-- ==================== 列表页（收藏 / 点赞 / 历史） ==================== -->
    <div v-else class="tab-content">
      <!-- 有数据 -->
      <template v-if="listData.length > 0">
        <div v-loading="loading" class="post-list">
          <el-card
            v-for="item in listData"
            :key="item.id"
            shadow="hover"
            class="post-card"
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
                  @click="handleRemoveFavorite(item.id)"
                >
                  取消收藏
                </el-button>
                <el-button
                  v-if="activeTab === 'likes'"
                  type="danger"
                  size="small"
                  text
                  @click="handleRemoveLike(item.id)"
                >
                  取消点赞
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
</style>
