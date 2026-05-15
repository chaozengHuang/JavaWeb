import request from './request'

export const login = (username, password) => {
  return request.post('/user/login', null, {
    params: { username, password },
  })
}

export const register = (username, password) => {
  return request.post('/user/register', null, {
    params: { username, password },
  })
}