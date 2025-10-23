import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import ImageGenerator from '../views/ImageGenerator.vue'
import SharePlaza from '../views/SharePlaza.vue'
import ShareDetail from '../views/ShareDetail.vue'
import MobileHome from '../views/mobile/MobileHome.vue'
import MobileLogin from '../views/mobile/MobileLogin.vue'
import MobileImageGenerator from '../views/mobile/MobileImageGenerator.vue'
import MobileSharePlaza from '../views/mobile/MobileSharePlaza.vue'
import { useUserStore } from '../stores/user'
import { isMobileDevice } from '../utils/device'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'home',
      component: Home,
      meta: { requiresAuth: true }
    },
    {
      path: '/login',
      name: 'login',
      component: Login
    },
    {
      path: '/image-generator',
      name: 'imageGenerator',
      component: ImageGenerator,
      meta: { requiresAuth: true }
    },
    {
      path: '/share-plaza',
      name: 'sharePlaza',
      component: SharePlaza,
      meta: { requiresAuth: true }
    },
    {
      path: '/share-detail',
      name: 'shareDetail',
      component: ShareDetail,
      meta: { requiresAuth: true }
    },
    // 手机端路由
    {
      path: '/mobile',
      name: 'mobileHome',
      component: MobileHome,
      meta: { requiresAuth: true }
    },
    {
      path: '/mobile/login',
      name: 'mobileLogin',
      component: MobileLogin
    },
    {
      path: '/mobile/image-generator',
      name: 'mobileImageGenerator',
      component: MobileImageGenerator,
      meta: { requiresAuth: true }
    },
    {
      path: '/mobile/share-plaza',
      name: 'mobileSharePlaza',
      component: MobileSharePlaza,
      meta: { requiresAuth: true }
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  // 判断是否是手机端路由
  const isMobileRoute = to.path.startsWith('/mobile')
  // 检测当前设备类型
  const isMobile = isMobileDevice()
  
  // 自动路由:根据设备类型自动跳转到对应的页面
  // PC访问手机端路由,跳转到对应的PC端路由
  if (!isMobile && isMobileRoute) {
    if (to.path === '/mobile') {
      return next('/')
    } else if (to.path === '/mobile/login') {
      return next('/login')
    } else if (to.path === '/mobile/image-generator') {
      return next('/image-generator')
    }
  }
  
  // 手机访问PC端路由,跳转到对应的手机端路由
  if (isMobile && !isMobileRoute) {
    if (to.path === '/') {
      return next('/mobile')
    } else if (to.path === '/login') {
      return next('/mobile/login')
    } else if (to.path === '/image-generator') {
      return next('/mobile/image-generator')
    } else if (to.path === '/share-plaza') {
      return next('/mobile/share-plaza')
    }
  }
  
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    // 需要登录但未登录,根据设备类型跳转到对应的登录页
    if (isMobile) {
      next('/mobile/login')
    } else {
      next('/login')
    }
  } else if ((to.path === '/login' || to.path === '/mobile/login') && userStore.isLoggedIn) {
    // 已登录访问登录页,根据设备类型跳转到对应的首页
    if (isMobile) {
      next('/mobile')
    } else {
      next('/')
    }
  } else {
    next()
  }
})

export default router