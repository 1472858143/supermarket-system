import request from './request'

export function listStockBatches(skuId) {
  return request.get(`/skus/${skuId}/stock-batches`)
}

export function lockStockBatch(skuId, batchId) {
  return request.put(`/skus/${skuId}/stock-batches/${batchId}/lock`)
}

export function unlockStockBatch(skuId, batchId) {
  return request.put(`/skus/${skuId}/stock-batches/${batchId}/unlock`)
}

export function damageStockBatch(skuId, batchId, payload) {
  return request.post(`/skus/${skuId}/stock-batches/${batchId}/damage`, payload)
}

export function closeStockBatch(skuId, batchId) {
  return request.put(`/skus/${skuId}/stock-batches/${batchId}/close`)
}
