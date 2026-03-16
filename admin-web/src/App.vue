<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSessionStore } from './stores/session'

const route = useRoute()
const router = useRouter()
const session = useSessionStore()

const isLoginPage = computed(() => route.path === '/login')

const navItems = [
  { path: '/dashboard', label: '概览' },
  { path: '/metrics', label: '跑步数据' },
  { path: '/students', label: '学生管理' },
  { path: '/moderation', label: '内容审核' },
  { path: '/activities', label: '活动管理' },
  { path: '/rewards', label: '勋章奖励' },
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
        <span class="brand-kicker">校园乐跑</span>
        <h1>校园乐跑管理后台</h1>
        <p>统一处理运营概览、内容审核、活动管理、学生管理与跑步数据分析。</p>
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
          <span class="page-tag">管理后台</span>
          <h2>{{ route.meta?.title || '校园乐跑管理后台' }}</h2>
        </div>
        <div class="header-pill">统计功能已启用</div>
      </header>
      <router-view />
    </main>
  </div>
</template>
