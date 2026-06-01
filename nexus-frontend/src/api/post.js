import request from './request'

export const getPostList = (params) => {
  return request.get('/post', { params })
}

export const getPostDetail = (id) => {
  return request.get(`/post/${id}`)
}

export const getPostsByUser = (userId, page = 1, size = 10) => {
  return request.get(`/post/user/${userId}`, { params: { page, size } })
}

export const createPost = (data) => {
  return request.post('/post', data)
}

export const updatePost = (id, data) => {
  return request.put(`/post/${id}`, data)
}

export const deletePost = (id) => {
  return request.delete(`/post/${id}`)
}

export const setPin = (id, status) => {
  return request.put(`/post/${id}/pin`, null, { params: { status } })
}

export const setGlobalPin = (id, status) => {
  return request.put(`/post/${id}/global-pin`, null, { params: { status } })
}

export const setFeature = (id, status) => {
  return request.put(`/post/${id}/feature`, null, { params: { status } })
}