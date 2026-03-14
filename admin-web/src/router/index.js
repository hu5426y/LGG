import { createRouter, createWebHistory } from 'vue-router'
import { useSessionStore } from '../stores/session'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import StudentsView from '../views/StudentsView.vue'
import ModerationView from '../views/ModerationView.vue'
import ActivitiesView from '../views/ActivitiesView.vue'
import RewardsView from '../views/RewardsView.vue'
import LogsView from '../views/LogsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: LoginView, meta: { public: true } },
    { path: '/', redirect: '/dashboard' },
    { path: '/dashboard', component: DashboardView },
    { path: '/students', component: StudentsView },
    { path: '/moderation', component: ModerationView },
    { path: '/activities', component: ActivitiesView },
    { path: '/rewards', component: RewardsView },
    { path: '/logs', component: LogsView }
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
