<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const logs = ref([])

async function load() {
  try {
    const result = await http.get('/admin/logs')
    logs.value = Array.isArray(result.data) ? result.data : []
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(load)
</script>

<template>
  <section class="page-card">
    <h3 class="section-title">操作日志</h3>
    <el-table :data="logs" style="width: 100%">
      <el-table-column prop="createdAt" label="时间" min-width="180" />
      <el-table-column prop="action" label="动作" width="180" />
      <el-table-column prop="targetType" label="对象类型" width="140" />
      <el-table-column prop="targetId" label="对象 ID" width="120" />
      <el-table-column prop="detail" label="详情" min-width="260" show-overflow-tooltip />
    </el-table>
  </section>
</template>
