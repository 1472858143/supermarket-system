import request from './request'

export function getSystemInfo() {
  return request.get('/system/info')
}
