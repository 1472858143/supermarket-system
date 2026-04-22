import request from './request'

export function listStocks(params) {
  return request.get('/stocks', { params })
}

export function updateStockLimit(productId, data) {
  return request.put(`/stocks/${productId}/limit`, data)
}
