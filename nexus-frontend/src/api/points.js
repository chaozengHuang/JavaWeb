import request from './request'

// 签到
export const checkIn = () => request.post('/api/points/check-in')

export const getCheckInStatus = () => request.get('/api/points/check-in/status')

// 充值
export const recharge = (amount) => request.post('/api/points/recharge', { amount })

export const getRechargeList = (page = 1, size = 10) =>
  request.get('/api/points/recharge/list', { params: { page, size } })
