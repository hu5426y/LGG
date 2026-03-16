<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const overview = ref({
  today: {
    activeUsers: 0,
    totalDistanceKm: 0,
    averagePaceSeconds: 0,
    checkinUsers: 0
  },
  totals: {
    students: 0,
    joinedSquadMembers: 0
  },
  streakBuckets: []
})
const trends = ref([])
const trendChartRef = ref(null)
const streakChartRef = ref(null)
let trendChart
let streakChart

function formatPace(value) {
  const totalSeconds = Number(value || 0)
  const minutes = String(Math.floor(totalSeconds / 60)).padStart(2, '0')
  const seconds = String(totalSeconds % 60).padStart(2, '0')
  return `${minutes}:${seconds}`
}

async function load() {
  try {
    const [overviewResult, trendResult] = await Promise.all([
      http.get('/admin/metrics/overview'),
      http.get('/admin/metrics/trends')
    ])
    overview.value = overviewResult.data || overview.value
    trends.value = Array.isArray(trendResult.data) ? trendResult.data : []
    await nextTick()
    renderCharts()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function renderCharts() {
  if (trendChartRef.value) {
    trendChart = trendChart || echarts.init(trendChartRef.value)
    trendChart.setOption({
      tooltip: { trigger: 'axis' },
      legend: { data: ['活跃跑者', '当日里程', '计划完成数'] },
      xAxis: {
        type: 'category',
        data: trends.value.map((item) => item.date)
      },
      yAxis: [
        { type: 'value', name: '人数 / 次数' },
        { type: 'value', name: '公里' }
      ],
      series: [
        {
          name: '活跃跑者',
          type: 'line',
          smooth: true,
          data: trends.value.map((item) => item.activeUsers)
        },
        {
          name: '当日里程',
          type: 'bar',
          yAxisIndex: 1,
          data: trends.value.map((item) => Number(item.totalDistanceKm || 0).toFixed(1))
        },
        {
          name: '计划完成数',
          type: 'line',
          smooth: true,
          data: trends.value.map((item) => item.completedPlans)
        }
      ]
    })
  }

  if (streakChartRef.value) {
    streakChart = streakChart || echarts.init(streakChartRef.value)
    streakChart.setOption({
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: ['42%', '70%'],
          data: Array.isArray(overview.value.streakBuckets) ? overview.value.streakBuckets : [],
          label: { formatter: '{b}\n{c}' }
        }
      ]
    })
  }
}

onMounted(load)

onBeforeUnmount(() => {
  trendChart?.dispose()
  streakChart?.dispose()
})
</script>

<template>
  <div class="grid-stack">
    <section class="metrics-grid">
      <article class="metric-card">
        <div class="metric-label">今日活跃跑者</div>
        <div class="metric-value">{{ overview.today.activeUsers }}</div>
      </article>
      <article class="metric-card">
        <div class="metric-label">今日里程</div>
        <div class="metric-value">{{ Number(overview.today.totalDistanceKm || 0).toFixed(1) }} 公里</div>
      </article>
      <article class="metric-card">
        <div class="metric-label">平均配速</div>
        <div class="metric-value">{{ formatPace(overview.today.averagePaceSeconds) }}</div>
      </article>
      <article class="metric-card">
        <div class="metric-label">打卡人数</div>
        <div class="metric-value">{{ overview.today.checkinUsers }}</div>
      </article>
    </section>

    <section class="two-column">
      <div class="page-card">
        <h3 class="section-title">趋势图</h3>
        <div ref="trendChartRef" class="chart-box"></div>
      </div>

      <div class="page-card">
        <h3 class="section-title">连续打卡分布</h3>
        <div ref="streakChartRef" class="chart-box"></div>
        <div class="bullet-list">
          <div class="bullet-item">学生总数：{{ overview.totals.students }}</div>
          <div class="bullet-item">跑团成员数：{{ overview.totals.joinedSquadMembers }}</div>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.chart-box {
  width: 100%;
  height: 360px;
}
</style>
