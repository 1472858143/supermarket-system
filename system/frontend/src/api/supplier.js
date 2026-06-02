import request from './request'

export function listSuppliers(params) {
  return request.get('/suppliers', { params })
}

export function createSupplier(data) {
  return request.post('/suppliers', data)
}

export function updateSupplier(id, data) {
  return request.put(`/suppliers/${id}`, data)
}

export function deleteSupplier(id) {
  return request.delete(`/suppliers/${id}`)
}

export function listSupplierSkus(supplierId) {
  return request.get(`/suppliers/${supplierId}/skus`)
}

export function listEnabledSupplierSkus(supplierId) {
  return request.get(`/suppliers/${supplierId}/skus/enabled`)
}

export function createSupplierSku(supplierId, data) {
  return request.post(`/suppliers/${supplierId}/skus`, data)
}

export function updateSupplierSku(supplierId, bindingId, data) {
  return request.put(`/suppliers/${supplierId}/skus/${bindingId}`, data)
}

export function deleteSupplierSku(supplierId, bindingId) {
  return request.delete(`/suppliers/${supplierId}/skus/${bindingId}`)
}
