import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/auth',
    },
    {
      path: '/auth',
      name: 'auth',
      component: () => import('@/views/AuthView.vue'),
    },
    {
      path: '/forum',
      name: 'forum',
      component: () => import('@/views/ForumView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/profile',
      name: 'profile',
      component: () => import('@/views/ProfileView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/chat',
      name: 'chat',
      component: () => import('@/views/ChatView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/chat/:userId',
      name: 'chatWithUser',
      component: () => import('@/views/ChatView.vue'),
      meta: { requiresAuth: true },
    },
  ],
})

router.beforeEach((to, from, next) => {
  const userStr = localStorage.getItem('user')
  const isLoggedIn = !!userStr

  if (to.meta.requiresAuth && !isLoggedIn) {
    ElMessage({ type: 'warning', message: '请先登录' })
    next('/auth')
    return
  }

  if (to.path === '/auth' && isLoggedIn) {
    next('/forum')
    return
  }

  next()
})

export default router
