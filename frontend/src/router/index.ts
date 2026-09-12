import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: () => import('@/views/Login.vue') },
  { path: '/', redirect: '/kb' },
  { path: '/kb', name: 'kb', component: () => import('@/views/KnowledgeBase.vue'), meta: { requiresAuth: true } },
  { path: '/kb/:id/docs', name: 'docs', component: () => import('@/views/DocumentManage.vue'), meta: { requiresAuth: true } },
  { path: '/chat', name: 'chat', component: () => import('@/views/Chat.vue'), meta: { requiresAuth: true } }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.token) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.token) {
    return { name: 'kb' }
  }
  return true
})

export default router
