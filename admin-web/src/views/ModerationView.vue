<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'

const posts = ref([])

async function load() {
  try {
    const result = await http.get('/admin/posts/pending')
    posts.value = Array.isArray(result.data) ? result.data : []
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function act(postId, action) {
  try {
    await http.post(`/admin/posts/${postId}/review`, { action, remark: '后台审核操作' })
    ElMessage.success('操作成功')
    await load()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(load)
</script>

<template>
  <section class="page-card">
    <h3 class="section-title">待审核动态</h3>
    <el-table :data="posts" style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="content" label="内容" min-width="260" show-overflow-tooltip />
      <el-table-column prop="riskTags" label="风险标签" width="180" />
      <el-table-column prop="reportCount" label="举报数" width="100" />
      <el-table-column label="操作" width="260">
        <template #default="{ row }">
          <el-space>
            <el-button type="success" plain @click="act(row.id, 'APPROVE')">通过</el-button>
            <el-button type="danger" plain @click="act(row.id, 'REJECT')">拒绝</el-button>
            <el-button type="warning" plain @click="act(row.id, 'OFFLINE')">下架</el-button>
            <el-button plain @click="act(row.id, 'FEATURE')">置顶</el-button>
          </el-space>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>
