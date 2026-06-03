import request from './request'

export const getBoardList = () => {
  return request.get('/board/list')
}

export const createBoard = (name, description, creatorId) => {
  return request.post('/board/create', null, {
    params: { name, description, creatorId },
  })
}

// 吧详情（含吧主、管理员、统计等）
export const getBoardDetail = (boardId) => request.get(`/board/${boardId}`)

// 更新吧描述
export const updateBoard = (boardId, description) =>
  request.put(`/board/${boardId}`, { description })

// 删除吧
export const deleteBoard = (boardId) => request.delete(`/board/${boardId}`)

// 隐藏帖子
export const hidePost = (boardId, postId) =>
  request.put(`/board/${boardId}/posts/${postId}/hide`)

// 恢复帖子
export const showPost = (boardId, postId) =>
  request.put(`/board/${boardId}/posts/${postId}/show`)

// 吧主删除帖子
export const deleteBoardPost = (boardId, postId) =>
  request.delete(`/board/${boardId}/posts/${postId}`)

// 禁言用户
export const muteUser = (boardId, userId) =>
  request.put(`/board/${boardId}/mute/${userId}`)

// 取消禁言
export const unmuteUser = (boardId, userId) =>
  request.delete(`/board/${boardId}/mute/${userId}`)

// 获取管理员列表
export const getAdmins = (boardId) => request.get(`/board/${boardId}/admins`)

// 任命管理员
export const addAdmin = (boardId, userId) =>
  request.put(`/board/${boardId}/admins/${userId}`)

// 移除管理员
export const removeAdmin = (boardId, userId) =>
  request.delete(`/board/${boardId}/admins/${userId}`)

// 成员列表
export const getMembers = (boardId, keyword = '', page = 1, size = 20) =>
  request.get(`/board/${boardId}/members`, { params: { keyword, page, size } })

// 当前用户在该吧的角色
export const getMyRole = (boardId) => request.get(`/board/${boardId}/my-role`)

// 吧头像上传
export const uploadBoardAvatar = (boardId, file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post(`/board/${boardId}/avatar`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// 加入吧
export const joinBoard = (boardId) => request.post(`/board/${boardId}/join`)

// 离开吧
export const leaveBoard = (boardId) => request.delete(`/board/${boardId}/leave`)

// 活跃度排行榜
export const getLeaderboard = (boardId) => request.get(`/board/${boardId}/leaderboard`)

// 我的贴吧
export const getMyBoards = () => request.get('/board/my-boards')

// 回收站
export const getTrashPosts = (boardId) => request.get(`/board/${boardId}/trash`)
export const recoverPost = (boardId, postId) => request.put(`/board/${boardId}/posts/${postId}/recover`)
export const getPostCommentsManage = (boardId, postId) =>
  request.get(`/board/${boardId}/posts/${postId}/comments-manage`)
export const deleteCommentManage = (boardId, commentId) =>
  request.delete(`/board/${boardId}/comments/${commentId}`)
export const recoverComment = (boardId, commentId) =>
  request.put(`/board/${boardId}/comments/${commentId}/recover`)

export const getTrashComments = (boardId) => request.get(`/board/${boardId}/trash-comments`)
