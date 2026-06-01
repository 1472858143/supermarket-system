import request from './request'

export function listBrands(params) {
  return request.get('/brands', { params })
}

export function listBrandOptions() {
  return request.get('/brands/options')
}

export function createBrand(data) {
  return request.post('/brands', data)
}

export function updateBrand(id, data) {
  return request.put(`/brands/${id}`, data)
}

export function deleteBrand(id) {
  return request.delete(`/brands/${id}`)
}
