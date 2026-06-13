import request from './request'

// ==================== 用户管理 ====================

export const getUsers = (params) => {
  return request.get('/admin/users', { params })
}

export const getUserDetail = (userId) => {
  return request.get(`/admin/users/${userId}`)
}

export const resetUserPassword = (userId, newPassword) => {
  return request.put(`/admin/users/${userId}/password`, { newPassword })
}

export const updateUserStatus = (userId, status) => {
  return request.put(`/admin/users/${userId}/status`, { status })
}

// ==================== 帖子管理 ====================

export const getPosts = (params) => {
  return request.get('/admin/posts', { params })
}

export const getPostDetail = (postId) => {
  return request.get(`/admin/posts/${postId}`)
}

export const updatePostStatus = (postId, status) => {
  return request.put(`/admin/posts/${postId}/status`, null, { params: { status } })
}

export const batchUpdatePostStatus = (ids, status) => {
  return request.put('/admin/posts/batch-status', { ids, status })
}

// ==================== 评论管理 ====================

export const getComments = (params) => {
  return request.get('/admin/comments', { params })
}

export const getCommentDetail = (commentId) => {
  return request.get(`/admin/comments/${commentId}`)
}

export const updateCommentStatus = (commentId, status) => {
  return request.put(`/admin/comments/${commentId}/status`, null, { params: { status } })
}

export const batchUpdateCommentStatus = (ids, status) => {
  return request.put('/admin/comments/batch-status', { ids, status })
}

// ==================== 操作日志 ====================

export const getAdminLogs = (params) => {
  return request.get('/admin/logs', { params })
}

// ==================== 吧管理 ====================

export const getBoards = (params) => request.get('/admin/boards', { params })

export const updateBoardStatus = (boardId, status) =>
  request.put(`/admin/boards/${boardId}/status`, null, { params: { status } })

// ==================== 硬删除（物理清除） ====================

export const hardDeleteUser = (userId) => request.delete(`/admin/users/${userId}/hard`)
export const hardDeletePost = (postId) => request.delete(`/admin/posts/${postId}/hard`)
export const hardDeleteComment = (commentId) => request.delete(`/admin/comments/${commentId}/hard`)
export const hardDeleteBoard = (boardId) => request.delete(`/admin/boards/${boardId}/hard`)
export const deleteLog = (logId) => request.delete(`/admin/logs/${logId}`)

// ==================== 通知管理员 ====================

export const getNotifyAdmin = () => request.get('/admin/notify-admin')
