import { createRouter, createWebHistory } from 'vue-router'
import { useSessionStore } from '../stores/session'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import MetricsView from '../views/MetricsView.vue'
import StudentsView from '../views/StudentsView.vue'
import ModerationView from '../views/ModerationView.vue'
import ActivitiesView from '../views/ActivitiesView.vue'
import RewardsView from '../views/RewardsView.vue'
import LogsView from '../views/LogsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { public: true, title: '登录' } },
    { path: '/', redirect: '/dashboard' },
    { path: '/dashboard', component: DashboardView, meta: { title: '运营概览' } },
    { path: '/metrics', component: MetricsView, meta: { title: '跑步数据' } },
    { path: '/students', component: StudentsView, meta: { title: '学生管理' } },
    { path: '/moderation', component: ModerationView, meta: { title: '内容审核' } },
    { path: '/activities', component: ActivitiesView, meta: { title: '活动管理' } },
    { path: '/rewards', component: RewardsView, meta: { title: '勋章奖励' } },
    { path: '/logs', component: LogsView, meta: { title: '审计日志' } }
  ]
})

router.beforeEach((to) => {
  const session = useSessionStore()
  if (!to.meta.public && !session.isAuthenticated) {
    return '/login'
  }
  if (to.path === '/login' && session.isAuthenticated) {
    return '/dashboard'
  }
})

export default router
