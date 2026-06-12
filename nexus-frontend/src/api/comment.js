
import request from './request'

export const getComments = (postId) => {
  return request.get('/comment', { params: { postId } })
}

export const createComment = (data) => {
  return request.post('/comment', data)
}

export const deleteComment = (id) => {
  return request.delete(`/comment/${id}`)
}

export const acceptComment = (id) => {
  return request.put(`/comment/${id}/accept`)
}