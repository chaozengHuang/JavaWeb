import request from './request'

export const getBoardList = () => {
  return request.get('/board/list')
}

export const createBoard = (name, description, creatorId) => {
  return request.post('/board/create', null, {
    params: { name, description, creatorId },
  })
}