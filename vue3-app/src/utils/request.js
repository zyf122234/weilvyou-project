import axios from 'axios'
import { useUserStore } from '@/stores/user'

//公共请求配置
const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

//请求拦截器，添加token到请求头
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

//响应拦截器，处理响应数据和错误
request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code && res.code !== 200) {
      console.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    const { response } = error
    if (response) {
      const apiMessage = response.data?.message
      switch (response.status) {
        case 401:
          console.error('未授权，请重新登录')
          const userStore = useUserStore()
          userStore.clearUserState()
          window.location.href = '/login'
          break
        case 403:
          console.error(apiMessage || '拒绝访问')
          break
        case 404:
          console.error(apiMessage || '请求地址不存在')
          break
        case 500:
          console.error(apiMessage || '服务器内部错误')
          break
        default:
          console.error(apiMessage || '请求失败')
      }
      return Promise.reject(new Error(apiMessage || '请求失败'))
    } else {
      console.error('网络连接异常，请检查网络')
      return Promise.reject(new Error('网络连接异常，请检查网络'))
    }
  }
)

//导出请求实例
export default request
