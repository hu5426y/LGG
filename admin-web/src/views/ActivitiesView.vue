<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const activities = ref([])
const tutorials = ref([])

const activityForm = reactive({
  title: '',
  location: '',
  startTime: '',
  endTime: '',
  registrationDeadline: '',
  coverUrl: '',
  description: '',
  maxCapacity: 100,
  status: 'PUBLISHED'
})

const tutorialForm = reactive({
  title: '',
  coverUrl: '',
  summary: '',
  content: '',
  published: true
})

async function load() {
  try {
    const [activityResult, tutorialResult] = await Promise.all([
      http.get('/activities'),
      http.get('/activities/tutorials')
    ])
    activities.value = Array.isArray(activityResult.data) ? activityResult.data : []
    tutorials.value = Array.isArray(tutorialResult.data) ? tutorialResult.data : []
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function createActivity() {
  try {
    await http.post('/admin/activities', activityForm)
    ElMessage.success('活动已创建')
    Object.assign(activityForm, {
      title: '',
      location: '',
      startTime: '',
      endTime: '',
      registrationDeadline: '',
      coverUrl: '',
      description: '',
      maxCapacity: 100,
      status: 'PUBLISHED'
    })
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function createTutorial() {
  try {
    await http.post('/admin/tutorials', tutorialForm)
    ElMessage.success('教程已创建')
    Object.assign(tutorialForm, {
      title: '',
      coverUrl: '',
      summary: '',
      content: '',
      published: true
    })
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(load)
</script>

<template>
  <div class="two-column">
    <section class="page-card">
      <h3 class="section-title">发布活动</h3>
      <div class="form-stack">
        <el-input v-model="activityForm.title" placeholder="活动标题" />
        <el-input v-model="activityForm.location" placeholder="地点" />
        <el-date-picker v-model="activityForm.startTime" type="datetime" placeholder="开始时间" value-format="YYYY-MM-DDTHH:mm:ss" />
        <el-date-picker v-model="activityForm.endTime" type="datetime" placeholder="结束时间" value-format="YYYY-MM-DDTHH:mm:ss" />
        <el-date-picker v-model="activityForm.registrationDeadline" type="datetime" placeholder="报名截止时间" value-format="YYYY-MM-DDTHH:mm:ss" />
        <el-input v-model="activityForm.coverUrl" placeholder="封面图地址" />
        <el-input v-model="activityForm.description" type="textarea" :rows="4" placeholder="活动说明" />
        <el-input-number v-model="activityForm.maxCapacity" :min="1" />
        <el-button class="accent-button" @click="createActivity">保存活动</el-button>
      </div>
    </section>

    <section class="page-card">
      <h3 class="section-title">发布跑步教程</h3>
      <div class="form-stack">
        <el-input v-model="tutorialForm.title" placeholder="教程标题" />
        <el-input v-model="tutorialForm.coverUrl" placeholder="封面图地址" />
        <el-input v-model="tutorialForm.summary" placeholder="摘要" />
        <el-input v-model="tutorialForm.content" type="textarea" :rows="6" placeholder="教程正文" />
        <el-button class="accent-button" @click="createTutorial">保存教程</el-button>
      </div>
    </section>

    <section class="page-card">
      <h3 class="section-title">已发布活动</h3>
      <div class="timeline-list">
        <div v-for="item in activities" :key="item.id" class="timeline-item">
          <strong>{{ item.title }}</strong>
          <div>{{ item.location }} · {{ item.startTime }}</div>
        </div>
      </div>
    </section>

    <section class="page-card">
      <h3 class="section-title">跑步教程</h3>
      <div class="timeline-list">
        <div v-for="item in tutorials" :key="item.id" class="timeline-item">
          <strong>{{ item.title }}</strong>
          <div>{{ item.summary }}</div>
        </div>
      </div>
    </section>
  </div>
</template>
