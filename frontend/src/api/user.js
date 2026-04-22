import request from './request'

export function listUsers(params) {
  return request.get('/users', { params })
}

export function listRoles() {
  return request.get('/users/roles')
}

export function createUser(data) {
  return request.post('/users', data)
}

export function updateUser(id, data) {
  return request.put(`/users/${id}`, data)
}

export function deleteUser(id) {
  return request.delete(`/users/${id}`)
}
