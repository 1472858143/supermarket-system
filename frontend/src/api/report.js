import request from './request'

export function getStockReport() {
  return request.get('/reports/stock')
}

export function getInboundReport() {
  return request.get('/reports/inbound')
}

export function getOutboundReport() {
  return request.get('/reports/outbound')
}

export function getWarningReport() {
  return request.get('/reports/warning')
}
