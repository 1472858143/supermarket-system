import request from './request'

export function listStockchecks(params) {
  return request.get('/stockchecks', { params })
}

export function createStockcheck(data) {
  return request.post('/stockchecks', data)
}
