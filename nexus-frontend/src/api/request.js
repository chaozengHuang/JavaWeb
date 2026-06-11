import axios from 'axios'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 请求拦截器：自动添加 token
request.interceptors.request.use(
  (config) => {
    const userStr = localStorage.getItem('user')
    if (userStr) {
      try {
        const { token } = JSON.parse(userStr)
        if (token) {
          config.headers.Authorization = `Bearer ${token}`
        }
      } catch {
        // ignore
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器：统一处理错误
request.interceptors.response.use(
  (response) => {
    const data = response.data
    // 检查业务状态码，非 200 都是错误
    if (data && typeof data.code === 'number' && data.code !== 200) {
      return Promise.reject(new Error(data.message || '请求失败'))
    }
    return data
  },
  (error) => {
    if (error.response) {
      const { status, data } = error.response
      if (data && data.message) {
        return Promise.reject(new Error(data.message))
      }
      if (status === 401) {
        return Promise.reject(new Error('登录已过期，请重新登录'))
      }
      if (status >= 500) {
        return Promise.reject(new Error('服务器错误，请稍后重试'))
      }
    }
    return Promise.reject(new Error('网络异常，请检查连接'))
  }
)

export default request