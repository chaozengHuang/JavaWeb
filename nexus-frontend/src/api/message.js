import request from './request'

export const getHistory = (userId) => {
  return request.get(`/api/message/history/${userId}`)
}

export const markAsRead = (userId) => {
  return request.put(`/api/message/read/${userId}`)
}

export const getUnreadCount = () => {
  return request.get('/api/message/unread')
}

export const getChatList = () => {
  return request.get('/api/message/chat-list')
}
