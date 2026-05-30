import request from './request'

export function listInbounds(params) {
  return request.get('/inbounds', { params })
}

export function createInbound(data) {
  return request.post('/inbounds', data)
}
