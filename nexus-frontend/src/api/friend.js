import request from './request'

// 发送好友请求
export const sendFriendRequest = (userId) => request.post(`/api/friend/request/${userId}`)

// 接受好友请求
export const acceptFriendRequest = (requestId) => request.put(`/api/friend/accept/${requestId}`)

// 拒绝好友请求
export const rejectFriendRequest = (requestId) => request.delete(`/api/friend/reject/${requestId}`)

// 删除好友
export const deleteFriend = (friendId) => request.delete(`/api/friend/${friendId}`)

// 好友列表
export const getFriendList = () => request.get('/api/friend/list')

// 待处理请求
export const getPendingRequests = () => request.get('/api/friend/requests')

// 待处理请求数量
export const getPendingCount = () => request.get('/api/friend/requests/count')

// 检查是否好友
export const isFriend = (userId) => request.get(`/api/friend/status/${userId}`)
