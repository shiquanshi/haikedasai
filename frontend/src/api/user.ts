import request from './request'

export interface LoginParams {
  username: string
  password: string
}

export interface RegisterParams {
  username: string
  password: string
  email?: string
}

export const userApi = {
  // 用户登录
  login(params: LoginParams) {
    return request({
      url: '/api/user/login',
      method: 'post',
      data: params
    })
  },

  // 用户注册
  register(params: RegisterParams) {
    return request({
      url: '/api/user/register',
      method: 'post',
      data: params
    })
  },

  // 获取当前用户信息
  getCurrentUser() {
    return request({
      url: '/api/user/info',
      method: 'get'
    })
  }
}