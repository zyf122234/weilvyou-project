import request from '@/utils/request'

// ==================== 用户管理 ====================

export function listUsers(params) {
  return request.get('/user/admin/users', { params })
}

export function updateUserStatus(id, status) {
  return request.put(`/user/admin/users/${id}/status`, { status })
}

// ==================== 商户审核 ====================

export function listMerchantApplications(params) {
  return request.get('/user/admin/merchant-applications', { params })
}

export function reviewMerchantApplication(id, data) {
  return request.put(`/user/admin/merchant-applications/${id}`, data)
}

// ==================== 璁㈠崟绠＄悊 ====================

export function listAllOrders(params) {
  return request.get('/order/orders/admin/all', { params })
}
