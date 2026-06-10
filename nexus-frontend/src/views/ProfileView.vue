<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getProfile, updateProfile, uploadAvatar, uploadBackground, setDefaultBackground,
  getFavorites, removeFavorite, getLikes, removeLike,
  getHistory, deleteHistory, clearHistory,
} from '@/api/profile'
import { getPublicProfile } from '@/api/user'
import { sendFriendRequest, isFriend } from '@/api/friend'
import { getMyBoards } from '@/api/board'
import { getPostsByUser } from '@/api/post'

const BASE = 'http://localhost:8081'
const route = useRoute()
const router = useRouter()

// ========== state ==========
const activeTab = ref('info')
const profile = ref(null)
const stats = ref({ postCount: 0, favoriteCount: 0, likeCount: 0 })
const avatarUploading = ref(false)
const bgUploading = ref(false)
const bgInputRef = ref(null)

const profileEditing = ref(false)
const profileSaving = ref(false)
const editForm = ref({ bio: '', phone: '', email: '', jobNature: '', location: '' })

const targetUserId = computed(() => route.params.userId)
const isOtherUser = computed(() => targetUserId.value && Number(targetUserId.value) !== currentUser.value.id)

const loading = ref(false)
const listData = ref([])
const listTotal = ref(0)
const listPage = ref(1)
const listPageSize = ref(10)
const friendStatus = ref('none')
const friendLoading = ref(false)
const myBoards = ref([])

// ========== default backgrounds ==========
const defaultBgs = [
  { name: 'default:blue', label: '天空蓝', gradient: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)' },
  { name: 'default:green', label: '青翠绿', gradient: 'linear-gradient(135deg, #11998e 0%, #38ef7d 100%)' },
  { name: 'default:purple', label: '紫罗兰', gradient: 'linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%)' },
  { name: 'default:sunset', label: '日落橙', gradient: 'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)' },
  { name: 'default:ocean', label: '深海蓝', gradient: 'linear-gradient(135deg, #2193b0 0%, #6dd5ed 100%)' },
  { name: 'default:night', label: '夜幕紫', gradient: 'linear-gradient(135deg, #0f0c29 0%, #302b63 50%, #24243e 100%)' },
]

// ========== computed ==========
const currentUser = computed(() => {
  const s = localStorage.getItem('user')
  try { return s ? JSON.parse(s).user || {} : {} } catch { return {} }
})
const avatarUrl = computed(() => {
  const a = profile.value?.avatar
  return a ? (a.startsWith('http') ? a : BASE + a) : ''
})
const bgStyle = computed(() => {
  const bg = profile.value?.background
  if (!bg) return defaultBgs[0].gradient
  if (bg.startsWith('default:')) {
    const found = defaultBgs.find(d => d.name === bg)
    return found ? found.gradient : defaultBgs[0].gradient
  }
  return `url(${bg.startsWith('http') ? bg : BASE + bg}) center/cover no-repeat`
})

// ========== load ==========
const loadProfile = async () => {
  try {
    const res = isOtherUser.value ? await getPublicProfile(targetUserId.value) : await getProfile()
    profile.value = res.data?.user
    stats.value = res.data?.stats || { postCount: 0, favoriteCount: 0, likeCount: 0 }
    if (isOtherUser.value) {
      myBoards.value = res.data?.boards || []
    } else {
      try { myBoards.value = ((await getMyBoards()).data) || [] } catch {}
    }
  } catch (err) { ElMessage.error(err.message || '加载失败') }
}

const loadList = async (page = 1) => {
  loading.value = true; listPage.value = page
  try {
    let res
    if (activeTab.value === 'favorites') res = await getFavorites(page, listPageSize.value)
    else if (activeTab.value === 'likes') res = await getLikes(page, listPageSize.value)
    else if (activeTab.value === 'history') res = await getHistory(page, listPageSize.value)
    else if (activeTab.value === 'posts') res = await getPostsByUser(targetUserId.value, page, listPageSize.value)
    else if (activeTab.value === 'myPosts') res = await getPostsByUser(currentUser.value.id, page, listPageSize.value)
    if (res) { listData.value = res.data?.records || []; listTotal.value = res.data?.total || 0 }
  } catch (err) { ElMessage.error(err.message || '加载失败') }
  finally { loading.value = false }
}

// ========== actions ==========
const handleTabChange = async (tab) => {
  activeTab.value = tab
  if (tab === 'myBoards') { try { myBoards.value = ((await getMyBoards()).data) || [] } catch {} }
  else if (tab !== 'info') { listPage.value = 1; loadList(1) }
}
const viewPost = (id) => router.push('/post/' + id)

const handleAvatarChange = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) { ElMessage.warning('请选择图片文件'); return }
  if (file.size > 5*1024*1024) { ElMessage.warning('图片不能超过5MB'); return }
  avatarUploading.value = true
  try {
    const res = await uploadAvatar(file)
    profile.value.avatar = res.data
    const s = JSON.parse(localStorage.getItem('user')||'{}')
    if (s.user) { s.user.avatar = res.data; localStorage.setItem('user', JSON.stringify(s)) }
    ElMessage.success('头像已更新')
  } catch (err) { ElMessage.error(err.message || '上传失败') }
  finally { avatarUploading.value = false }
}

const handleBgUpload = async (e) => {
  const file = e.target.files?.[0]
  if (!file) return
  if (!file.type.startsWith('image/')) { ElMessage.warning('请选择图片文件'); return }
  bgUploading.value = true
  try {
    const res = await uploadBackground(file)
    profile.value.background = res.data
    ElMessage.success('背景已更新')
  } catch (err) { ElMessage.error(err.message || '上传失败') }
  finally { bgUploading.value = false }
}

const selectDefaultBg = async (bgName) => {
  try {
    await setDefaultBackground(bgName)
    profile.value.background = bgName
    ElMessage.success('背景已更换')
  } catch (err) { ElMessage.error(err.message || '设置失败') }
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
const handleSaveProfile = async () => {
  profileSaving.value = true
  try { const res = await updateProfile(editForm.value); profile.value = res.data; profileEditing.value = false; ElMessage.success('资料已更新') }
  catch (err) { ElMessage.error(err.message || '保存失败') }
  finally { profileSaving.value = false }
}

const handleAddFriend = async () => {
  friendLoading.value = true
  try { await sendFriendRequest(Number(targetUserId.value)); friendStatus.value = 'pending_sent'; ElMessage.success('请求已发送') }
  catch (err) { ElMessage.error(err.message || '操作失败') }
  finally { friendLoading.value = false }
}

const checkFriendStatus = async () => {
  if (!isOtherUser.value) return
  try { friendStatus.value = (await isFriend(Number(targetUserId.value))).data?.status || 'none' }
  catch { friendStatus.value = 'none' }
}

const handleRemoveFavorite = async (id) => { try { await removeFavorite(id); loadList(listPage.value); stats.value.favoriteCount = Math.max(0, stats.value.favoriteCount - 1) } catch(e){} }
const handleRemoveLike = async (id) => { try { await removeLike(id); loadList(listPage.value); stats.value.likeCount = Math.max(0, stats.value.likeCount - 1) } catch(e){} }
const handleDeleteHistory = async (id) => { try { await deleteHistory(id); loadList(listPage.value) } catch(e){} }
const handleClearHistory = async () => { try { await clearHistory(); loadList(1) } catch(e){} }

onMounted(async () => { await loadProfile(); await checkFriendStatus(); if (activeTab.value !== 'info') loadList(1) })
</script>

<template>
  <div class="profile-page">
    <!-- banner -->
    <div class="profile-banner" :style="{ background: bgStyle }">
      <div class="banner-overlay"></div>
      <div class="banner-content">
        <div class="avatar-wrapper">
          <el-avatar :size="96" :src="avatarUrl" class="profile-avatar">
            {{ profile?.username?.charAt(0) }}
          </el-avatar>
          <label v-if="!isOtherUser" class="avatar-upload-btn" v-loading="avatarUploading">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2v11z" stroke="currentColor" stroke-width="1.5"/><circle cx="12" cy="13" r="4" stroke="currentColor" stroke-width="1.5"/></svg>
            <input type="file" accept="image/*" @change="handleAvatarChange" />
          </label>
        </div>
        <h2 class="banner-name">{{ profile?.username || '未知用户' }}</h2>
        <p class="banner-role">
          <span v-if="profile?.globalRole === 'SYS_ADMIN'" class="tag tag-admin">系统管理员</span>
          <span v-else-if="profile?.globalRole === 'NOTIFY_ADMIN'" class="tag tag-notify">通知管理员</span>
          <span v-else class="tag tag-user">用户</span>
          <span class="banner-points">积分 {{ profile?.points ?? 0 }}</span>
        </p>
        <div v-if="isOtherUser" class="banner-actions">
          <el-button type="primary" size="default" round @click="router.push('/chat/' + targetUserId)">发送消息</el-button>
          <el-button v-if="friendStatus === 'none'" type="success" size="default" round :loading="friendLoading" @click="handleAddFriend">添加好友</el-button>
          <el-tag v-else-if="friendStatus === 'pending_sent'" type="warning" size="large" round>等待对方通过</el-tag>
          <el-tag v-else-if="friendStatus === 'pending_received'" type="info" size="large" round>对方已申请</el-tag>
          <el-tag v-else-if="friendStatus === 'accepted'" type="success" size="large" round>已是好友</el-tag>
        </div>
        <!-- stats row -->
        <div class="banner-stats">
          <div class="stat-it"><strong>{{ stats.postCount }}</strong><span>发帖</span></div>
          <div v-if="!isOtherUser" class="stat-it"><strong>{{ stats.favoriteCount }}</strong><span>收藏</span></div>
          <div v-if="!isOtherUser" class="stat-it"><strong>{{ stats.likeCount }}</strong><span>点赞</span></div>
        </div>
      </div>
    </div>

    <!-- bg editor (owner only) -->
    <div v-if="!isOtherUser" class="bg-editor">
      <el-popover placement="bottom" :width="320" trigger="click">
        <template #reference>
          <el-button size="small" text>更换背景</el-button>
        </template>
        <div class="bg-grid">
          <div v-for="bg in defaultBgs" :key="bg.name" class="bg-chip" :style="{ background: bg.gradient }" :title="bg.label" @click.stop="selectDefaultBg(bg.name)">{{ bg.label }}</div>
          <div class="bg-chip bg-custom" @click.stop="bgInputRef?.click()">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/></svg>
            <span v-if="!bgUploading">自定义</span>
            <span v-else class="uploading-tip">上传中</span>
          </div>
        </div>
        <input ref="bgInputRef" type="file" accept="image/*" style="display:none" @change="handleBgUpload" />
      </el-popover>
    </div>

    <!-- tabs -->
    <div class="profile-body">
      <div class="profile-tabs">
        <button :class="{ active: activeTab === 'info' }" @click="handleTabChange('info')">主页</button>
        <button v-if="!isOtherUser" :class="{ active: activeTab === 'favorites' }" @click="handleTabChange('favorites')">收藏</button>
        <button v-if="!isOtherUser" :class="{ active: activeTab === 'likes' }" @click="handleTabChange('likes')">点赞</button>
        <button v-if="!isOtherUser" :class="{ active: activeTab === 'myPosts' }" @click="handleTabChange('myPosts')">我的帖子</button>
        <button v-if="isOtherUser" :class="{ active: activeTab === 'posts' }" @click="handleTabChange('posts')">{{ profile?.username }}的帖子</button>
        <button v-if="!isOtherUser" :class="{ active: activeTab === 'history' }" @click="handleTabChange('history')">历史</button>
      </div>

      <!-- info -->
      <div v-if="activeTab === 'info'" class="tab-pane">
        <div class="info-grid">
          <!-- left -->
          <div class="info-card">
            <h4>个人资料</h4>
            <template v-if="profileEditing">
              <el-form label-width="70px" label-position="left" size="small">
                <el-form-item label="简介"><el-input v-model="editForm.bio" type="textarea" :rows="3" maxlength="200" show-word-limit /></el-form-item>
                <el-form-item label="电话"><el-input v-model="editForm.phone" /></el-form-item>
                <el-form-item label="邮箱"><el-input v-model="editForm.email" /></el-form-item>
                <el-form-item label="身份"><el-input v-model="editForm.jobNature" placeholder="学生/前端工程师..." /></el-form-item>
                <el-form-item label="地区"><el-input v-model="editForm.location" placeholder="北京/上海..." /></el-form-item>
              </el-form>
              <div style="display:flex;gap:8px;justify-content:flex-end;">
                <el-button size="small" @click="profileEditing = false">取消</el-button>
                <el-button size="small" type="primary" :loading="profileSaving" @click="handleSaveProfile">保存</el-button>
              </div>
            </template>
            <template v-else>
              <dl class="info-dl">
                <dt>简介</dt><dd>{{ profile?.bio || '未设置' }}</dd>
                <dt>电话</dt><dd>{{ profile?.phone || '未设置' }}</dd>
                <dt>邮箱</dt><dd>{{ profile?.email || '未设置' }}</dd>
                <dt>身份</dt><dd>{{ profile?.jobNature || '未设置' }}</dd>
                <dt>地区</dt><dd>{{ profile?.location || '未设置' }}</dd>
              </dl>
              <el-button v-if="!isOtherUser" size="small" @click="startEditProfile">编辑资料</el-button>
            </template>
          </div>
          <!-- right: boards -->
          <div class="info-card">
            <h4>{{ isOtherUser ? (profile?.username || 'TA') + '加入的吧' : '我的贴吧' }}</h4>
            <div v-if="myBoards.length === 0" style="color:#909399;font-size:13px;">暂未加入任何贴吧</div>
            <div v-for="b in myBoards" :key="b.boardId" class="board-row" @click="router.push('/forum/' + b.boardId)">
              <el-avatar :size="32" shape="square" :src="b.boardAvatar ? BASE + b.boardAvatar : ''">{{ b.boardName?.charAt(0) }}</el-avatar>
              <span>{{ b.boardName }}</span>
              <el-tag size="small" :type="b.boardRole==='OWNER'?'danger':b.boardRole==='ADMIN'?'warning':''">{{ b.boardRole==='OWNER'?'吧主':b.boardRole==='ADMIN'?'管理':'成员' }}</el-tag>
            </div>
          </div>
        </div>
      </div>

      <!-- list -->
      <div v-else class="tab-pane">
        <div v-if="activeTab === 'history' && listData.length > 0" style="text-align:right;margin-bottom:8px;">
          <el-button size="small" type="danger" text @click="handleClearHistory">清空历史</el-button>
        </div>
        <div v-loading="loading" class="post-list">
          <div v-for="item in listData" :key="item.id || item.boardId" class="list-card" @click="activeTab === 'myBoards' ? router.push('/forum/' + item.boardId) : viewPost(item.id)">
            <template v-if="activeTab !== 'myBoards'">
              <div class="list-main">
                <h4 class="list-title">{{ item.title }}</h4>
                <div class="list-meta">
                  <span v-if="item.boardName">吧：{{ item.boardName }}</span>
                  <span>{{ (item.createdAt || item.createTime || '').replace('T',' ').slice(0,16) }}</span>
                </div>
              </div>
              <div class="list-actions" @click.stop>
                <el-button v-if="activeTab === 'favorites'" size="small" text type="danger" @click="handleRemoveFavorite(item.id)">取消收藏</el-button>
                <el-button v-if="activeTab === 'likes'" size="small" text type="danger" @click="handleRemoveLike(item.id)">取消点赞</el-button>
                <el-button v-if="activeTab === 'history'" size="small" text type="danger" @click="handleDeleteHistory(item.id)">删除</el-button>
              </div>
            </template>
            <template v-else>
              <div style="display:flex;align-items:center;gap:12px;">
                <el-avatar :size="36" shape="square" :src="item.boardAvatar ? BASE + item.boardAvatar : ''">{{ item.boardName?.charAt(0) }}</el-avatar>
                <div>
                  <strong>{{ item.boardName }}</strong>
                  <div style="font-size:12px;color:#909399;">
                    <el-tag size="small">{{ item.boardRole==='OWNER'?'吧主':item.boardRole==='ADMIN'?'管理':'成员' }}</el-tag>
                    活跃度 {{ item.activityPoints }}
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>
        <div v-if="listTotal > listPageSize && activeTab !== 'myBoards'" style="display:flex;justify-content:center;padding:16px 0;">
          <el-pagination :current-page="listPage" :page-size="listPageSize" :total="listTotal" layout="prev,pager,next" background @current-change="loadList" />
        </div>
        <el-empty v-if="!loading && listData.length === 0" :description="'暂无内容'" />
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-page { max-width: 900px; margin: 0 auto; border-radius: 12px; overflow: hidden; }

/* banner */
.profile-banner { position: relative; padding: 48px 32px 24px; color: #fff; min-height: 220px; display: flex; flex-direction: column; justify-content: flex-end; }
.banner-overlay { position: absolute; inset: 0; background: rgba(0,0,0,.25); }
.banner-content { position: relative; z-index: 1; display: flex; flex-direction: column; align-items: center; gap: 8px; }
.avatar-wrapper { position: relative; }
.profile-avatar { border: 4px solid rgba(255,255,255,.6); box-shadow: 0 4px 20px rgba(0,0,0,.2); background: #409eff; font-size: 36px; font-weight: 700; }
.avatar-upload-btn {
  position: absolute; bottom: 0; right: 0; width: 32px; height: 32px; border-radius: 50%;
  background: #fff; color: #409eff; display: flex; align-items: center; justify-content: center;
  cursor: pointer; box-shadow: 0 2px 8px rgba(0,0,0,.15); transition: transform .15s;
}
.avatar-upload-btn:hover { transform: scale(1.1); }
.avatar-upload-btn input { display: none; }
.banner-name { margin: 0; font-size: 22px; font-weight: 700; }
.banner-role { display: flex; gap: 8px; align-items: center; font-size: 13px; }
.tag { padding: 2px 10px; border-radius: 10px; font-size: 12px; }
.tag-admin { background: rgba(255,255,255,.25); }
.tag-notify { background: rgba(255,255,255,.25); }
.tag-user { background: rgba(255,255,255,.2); }
.banner-points { opacity: .85; }
.banner-actions { display: flex; gap: 8px; margin-top: 4px; }
.banner-stats { display: flex; gap: 32px; margin-top: 12px; }
.stat-it { display: flex; flex-direction: column; align-items: center; font-size: 13px; opacity: .9; }
.stat-it strong { font-size: 22px; font-weight: 700; }

/* bg editor */
.bg-editor { text-align: right; padding: 8px 16px 0; }
.bg-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 8px; }
.bg-chip { height: 56px; border-radius: 8px; cursor: pointer; display: flex; align-items: center; justify-content: center; font-size: 12px; color: #fff; text-shadow: 0 1px 2px rgba(0,0,0,.3); transition: transform .1s; }
.bg-chip:hover { transform: scale(1.05); }
.bg-custom { border: 2px dashed #ccc; background: #fafafa; color: #909399; text-shadow: none; flex-direction: column; gap: 2px; }
.bg-custom:hover { background: #f0f0f0; }
.bg-custom span { font-size: 10px; }

/* body */
.profile-body { background: #fff; padding: 0 16px 24px; border-radius: 0 0 12px 12px; }
.profile-tabs { display: flex; gap: 0; border-bottom: 1px solid #f0f0f0; padding: 8px 0; }
.profile-tabs button {
  padding: 8px 18px; border: none; background: none; font-size: 14px; color: #909399;
  cursor: pointer; border-bottom: 2px solid transparent; transition: all .15s;
}
.profile-tabs button:hover { color: #409eff; }
.profile-tabs button.active { color: #409eff; border-bottom-color: #409eff; font-weight: 600; }

.tab-pane { padding-top: 16px; }

/* info grid */
.info-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
@media (max-width: 700px) { .info-grid { grid-template-columns: 1fr; } }
.info-card { background: #fafafa; border-radius: 10px; padding: 16px; }
.info-card h4 { margin: 0 0 12px 0; font-size: 15px; color: #303133; }
.info-dl { margin: 0; }
.info-dl dt { font-size: 12px; color: #909399; margin-top: 10px; }
.info-dl dt:first-child { margin-top: 0; }
.info-dl dd { margin: 2px 0 0 0; font-size: 14px; color: #303133; }
.board-row { display: flex; align-items: center; gap: 10px; padding: 8px 0; cursor: pointer; border-bottom: 1px solid #f0f0f0; }
.board-row:last-child { border-bottom: none; }
.board-row:hover { background: #f5f7fa; margin: 0 -8px; padding-left: 8px; padding-right: 8px; border-radius: 6px; }

/* list */
.list-card {
  display: flex; justify-content: space-between; align-items: center; padding: 14px 12px;
  background: #fafafa; border-radius: 8px; margin-bottom: 8px; cursor: pointer; transition: background .12s;
}
.list-card:hover { background: #f0f2f5; }
.list-main { flex: 1; min-width: 0; }
.list-title { margin: 0 0 4px; font-size: 15px; color: #303133; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.list-meta { font-size: 12px; color: #909399; display: flex; gap: 16px; }
.list-actions { flex-shrink: 0; margin-left: 12px; }
</style>
