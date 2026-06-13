<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPostDetail, updatePost, deletePost } from '@/api/post'
import { toggleLike, toggleFavorite, isLiked, isFavorited, recordBrowse } from '@/api/profile'
import { getComments, createComment, deleteComment, acceptComment } from '@/api/comment'
import { getMyRole, joinBoard } from '@/api/board'
import { uploadImage } from '@/api/upload'

const route = useRoute()
const router = useRouter()

const postId = computed(() => route.params.postId)
const post = ref(null)
const loading = ref(false)
const commentLoading = ref(false)
const comments = ref([])
const commentInput = ref('')
const commentSubmitting = ref(false)
const replyTarget = ref(null)
const commentImageUploading = ref(false)
const emojiList = ['😀','😃','😄','😁','😂','🤣','😊','😇','🙂','😉','😍','🥰','😘','😋','😜','🤪','😝','😏','😒','😔','😢','😭','😤','😡','🤬','👍','👎','👏','🙌','💪','❤️','🔥','⭐','✅','❌','💯']
const liked = ref(false)
const favorited = ref(false)
const editDialogVisible = ref(false)
const editForm = ref({ title: '', content: '' })

const currentUser = computed(() => { const s = localStorage.getItem('user'); try { return s ? JSON.parse(s).user || {} : {} } catch { return {} } })
const isAuthor = computed(() => post.value && currentUser.value.id && post.value.authorId === currentUser.value.id)
const isBoardManager = ref(false)
const canManage = computed(() => isAuthor.value || isBoardManager.value)

const fetchPost = async () => {
  loading.value = true
  try { const res = await getPostDetail(postId.value); post.value = res.data || null; if (!isAuthor.value) { try { liked.value = await isLiked(postId.value).then(r => r.data) } catch {}; try { favorited.value = await isFavorited(postId.value).then(r => r.data) } catch {} } }
  catch (err) { ElMessage.error(err.message || '加载帖子失败'); router.back() }
  finally { loading.value = false }
}
const fetchComments = async () => {
  commentLoading.value = true
  try { comments.value = (await getComments(postId.value)).data || [] } catch { comments.value = [] }
  finally { commentLoading.value = false }
}
const handleLike = async () => { if (!currentUser.value.id) return ElMessage.warning('请先登录'); try { await toggleLike(postId.value); liked.value = !liked.value; post.value.likeCount = (post.value.likeCount||0)+(liked.value?1:-1) } catch(err) { ElMessage.error(err.message) } }
const handleFavorite = async () => { if (!currentUser.value.id) return ElMessage.warning('请先登录'); try { await toggleFavorite(postId.value); favorited.value = !favorited.value; post.value.favoriteCount = (post.value.favoriteCount||0)+(favorited.value?1:-1) } catch(err) { ElMessage.error(err.message) } }

const handleSubmitComment = async () => {
  if (!commentInput.value.trim()) return
  commentSubmitting.value = true
  try { const body = { postId: Number(postId.value), content: commentInput.value }; if (replyTarget.value) body.parentCommentId = replyTarget.value.id; await createComment(body); ElMessage.success(replyTarget.value?'回复成功':'评论成功'); commentInput.value = ''; fetchComments(); post.value.commentCount = (post.value.commentCount||0)+1 }
  catch (err) {
    const msg = err.message||'评论失败'
    if (msg.includes('REQUIRE_JOIN')) ElMessageBox.confirm('请先加入该吧后再评论','提示',{confirmButtonText:'立即加入',cancelButtonText:'取消',type:'warning'}).then(async()=>{try{const bid=post.value?.boardId;if(bid){await joinBoard(bid);ElMessage.success('已加入贴吧')}}catch(e){ElMessage.error(e.message)}}).catch(()=>{})
    else ElMessage.error(msg)
  }
  finally { commentSubmitting.value = false }
}

const handleCommentImage = async (e) => { const f=e.target.files?.[0]; if(!f)return; if(!f.type.startsWith('image/')){ElMessage.warning('请选择图片文件');return}; commentImageUploading.value=true; try{const r=await uploadImage(f);commentInput.value+=`\n![图片](${r.data?.url||''})\n`}catch{ElMessage.error('图片上传失败')}finally{commentImageUploading.value=false};e.target.value='' }
const startReply = (c) => { replyTarget.value = { id: c.id, username: c.authorUsername }; commentInput.value = '' }

const parseContent = (text) => { if(!text)return''; return text.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/!\[([^\]]*)\]\((\/uploads\/[^)]+)\)/g,'<img src="$2" alt="$1" style="max-width:100%;border-radius:8px;margin:4px 0;" />').replace(/\n/g,'<br>') }
const renderedContent = computed(() => parseContent(post.value?.content))

const handleDeleteComment = async (id) => { try { await ElMessageBox.confirm('确定删除这条评论吗？','提示',{type:'warning'}); await deleteComment(id); ElMessage.success('删除成功'); fetchComments(); post.value.commentCount=Math.max(0,(post.value.commentCount||0)-1) } catch(err) { if(err!=='cancel') ElMessage.error(err.message) } }
const handleAcceptComment = async (id) => {
  const maxPoints = post.value?.rewardPoints || 0
  if (maxPoints <= 0) { ElMessage.warning('悬赏积分已用完'); return }
  try {
    const { value } = await ElMessageBox.prompt('请输入悬赏金额（积分）', '采纳评论', { inputValue: maxPoints, inputType: 'number', inputValidator: v => { const n = Number(v); return n <= 0 || n > maxPoints ? `请输入1~${maxPoints}之间的积分` : true }, confirmButtonText: '确认采纳', cancelButtonText: '取消' })
    await acceptComment(id, Number(value))
    ElMessage.success(`已采纳，转移${value}积分`)
    fetchPost(); fetchComments()
  } catch(err) {
    if (err !== 'cancel') ElMessage.error(err.message || '采纳失败')
  }
}
const openEditDialog = () => { editForm.value = { title:post.value.title, content:post.value.content }; editDialogVisible.value = true }
const handleEdit = async () => { if(!editForm.value.title||!editForm.value.content)return ElMessage.warning('请填写完整'); try{await updatePost(postId.value,{title:editForm.value.title,content:editForm.value.content});ElMessage.success('修改成功');editDialogVisible.value=false;fetchPost()}catch(err){ElMessage.error(err.message)} }
const handleDelete = async () => { try{await ElMessageBox.confirm('确定删除这篇帖子吗？','提示',{type:'warning'});const pt=post.value;await deletePost(postId.value);ElMessage.success('删除成功');if(pt?.type==='REWARD'&&pt?.rewardPoints>0){const s=JSON.parse(localStorage.getItem('user')||'{}');if(s.user&&s.user.points!=null){s.user.points+=pt.rewardPoints;localStorage.setItem('user',JSON.stringify(s));window.dispatchEvent(new CustomEvent('points-updated',{detail:{points:s.user.points}}))}}router.push('/forum')}catch(err){if(err!=='cancel')ElMessage.error(err.message)} }
const goBack = () => router.back()
const goToUserProfile = (uid) => { if(!uid)return; uid===currentUser.value.id?router.push('/profile'):router.push(`/user/${uid}`) }
const formatDate = (d) => d ? new Date(d).toLocaleString() : ''
const scrollToComment = (cid) => { nextTick(()=>{ const el=document.getElementById('comment-'+cid); if(!el)return; el.scrollIntoView({behavior:'smooth',block:'center'}); el.classList.add('comment-highlight'); setTimeout(()=>el.classList.remove('comment-highlight'),2500) }) }

onMounted(async () => {
  fetchPost(); fetchComments(); recordBrowse(postId.value).catch(()=>{})
  try { const pd = (await getPostDetail(postId.value)).data; if(pd?.boardId) { const r = (await getMyRole(pd.boardId)).data?.role; isBoardManager.value = r==='OWNER'||r==='ADMIN' } } catch {}
  const cid = route.query.comment; if(cid) { await nextTick(); scrollToComment(Number(cid)) }
})
</script>

<template>
  <div class="post-detail-container">
    <div class="post-detail-header">
      <el-button text @click="goBack">返回</el-button>
    </div>
    <div v-loading="loading" class="post-content">
      <template v-if="post">
        <div class="post-badges">
          <span v-if="post.isPinned" class="badge badge-pin">置顶</span>
          <span v-if="post.isGlobalPinned" class="badge badge-global">全局置顶</span>
          <span v-if="post.isFeatured" class="badge badge-featured">精华</span>
          <span v-if="post.type==='REWARD'&&post.rewardPoints>0" class="badge badge-reward">悬赏 {{ post.rewardPoints }} 积分</span>
          <span v-if="post.type==='REWARD'&&post.rewardPoints===0" class="badge badge-resolved">悬赏已采纳</span>
        </div>
        <h1 class="post-title">{{ post.title }}</h1>
        <div class="post-meta">
          <span class="author" style="cursor:pointer;color:#409eff" @click="goToUserProfile(post.authorId)">{{ post.authorUsername||'匿名' }}</span>
          <span class="time">{{ formatDate(post.createdAt) }}</span>
        </div>
        <div class="post-body" v-html="renderedContent"></div>
        <div class="post-actions">
          <div class="action-left">
            <el-button :type="liked?'primary':'default'" text @click="handleLike">
              👍 {{ post.likeCount||0 }}
            </el-button>
            <el-button :type="favorited?'primary':'default'" text @click="handleFavorite">
              {{ favorited ? '⭐' : '☆' }} {{ post.favoriteCount||0 }}
            </el-button>
            <span class="comment-count">💬 {{ post.commentCount||0 }}</span>
          </div>
          <div v-if="canManage" class="action-right">
            <el-button type="primary" text @click="openEditDialog">编辑</el-button>
            <el-button type="danger" text @click="handleDelete">删除</el-button>
          </div>
        </div>
        <el-dialog v-model="editDialogVisible" title="编辑帖子" width="600px">
          <el-form :model="editForm" label-width="80px">
            <el-form-item label="帖子标题"><el-input v-model="editForm.title" placeholder="请输入标题" maxlength="150" show-word-limit /></el-form-item>
            <el-form-item label="帖子内容"><el-input v-model="editForm.content" type="textarea" :rows="6" placeholder="请输入内容" /></el-form-item>
          </el-form>
          <template #footer><el-button @click="editDialogVisible=false">取消</el-button><el-button type="primary" @click="handleEdit">保存</el-button></template>
        </el-dialog>
        <div class="comments-section">
          <h3 class="comments-title">评论 ({{ comments.length }})</h3>
          <div class="comment-input-area" :class="{ 'comment-sticky': replyTarget }">
            <div style="flex:1;">
              <div v-if="replyTarget" class="reply-bar">回复 <strong>@{{ replyTarget.username }}</strong> <el-button type="danger" size="small" text @click="replyTarget=null">取消</el-button></div>
              <div class="comment-toolbar">
                <label class="comment-tool-btn" title="插入图片" :class="{ uploading: commentImageUploading }">
                  <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><rect x="3" y="3" width="18" height="18" rx="2" stroke="currentColor" stroke-width="1.5"/><circle cx="8.5" cy="8.5" r="1.5" fill="currentColor"/><path d="M21 15l-5-5L5 21h16v-6z" stroke="currentColor" stroke-width="1.5" stroke-linejoin="round"/></svg>
                  <input type="file" accept="image/*" @change="handleCommentImage" />
                </label>
                <el-popover placement="right-start" :width="320" trigger="click" popper-class="emoji-popover">
                  <template #reference><button class="comment-tool-btn" title="表情">😊</button></template>
                  <div class="emoji-grid-popup"><button v-for="e in emojiList" :key="e" class="emoji-chip" type="button" @click="commentInput+=e">{{ e }}</button></div>
                </el-popover>
              </div>
              <el-input v-model="commentInput" type="textarea" :rows="3" :placeholder="replyTarget?'回复 @'+replyTarget.username+'...':'写下你的评论...'" resize="none" />
            </div>
            <el-button type="primary" :loading="commentSubmitting" @click="handleSubmitComment">{{ replyTarget?'回复':'发表评论' }}</el-button>
          </div>
          <div v-loading="commentLoading" class="comment-list">
            <div v-if="comments.length===0" class="comment-empty">暂无评论，快来发表第一楼吧</div>
            <template v-for="comment in comments" :key="comment.id">
              <div class="comment-item" :id="'comment-'+comment.id">
                <div class="comment-header">
                  <span class="comment-author" style="cursor:pointer;color:#409eff" @click="goToUserProfile(comment.authorId)">{{ comment.authorUsername||'匿名' }}</span>
                  <span class="comment-time">{{ formatDate(comment.createdAt) }}</span>
                  <el-tag v-if="comment.isAccepted===1" size="small" type="success">已采纳</el-tag>
                </div>
                <div class="comment-body"><span v-html="parseContent(comment.content)"></span></div>
                <div class="comment-actions">
                  <el-button size="small" text @click="startReply(comment)">回复</el-button>
                  <template v-if="isAuthor&&comment.isAccepted!==1&&post.type==='REWARD'&&post.rewardPoints>0"><el-button type="success" size="small" text @click="handleAcceptComment(comment.id)">采纳</el-button></template>
                  <template v-if="comment.authorId===currentUser.id||currentUser.globalRole==='SYS_ADMIN'"><el-button type="danger" size="small" text @click="handleDeleteComment(comment.id)">删除</el-button></template>
                </div>
                <div v-if="comment.children&&comment.children.length" class="comment-replies">
                  <div v-for="child in comment.children" :key="child.id" class="reply-item">
                    <div class="comment-header">
                      <span class="comment-author" style="cursor:pointer;color:#409eff" @click="goToUserProfile(child.authorId)">{{ child.authorUsername||'匿名' }}</span>
                      <span v-if="child.parentAuthorUsername" style="font-size:12px;color:#909399;">回复 @{{ child.parentAuthorUsername }}</span>
                      <span class="comment-time">{{ formatDate(child.createdAt) }}</span>
                    </div>
                    <div class="comment-body"><span v-html="parseContent(child.content)"></span></div>
                    <div class="comment-actions">
                      <el-button size="small" text @click="startReply(comment)">回复</el-button>
                      <template v-if="isAuthor&&child.isAccepted!==1&&post.type==='REWARD'&&post.rewardPoints>0"><el-button type="success" size="small" text @click="handleAcceptComment(child.id)">采纳</el-button></template>
                      <template v-if="child.authorId===currentUser.id||currentUser.globalRole==='SYS_ADMIN'"><el-button type="danger" size="small" text @click="handleDeleteComment(child.id)">删除</el-button></template>
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
.post-detail-container { max-width: 900px; margin: 0 auto; }
.post-detail-header { margin-bottom: 16px; }
.post-content { background: #fff; border-radius: 12px; padding: 24px; }
.post-badges { display: flex; gap: 6px; margin-bottom: 12px; }
.badge { font-size: 12px; padding: 2px 6px; border-radius: 4px; }
.badge-pin { background: #e6f0ff; color: #409eff; }
.badge-global { background: #fff3e6; color: #ff9900; }
.badge-featured { background: #fff1e6; color: #ff6600; }
.badge-reward { background: #e6fff0; color: #52c41a; }
.badge-resolved { background: #f5f5f5; color: #909399; }
.post-title { margin: 0 0 12px 0; font-size: 24px; font-weight: 600; color: #303133; }
.post-meta { display: flex; gap: 16px; font-size: 13px; color: #909399; margin-bottom: 20px; }
.post-body { font-size: 15px; line-height: 1.8; color: #303133; white-space: pre-wrap; margin-bottom: 20px; min-height: 100px; }
.post-actions { display: flex; justify-content: space-between; align-items: center; padding: 16px 0; border-top: 1px solid #f0f0f0; }
.action-left { display: flex; align-items: center; gap: 8px; }
.action-right { display: flex; gap: 8px; }
.comment-count { display: flex; align-items: center; color: #909399; font-size: 14px; }
.comments-section { margin-top: 24px; padding-top: 24px; border-top: 1px solid #f0f0f0; }
.comments-title { margin: 0 0 16px 0; font-size: 16px; color: #303133; }
.comment-input-area { display: flex; gap: 12px; align-items: flex-end; margin-bottom: 20px; }
.comment-input-area :deep(.el-textarea__inner) { resize: none; }
.comment-sticky { position: sticky; bottom: 0; z-index: 10; background: #fff; padding-top: 12px; padding-bottom: 12px; border-top: 1px solid #f0f0f0; margin-bottom: 0; }
.comment-toolbar { display: flex; gap: 6px; padding: 4px 0; }
.comment-tool-btn { display: inline-flex; align-items: center; justify-content: center; width: 30px; height: 30px; border: 1px solid #e4e7ed; border-radius: 8px; background: #fff; color: #909399; cursor: pointer; position: relative; transition: all .15s; font-size: 16px; }
.comment-tool-btn:hover { border-color: #c6e2ff; background: #ecf5ff; color: #409eff; }
.comment-tool-btn.uploading { opacity: .5; pointer-events: none; }
.comment-tool-btn input { position: absolute; inset: 0; opacity: 0; cursor: pointer; }
.comment-list { display: flex; flex-direction: column; gap: 16px; }
.comment-empty { text-align: center; color: #909399; padding: 40px 0; }
.comment-item { padding: 16px; background: #f9f9f9; border-radius: 8px; }
.comment-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.comment-author { font-size: 14px; font-weight: 500; }
.comment-time { font-size: 12px; color: #909399; }
.comment-body { font-size: 14px; line-height: 1.6; color: #303133; margin-bottom: 8px; white-space: pre-wrap; }
.comment-actions { display: flex; gap: 8px; }
.reply-bar { padding: 6px 10px; background: #ecf5ff; border-radius: 4px 4px 0 0; font-size: 13px; color: #409eff; margin-bottom: 0; display: flex; align-items: center; justify-content: space-between; }
.comment-highlight { animation: comment-pulse .4s ease-out 3; box-shadow: 0 0 0 3px rgba(64,158,255,.4); border-radius: 8px; }
@keyframes comment-pulse { 0%,100% { background: transparent; } 50% { background: rgba(64,158,255,.1); } }
.comment-replies { margin-top: 12px; padding-left: 24px; border-left: 3px solid #e4e7ed; }
.reply-item { padding: 12px; background: #fff; border-radius: 6px; margin-bottom: 8px; }
.reply-item:last-child { margin-bottom: 0; }
</style>
