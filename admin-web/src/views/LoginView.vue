<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useSessionStore } from '../stores/session'

const router = useRouter()
const session = useSessionStore()
const loading = ref(false)
const form = reactive({
  username: 'admin',
  password: 'admin123'
})

async function submit() {
  try {
    loading.value = true
    await session.login(form)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-wrap">
    <div class="login-card">
      <section class="login-hero">
        <span class="brand-kicker" style="background: rgba(255,255,255,0.16); color: white;">Ops Console</span>
        <h1>校园乐跑运营控制台</h1>
        <p>用于学生账号导入、内容审核、活动运营、勋章配置与审计日志管理的统一后台。</p>
      </section>

      <section class="login-form">
        <h2>登录后台</h2>
        <p>请使用管理员账号进入控制台。</p>
        <p>默认账号：`admin` / `admin123`</p>
        <el-form class="form-stack" @submit.prevent="submit">
          <el-input v-model="form.username" placeholder="管理员用户名" size="large" />
          <el-input v-model="form.password" type="password" show-password placeholder="密码" size="large" />
          <el-button class="accent-button" :loading="loading" @click="submit">进入系统</el-button>
        </el-form>
      </section>
    </div>
  </div>
</template>
