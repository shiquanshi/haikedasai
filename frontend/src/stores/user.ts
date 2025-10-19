import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi, type LoginParams, type RegisterParams } from '../api/user'
import { ElMessage } from 'element-plus'

export interface UserInfo {
  id: number
  username: string
  email?: string
  tenantId: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token')?.trim() || '')
  const userInfo = ref<UserInfo | null>(null)
  const isLoggedIn = computed(() => !!token.value)

  // 登录
  const login = async (params: LoginParams) => {
    try {
      const response = await userApi.login(params)
      if (response.token) {
        // trim()确保token没有多余的空格或换行符
        const cleanToken = response.token.trim()
        token.value = cleanToken
        userInfo.value = response.user
        localStorage.setItem('token', cleanToken)
        ElMessage.success('登录成功')
        return true
      }
      return false
    } catch (error) {
      ElMessage.error('登录失败')
      return false
    }
  }

  // 注册
  const register = async (params: RegisterParams) => {
    try {
      const response = await userApi.register(params)
      if (response.token) {
        // 注册成功后自动登录，trim()确保token没有多余的空格或换行符
        const cleanToken = response.token.trim()
        token.value = cleanToken
        userInfo.value = response.user
        localStorage.setItem('token', cleanToken)
        ElMessage.success('注册成功')
        return true
      }
      return false
    } catch (error) {
      ElMessage.error('注册失败')
      return false
    }
  }

  // 登出
  const logout = () => {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    ElMessage.success('已退出登录')
  }

  // 获取当前用户信息
  const fetchUserInfo = async () => {
    try {
      const response = await userApi.getCurrentUser()
      if (response.success) {
        userInfo.value = response.data
        return true
      }
      return false
    } catch (error) {
      return false
    }
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    login,
    register,
    logout,
    fetchUserInfo
  }
})