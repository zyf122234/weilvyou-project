import request from '@/utils/request'

export function getPublishedProduct(id) {
  return request.get(`/product/published/${id}`)
}

export function searchHotels(params) {
  return request.get('/product/hotel/search', { params })
}

export function uploadProductCover(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/product/cover', formData)
}

export function listMerchantProducts(params) {
  return request.get('/product/merchant', { params })
}

export function listAdminProducts(params) {
  return request.get('/product/admin', { params })
}

export function createProduct(data) {
  return request.post('/product', data)
}

export function updateProduct(id, data) {
  return request.put(`/product/${id}`, data)
}

export function updateProductStatus(id, status) {
  return request.patch(`/product/${id}/status`, null, { params: { status } })
}

export function deleteProduct(id) {
  return request.delete(`/product/${id}`)
}
