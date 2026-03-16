<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { badgeRuleTypeLabel } from '../utils/display'

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
    ElMessage.success('勋章已创建')
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
      <h3 class="section-title">创建勋章</h3>
      <div class="form-stack">
        <el-input v-model="form.code" placeholder="唯一编码" />
        <el-input v-model="form.name" placeholder="勋章名称" />
        <el-input v-model="form.description" placeholder="勋章说明" />
        <el-input v-model="form.icon" placeholder="图标标识" />
        <el-select v-model="form.ruleType" placeholder="奖励规则">
          <el-option label="累计里程" value="TOTAL_DISTANCE" />
          <el-option label="跑步次数" value="RUN_COUNT" />
          <el-option label="积分" value="POINTS" />
          <el-option label="连续打卡" value="CHECKIN_STREAK" />
          <el-option label="计划完成次数" value="PLAN_COMPLETION" />
          <el-option label="跑团参与" value="SQUAD_PARTICIPATION" />
        </el-select>
        <el-input-number v-model="form.ruleThreshold" :min="1" />
        <el-button class="accent-button" @click="createBadge">保存勋章</el-button>
      </div>
    </section>

    <section class="page-card">
      <h3 class="section-title">勋章库</h3>
      <div class="timeline-list">
        <div v-for="badge in badges" :key="badge.id" class="timeline-item">
          <strong>{{ badge.name }}</strong>
          <div>{{ badgeRuleTypeLabel(badge.ruleType) }} / {{ badge.ruleThreshold }}</div>
          <small>{{ badge.description }}</small>
        </div>
      </div>
    </section>
  </div>
</template>
