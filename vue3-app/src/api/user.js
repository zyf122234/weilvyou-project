import request from '@/utils/request'

export function login(data) {
  return request.post('/user/login', data)
}

export function register(data) {
  return request.post('/user/register', data)
}

export function logout() {
  return request.post('/user/logout')
}

export function getUserInfo() {
  return request.get('/user/info')
}

export function uploadAvatar(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/user/avatar', formData)
}

export function updateUserInfo(data) {
  return request.put('/user/info', data)
}

export function rechargeBalance(data) {
  return request.post('/user/recharge', data)
}

export function applyForMerchant() {
  return request.post('/user/apply-merchant')
}

export function getMerchantApplication() {
  return request.get('/user/merchant-application')
}
