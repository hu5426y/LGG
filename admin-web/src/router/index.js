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
    { path: '/login', component: LoginView, meta: { public: true, title: 'Login' } },
    { path: '/', redirect: '/dashboard' },
    { path: '/dashboard', component: DashboardView, meta: { title: 'Operations Overview' } },
    { path: '/metrics', component: MetricsView, meta: { title: 'Running Metrics' } },
    { path: '/students', component: StudentsView, meta: { title: 'Students' } },
    { path: '/moderation', component: ModerationView, meta: { title: 'Moderation' } },
    { path: '/activities', component: ActivitiesView, meta: { title: 'Activities' } },
    { path: '/rewards', component: RewardsView, meta: { title: 'Rewards' } },
    { path: '/logs', component: LogsView, meta: { title: 'Audit Logs' } }
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
