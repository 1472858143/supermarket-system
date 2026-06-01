import request from './request'

export function listStockchecks(params) {
  return request.get('/stockchecks', { params })
}

export function createStockcheck(data) {
  return request.post('/stockchecks', data)
}

export function getStockcheck(id) {
  return request.get(`/stockchecks/${id}`)
}

export function updateStockcheckItems(id, data) {
  return request.put(`/stockchecks/${id}/items`, data)
}

export function completeStockcheck(id) {
  return request.post(`/stockchecks/${id}/complete`)
}
