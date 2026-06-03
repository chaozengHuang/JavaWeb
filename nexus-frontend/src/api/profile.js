import request from './request'

// 个人信息
export const getProfile = () => request.get('/api/profile')

export const updateBio = (bio) => request.put('/api/profile/bio', { bio })

export const updateProfile = (fields) => request.put('/api/profile', fields)

export const uploadAvatar = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/profile/avatar', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

// 收藏
export const toggleFavorite = (postId) => request.post(`/api/profile/favorites/${postId}`)

export const removeFavorite = (postId) => request.delete(`/api/profile/favorites/${postId}`)

export const getFavorites = (page = 1, size = 10) =>
  request.get('/api/profile/favorites', { params: { page, size } })

export const isFavorited = (postId) => request.get(`/api/profile/favorites/${postId}/status`)

// 点赞
export const toggleLike = (postId) => request.post(`/api/profile/likes/${postId}`)

export const removeLike = (postId) => request.delete(`/api/profile/likes/${postId}`)

export const getLikes = (page = 1, size = 10) =>
  request.get('/api/profile/likes', { params: { page, size } })

export const isLiked = (postId) => request.get(`/api/profile/likes/${postId}/status`)

// 浏览历史
export const recordBrowse = (postId) => request.post(`/api/profile/history/${postId}`)

export const getHistory = (page = 1, size = 10) =>
  request.get('/api/profile/history', { params: { page, size } })

export const deleteHistory = (postId) => request.delete(`/api/profile/history/${postId}`)

export const clearHistory = () => request.delete('/api/profile/history')
