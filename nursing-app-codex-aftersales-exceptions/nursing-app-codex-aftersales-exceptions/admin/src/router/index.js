import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/login/index.vue'), meta: { public: true } },
  {
    path: '/',
    component: () => import('../layouts/admin-layout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/dashboard/index.vue'), meta: { title: '审核工作台' } },
      { path: 'reviews/:type', name: 'review-list', component: () => import('../views/reviews/index.vue'), meta: { title: '审核管理' } },
      { path: 'reviews/:type/:id', name: 'review-detail', component: () => import('../views/reviews/detail.vue'), meta: { title: '审核详情' } },
      { path: 'exceptions', name: 'exception-list', component: () => import('../views/exceptions/index.vue'), meta: { title: '异常监管' } },
      { path: 'exceptions/:id', name: 'exception-detail', component: () => import('../views/exceptions/detail.vue'), meta: { title: '异常详情' } },
      { path: 'complaints', name: 'complaint-list', component: () => import('../views/complaints/index.vue'), meta: { title: '投诉仲裁' } },
      { path: 'complaints/:id', name: 'complaint-detail', component: () => import('../views/complaints/detail.vue'), meta: { title: '仲裁详情' } },
      { path: 'notifications', name: 'notification-list', component: () => import('../views/notifications/index.vue'), meta: { title: '平台消息' } },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' },
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to) => {
  const token = localStorage.getItem('adminToken')
  if (!to.meta.public && !token) return { name: 'login', query: { redirect: to.fullPath } }
  if (to.name === 'login' && token) return { name: 'dashboard' }
  return true
})

export default router
