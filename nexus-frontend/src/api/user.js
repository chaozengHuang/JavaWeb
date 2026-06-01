import request from './request'

export const login = (username, password) => {
  return request.post('/api/user/login', { username, password })
}

export const register = (username, password, confirmPassword) => {
  return request.post('/api/user/register', { username, password, confirmPassword })
}

export const getUserInfo = () => {
  return request.get('/api/user/info')
}

export const getUserInfoById = (userId) => {
  return request.get(`/api/user/${userId}`)
}

export const getPublicProfile = (userId) => {
  return request.get(`/api/profile/public/${userId}`)
}
