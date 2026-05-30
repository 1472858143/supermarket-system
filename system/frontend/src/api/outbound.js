import request from './request'

export function listOutbounds(params) {
  return request.get('/outbounds', { params })
}

export function createOutbound(data) {
  return request.post('/outbounds', data)
}
