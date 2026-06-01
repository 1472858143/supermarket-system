import request from './request'

export function listStockBatches(skuId) {
  return request.get(`/skus/${skuId}/stock-batches`)
}
