import request from './request'

export const uploadImage = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

export const uploadFile = (file) => {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/api/upload/file', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}
