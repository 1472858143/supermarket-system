import request from './request'

export function listStocks(params) {
  return request.get('/stocks', { params })
}

export function updateStockLimit(skuId, data) {
  return request.put(`/stocks/${skuId}/limit`, data)
}
