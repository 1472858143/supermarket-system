import request from './request'

export function listPurchaseInbounds(params) {
  return request.get('/purchase-inbounds', { params })
}

export function getPurchaseInbound(id) {
  return request.get(`/purchase-inbounds/${id}`)
}

export function createPurchaseInbound(data) {
  return request.post('/purchase-inbounds', data)
}
