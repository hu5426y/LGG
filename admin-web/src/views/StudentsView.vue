<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const students = ref([])
const selected = ref(null)
const importing = ref(false)
const importFile = ref(null)

function statusLabel(status) {
  return status === 'DISABLED' ? '已禁用' : '正常'
}

async function load() {
  try {
    const result = await http.get('/admin/students')
    students.value = Array.isArray(result.data) ? result.data : []
    if (selected.value) {
      selected.value = students.value.find((item) => item.id === selected.value.id) || students.value[0] || null
    } else {
      selected.value = students.value[0] || null
    }
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function handleFileChange(event) {
  const [file] = event.target.files || []
  importFile.value = file || null
}

async function importStudents() {
  if (!importFile.value) {
    ElMessage.error('请先选择学生导入文件')
    return
  }
  const formData = new FormData()
  formData.append('file', importFile.value)
  try {
    importing.value = true
    const result = await http.post('/admin/students/import', formData, {
      headers: {
        'Content-Type': 'multipart/form-data'
      }
    })
    ElMessage.success(`导入完成：新增 ${result.data.created}，更新 ${result.data.updated}`)
    importFile.value = null
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    importing.value = false
  }
}

async function updateStatus(student, status) {
  try {
    await http.post(`/admin/students/${student.id}/status`, { status })
    ElMessage.success('学生状态已更新')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function resetBinding(student) {
  try {
    await http.post(`/admin/students/${student.id}/reset-binding`)
    ElMessage.success('微信绑定已重置')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(load)
</script>

<template>
  <div class="grid-stack">
    <section class="page-card">
      <h3 class="section-title">导入学生</h3>
      <div class="bullet-list">
        <div class="bullet-item">支持 `.csv` 和 `.xlsx` 文件。</div>
        <div class="bullet-item">推荐列：`student_no, display_name, college, class_name, username, password, status`。</div>
      </div>
      <div class="form-stack" style="margin-top: 18px;">
        <input type="file" accept=".csv,.xlsx" @change="handleFileChange" />
        <button class="accent-button" :disabled="importing" @click="importStudents">
          {{ importing ? '导入中...' : '导入学生名单' }}
        </button>
      </div>
    </section>

    <div class="two-column">
      <section class="table-wrap">
        <el-table :data="students" style="width: 100%" @row-click="selected = $event">
          <el-table-column prop="displayName" label="姓名" />
          <el-table-column prop="studentNo" label="学号" />
          <el-table-column prop="college" label="学院" />
          <el-table-column prop="status" label="状态">
            <template #default="{ row }">
              {{ statusLabel(row.status) }}
            </template>
          </el-table-column>
          <el-table-column prop="wechatBound" label="微信绑定">
            <template #default="{ row }">
              {{ row.wechatBound ? '已绑定' : '未绑定' }}
            </template>
          </el-table-column>
          <el-table-column prop="totalDistanceKm" label="累计里程" />
          <el-table-column prop="points" label="积分" />
        </el-table>
      </section>

      <section class="page-card" v-if="selected">
        <h3 class="section-title">学生详情</h3>
        <div class="bullet-list">
          <div class="bullet-item"><strong>姓名：</strong>{{ selected.displayName }}</div>
          <div class="bullet-item"><strong>学号：</strong>{{ selected.studentNo }}</div>
          <div class="bullet-item"><strong>班级：</strong>{{ selected.className }}</div>
          <div class="bullet-item"><strong>学院：</strong>{{ selected.college }}</div>
          <div class="bullet-item"><strong>状态：</strong>{{ statusLabel(selected.status) }}</div>
          <div class="bullet-item"><strong>微信绑定：</strong>{{ selected.wechatBound ? '已绑定' : '未绑定' }}</div>
          <div class="bullet-item"><strong>绑定时间：</strong>{{ selected.wechatBoundAt || '-' }}</div>
          <div class="bullet-item"><strong>累计里程：</strong>{{ selected.totalDistanceKm }} km</div>
          <div class="bullet-item"><strong>累计时长：</strong>{{ selected.totalDurationSeconds }} 秒</div>
          <div class="bullet-item"><strong>积分/等级：</strong>{{ selected.points }} / Lv.{{ selected.levelValue }}</div>
          <div class="bullet-item"><strong>个性签名：</strong>{{ selected.bio }}</div>
        </div>

        <div class="form-stack" style="margin-top: 18px;">
          <button
            class="ghost-button"
            @click="updateStatus(selected, selected.status === 'ACTIVE' ? 'DISABLED' : 'ACTIVE')"
          >
            {{ selected.status === 'ACTIVE' ? '禁用学生' : '启用学生' }}
          </button>
          <button class="ghost-button" @click="resetBinding(selected)">重置微信绑定</button>
        </div>
      </section>
    </div>
  </div>
</template>
