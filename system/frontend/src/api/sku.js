import request from './request'

export function listSkus(productId) {
  return request.get(`/products/${productId}/skus`)
}

export function createSku(productId, data) {
  return request.post(`/products/${productId}/skus`, data)
}

export function updateSku(productId, skuId, data) {
  return request.put(`/products/${productId}/skus/${skuId}`, data)
}

export function deleteSku(productId, skuId) {
  return request.delete(`/products/${productId}/skus/${skuId}`)
}

export function listUnits(productId, skuId) {
  return request.get(`/products/${productId}/skus/${skuId}/units`)
}

export function createUnit(productId, skuId, data) {
  return request.post(`/products/${productId}/skus/${skuId}/units`, data)
}

export function updateUnit(productId, skuId, unitId, data) {
  return request.put(`/products/${productId}/skus/${skuId}/units/${unitId}`, data)
}

export function deleteUnit(productId, skuId, unitId) {
  return request.delete(`/products/${productId}/skus/${skuId}/units/${unitId}`)
}
