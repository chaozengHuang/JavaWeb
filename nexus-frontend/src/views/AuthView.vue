<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const router = useRouter()
const API_BASE = 'http://localhost:8081'

const activeTab = ref('login')

const loginForm = ref({
  username: '',
  password: '',
})

const registerForm = ref({
  username: '',
  password: '',
})

const onLogin = async () => {
  try {
    const params = new URLSearchParams()
    params.append('username', loginForm.value.username)
    params.append('password', loginForm.value.password)
    const res = await axios.post(`${API_BASE}/user/login`, params)
    if (res.data && res.data.id) {
      localStorage.setItem('user', JSON.stringify(res.data))
      ElMessage({ type: 'success', message: '登录成功' })
      router.push('/forum')
    } else {
      ElMessage({ type: 'error', message: '账号或密码错误' })
    }
  } catch {
    ElMessage({ type: 'error', message: '请求失败，请检查网络或后端服务' })
  }
}

const onRegister = async () => {
  try {
    const params = new URLSearchParams()
    params.append('username', registerForm.value.username)
    params.append('password', registerForm.value.password)
    const res = await axios.post(`${API_BASE}/user/register`, params)
    if (res.data && String(res.data).includes('成功')) {
      ElMessage({ type: 'success', message: '注册成功' })
      activeTab.value = 'login'
    } else {
      ElMessage({ type: 'error', message: '注册失败' })
    }
  } catch {
    ElMessage({ type: 'error', message: '请求失败，请检查网络或后端服务' })
  }
}
</script>

<template>
  <div class="auth-container">
    <el-card class="auth-card">
      <h2 class="auth-title">Nexus 平台</h2>
      <el-tabs v-model="activeTab" class="auth-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm" label-width="0">
            <el-form-item>
              <el-input v-model="loginForm.username" placeholder="用户名" prefix-icon="User" size="large" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" size="large" class="auth-btn" @click="onLogin">登 录</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form :model="registerForm" label-width="0">
            <el-form-item>
              <el-input v-model="registerForm.username" placeholder="用户名" prefix-icon="User" size="large" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="registerForm.password" type="password" placeholder="密码" prefix-icon="Lock" size="large" show-password />
            </el-form-item>
            <el-form-item>
              <el-button type="success" size="large" class="auth-btn" @click="onRegister">注 册</el-button>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<style scoped>
.auth-container {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.auth-card {
  width: 400px;
  border-radius: 12px;
}

.auth-title {
  text-align: center;
  margin: 0 0 16px 0;
  color: #303133;
  font-size: 24px;
}

.auth-tabs :deep(.el-tabs__nav) {
  width: 100%;
  display: flex;
}

.auth-tabs :deep(.el-tabs__item) {
  flex: 1;
  text-align: center;
}

.auth-btn {
  width: 100%;
}
</style>
