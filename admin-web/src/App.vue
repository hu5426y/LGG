<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from './stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()

const isLoginPage = computed(() => route.path === '/login')

const navItems = [
  { path: '/dashboard', label: '总览' },
  { path: '/students', label: '学生管理' },
  { path: '/moderation', label: '内容审核' },
  { path: '/activities', label: '活动教程' },
  { path: '/rewards', label: '勋章等级' },
  { path: '/logs', label: '审计日志' }
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
        <h1>校园乐跑后台</h1>
        <p>运营、审核与活动管理一体化控制台</p>
      </div>

      <nav class="nav-list">
        <router-link v-for="item in navItems" :key="item.path" :to="item.path" class="nav-item">
          {{ item.label }}
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <div class="user-card">
          <strong>{{ session.user?.displayName || '管理员' }}</strong>
          <span>{{ session.user?.username }}</span>
        </div>
        <button class="ghost-button" @click="logout">退出登录</button>
      </div>
    </aside>

    <main class="main-panel">
      <header class="page-header">
        <div>
          <span class="page-tag">Operations Console</span>
          <h2>{{ route.meta?.title || '校园乐跑运营后台' }}</h2>
        </div>
        <div class="header-pill">生产准备中</div>
      </header>
      <router-view />
    </main>
  </div>
</template>
