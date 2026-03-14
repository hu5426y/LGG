<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const badges = ref([])
const form = reactive({
  code: '',
  name: '',
  description: '',
  icon: '',
  ruleType: 'TOTAL_DISTANCE',
  ruleThreshold: 1
})

async function load() {
  try {
    const result = await http.get('/admin/badges')
    badges.value = Array.isArray(result.data) ? result.data : []
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function createBadge() {
  try {
    await http.post('/admin/badges', form)
    ElMessage.success('Badge created')
    Object.assign(form, {
      code: '',
      name: '',
      description: '',
      icon: '',
      ruleType: 'TOTAL_DISTANCE',
      ruleThreshold: 1
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
      <h3 class="section-title">Create Badge</h3>
      <div class="form-stack">
        <el-input v-model="form.code" placeholder="Unique code" />
        <el-input v-model="form.name" placeholder="Badge name" />
        <el-input v-model="form.description" placeholder="Description" />
        <el-input v-model="form.icon" placeholder="Icon key" />
        <el-select v-model="form.ruleType" placeholder="Rule type">
          <el-option label="Total distance" value="TOTAL_DISTANCE" />
          <el-option label="Run count" value="RUN_COUNT" />
          <el-option label="Points" value="POINTS" />
          <el-option label="Check-in streak" value="CHECKIN_STREAK" />
          <el-option label="Plan completion" value="PLAN_COMPLETION" />
          <el-option label="Squad participation" value="SQUAD_PARTICIPATION" />
        </el-select>
        <el-input-number v-model="form.ruleThreshold" :min="1" />
        <el-button class="accent-button" @click="createBadge">Save Badge</el-button>
      </div>
    </section>

    <section class="page-card">
      <h3 class="section-title">Badge Library</h3>
      <div class="timeline-list">
        <div v-for="badge in badges" :key="badge.id" class="timeline-item">
          <strong>{{ badge.name }}</strong>
          <div>{{ badge.ruleType }} / {{ badge.ruleThreshold }}</div>
          <small>{{ badge.description }}</small>
        </div>
      </div>
    </section>
  </div>
</template>
