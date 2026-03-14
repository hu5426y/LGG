<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from './stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()

const isLoginPage = computed(() => route.path === '/login')

const navItems = [
  { path: '/dashboard', label: 'Overview' },
  { path: '/metrics', label: 'Metrics' },
  { path: '/students', label: 'Students' },
  { path: '/moderation', label: 'Moderation' },
  { path: '/activities', label: 'Activities' },
  { path: '/rewards', label: 'Rewards' },
  { path: '/logs', label: 'Audit Logs' }
]

function logout() {
  session.logout()
  router.push('/login')
}
</script>

<template>
  <div v-if="isLoginPage" class="login-shell">
    <router-view />
  </div>
  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-kicker">Campus Run</span>
        <h1>Campus Run Console</h1>
        <p>Operations, moderation, activity management, and running analytics in one place.</p>
      </div>

      <nav class="nav-list">
        <router-link v-for="item in navItems" :key="item.path" :to="item.path" class="nav-item">
          {{ item.label }}
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="user-card">
          <strong>{{ session.user?.displayName || 'Admin' }}</strong>
          <span>{{ session.user?.username }}</span>
        </div>
        <button class="ghost-button" @click="logout">Sign Out</button>
      </div>
    </aside>

    <main class="main-panel">
      <header class="page-header">
        <div>
          <span class="page-tag">Operations Console</span>
          <h2>{{ route.meta?.title || 'Campus Run Console' }}</h2>
        </div>
        <div class="header-pill">Metrics Enabled</div>
      </header>
      <router-view />
    </main>
  </div>
</template>
