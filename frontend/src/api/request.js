import axios from 'axios'
import { getToken, removeStoredUser, removeToken } from '../utils/token'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 12000
})

request.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const body = response.data
    if (!body || typeof body.code === 'undefined') {
      return body
    }
    if (body.code === 0) {
      return body.data
    }
    if (body.code === 401) {
      removeToken()
      removeStoredUser()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    const error = new Error(body.message || '请求失败')
    error.code = body.code
    throw error
  },
  (error) => {
    const body = error.response?.data
    if (body?.code === 401) {
      removeToken()
      removeStoredUser()
      if (window.location.pathname !== '/login') {
        window.location.href = '/login'
      }
    }
    throw new Error(body?.message || error.message || '网络异常')
  }
)

export default request
