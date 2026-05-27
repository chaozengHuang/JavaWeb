import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

const isAdmin = () => {
  try {
    const stored = localStorage.getItem('user')
    if (!stored) return false
    const data = JSON.parse(stored)
    return data.user?.globalRole === 'SYS_ADMIN'
  } catch {
    return false
  }
}

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
    // ==================== 管理员路由 ====================
    {
      path: '/admin',
      component: () => import('@/views/admin/AdminLayout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      redirect: '/admin/users',
      children: [
        {
          path: 'users',
          name: 'adminUsers',
          component: () => import('@/views/admin/UserManage.vue'),
          meta: { requiresAuth: true, requiresAdmin: true },
        },
        {
          path: 'posts',
          name: 'adminPosts',
          component: () => import('@/views/admin/PostManage.vue'),
          meta: { requiresAuth: true, requiresAdmin: true },
        },
        {
          path: 'comments',
          name: 'adminComments',
          component: () => import('@/views/admin/CommentManage.vue'),
          meta: { requiresAuth: true, requiresAdmin: true },
        },
      ],
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

  if (to.meta.requiresAdmin && !isAdmin()) {
    ElMessage({ type: 'error', message: '权限不足，需要管理员权限' })
    next('/forum')
    return
  }

  if (to.path === '/auth' && isLoggedIn) {
    next('/forum')
    return
  }

  next()
})

export default router
