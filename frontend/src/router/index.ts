import { createRouter, createWebHistory } from 'vue-router'
import Home from '../views/Home.vue'
import Login from '../views/Login.vue'
import ImageGenerator from '../views/ImageGenerator.vue'
import MobileHome from '../views/mobile/MobileHome.vue'
import MobileLogin from '../views/mobile/MobileLogin.vue'
import MobileImageGenerator from '../views/mobile/MobileImageGenerator.vue'
import { useUserStore } from '../stores/user'

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
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    // 需要登录但未登录,跳转到登录页
    next('/login')
  } else if (to.path === '/login' && userStore.isLoggedIn) {
    // 已登录访问登录页,跳转到首页
    next('/')
  } else {
    next()
  }
})

export default router