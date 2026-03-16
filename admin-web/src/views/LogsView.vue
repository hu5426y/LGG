<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { auditActionLabel, auditTargetTypeLabel, formatAuditDetail } from '../utils/display'

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
      <el-table-column label="操作类型" width="180">
        <template #default="{ row }">
          {{ auditActionLabel(row.action) }}
        </template>
      </el-table-column>
      <el-table-column label="对象类型" width="140">
        <template #default="{ row }">
          {{ auditTargetTypeLabel(row.targetType) }}
        </template>
      </el-table-column>
      <el-table-column prop="targetId" label="对象编号" width="120" />
      <el-table-column label="详情" min-width="260" show-overflow-tooltip>
        <template #default="{ row }">
          {{ formatAuditDetail(row.action, row.detail) }}
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>
