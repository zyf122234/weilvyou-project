import request from '@/utils/request'

export function createOrder(data) {
  return request.post('/order/orders', data)
}

// 支付订单
export function payOrder(id) {
  return request.patch(`/order/orders/${id}/pay`)
}

// 取消订单
export function cancelOrder(id) {
  return request.patch(`/order/orders/${id}/cancel`)
}

// 我的订单
export function listMyOrders(params) {
  return request.get('/order/orders/mine', { params })
}

// 本店铺订单
export function listMerchantOrders(params) {
  return request.get('/order/orders/merchant', { params })
}
