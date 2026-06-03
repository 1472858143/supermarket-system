import request from './request'

export function getCategoryTree() {
  return request.get('/categories')
}

export function createCategory(data) {
  return request.post('/categories', data)
}

export function updateCategory(id, data) {
  return request.put(`/categories/${id}`, data)
}

export function updateCategorySortOrder(items) {
  return request.put('/categories/sort-order', items)
}

export function deleteCategory(id) {
  return request.delete(`/categories/${id}`)
}
