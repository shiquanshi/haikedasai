import axios from 'axios'
import { ElMessage } from 'element-plus'

const request = axios.create({
  baseURL: '/api',
  timeout: 60000, // 增加到60秒,适应AI生成等耗时操作
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    console.log('🚀 发送请求:', {
      url: config.url,
      method: config.method,
      baseURL: config.baseURL,
      fullURL: config.baseURL + config.url
    })
    // 从localStorage获取token并添加到请求头
    const token = localStorage.getItem('token')
    if (token) {
      // trim()移除token首尾可能的空格或换行符
      const cleanToken = token.trim()
      config.headers.Authorization = `Bearer ${cleanToken}`
      console.log('🔑 已添加token到请求头:', cleanToken.substring(0, 20) + '...')
    } else {
      console.log('⚠️ 未找到token，请求将不包含认证信息')
    }
    return config
  },
  (error) => {
    console.error('❌ 请求发送失败:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    console.log('✅ 收到响应:', {
      url: response.config.url,
      status: response.status,
      statusText: response.statusText
    })
    
    // 检查响应体中的code字段，如果不是200，视为业务错误
    if (response.data && response.data.code && response.data.code !== 200) {
      console.error('❌ 业务错误:', {
        url: response.config.url,
        code: response.data.code,
        message: response.data.message
      })
      const message = response.data.message || '请求失败'
      ElMessage.error(message)
      return Promise.reject(new Error(message))
    }
    
    return response.data
  },
  (error) => {
    console.error('❌ HTTP请求错误:', {
      url: error.config?.url,
      status: error.response?.status,
      data: error.response?.data
    })
    const message = error.response?.data?.message || '网络请求失败'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

export default request