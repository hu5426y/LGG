<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const defaultDashboard = {
  studentCount: 0,
  totalDistanceKm: 0,
  totalPoints: 0,
  pendingPosts: 0,
  latestLogs: []
}

const dashboard = ref({ ...defaultDashboard })
const totalDistanceKmText = computed(() => Number(dashboard.value.totalDistanceKm || 0).toFixed(1))
const latestLogs = computed(() => Array.isArray(dashboard.value.latestLogs) ? dashboard.value.latestLogs.slice(0, 5) : [])

async function load() {
  try {
    const result = await http.get('/admin/dashboard')
    dashboard.value = {
      ...defaultDashboard,
      ...(result.data || {}),
      latestLogs: Array.isArray(result.data?.latestLogs) ? result.data.latestLogs : []
    }
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(load)
</script>

<template>
  <div class="grid-stack">
    <section class="metrics-grid">
      <article class="metric-card">
        <div class="metric-label">学生总数</div>
        <div class="metric-value">{{ dashboard.studentCount }}</div>
      </article>
      <article class="metric-card">
        <div class="metric-label">累计里程</div>
        <div class="metric-value">{{ totalDistanceKmText }} km</div>
      </article>
      <article class="metric-card">
        <div class="metric-label">累计积分</div>
        <div class="metric-value">{{ dashboard.totalPoints }}</div>
      </article>
      <article class="metric-card">
        <div class="metric-label">待审动态</div>
        <div class="metric-value">{{ dashboard.pendingPosts }}</div>
      </article>
    </section>

    <section class="two-column">
      <div class="page-card">
        <h3 class="section-title">后台定位</h3>
        <div class="bullet-list">
          <div class="bullet-item">负责学生数据查看、动态审核、推荐干预、活动发布、勋章维护与日志审计。</div>
          <div class="bullet-item">当前版本使用 Spring Boot + Vue 3 + Element Plus，适合本地演示与二次开发。</div>
          <div class="bullet-item">接口统一走 `/api`，可和微信小程序端共享后端能力。</div>
        </div>
      </div>

      <div class="page-card">
        <h3 class="section-title">最近日志</h3>
        <div class="timeline-list">
          <div v-for="log in latestLogs" :key="log.id" class="timeline-item">
            <strong>{{ log.action }}</strong>
            <div>{{ log.targetType }} / {{ log.targetId || '-' }}</div>
            <small>{{ log.detail }}</small>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>
