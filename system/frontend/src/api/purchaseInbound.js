import request from './request'

export function listPurchaseInbounds(params) {
  return request.get('/purchase-inbounds', { params })
}

export function getPurchaseInbound(id) {
  return request.get(`/purchase-inbounds/${id}`)
}

export function createPurchaseInboundDraft(data) {
  return request.post('/purchase-inbounds/drafts', data)
}

export function updatePurchaseInboundPlan(id, data) {
  return request.put(`/purchase-inbounds/${id}/plan`, data)
}

export function submitPurchaseInbound(id) {
  return request.post(`/purchase-inbounds/${id}/submit`)
}

export function approvePurchaseInbound(id) {
  return request.post(`/purchase-inbounds/${id}/approve`)
}

export function returnPurchaseInbound(id, data) {
  return request.post(`/purchase-inbounds/${id}/return`, data)
}

export function cancelPurchaseInbound(id, data) {
  return request.post(`/purchase-inbounds/${id}/cancel`, data)
}

export function closePurchaseInbound(id, data) {
  return request.post(`/purchase-inbounds/${id}/close`, data)
}

export function receivePurchaseInbound(id, data) {
  return request.post(`/purchase-inbounds/${id}/receipts`, data)
}
