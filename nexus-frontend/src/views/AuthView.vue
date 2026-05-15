<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login, register } from '@/api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()

const activeTab = ref('login')

const loginForm = ref({
  username: '',
  password: '',
})

const registerForm = ref({
  username: '',
  password: '',
  confirmPassword: '',
})

const onLogin = async () => {
  if (!loginForm.value.username || !loginForm.value.password) {
    ElMessage({ type: 'warning', message: '请填写用户名和密码' })
    return
  }
  try {
    const data = await login(loginForm.value.username, loginForm.value.password)
    localStorage.setItem('user', JSON.stringify(data))
    ElMessage({ type: 'success', message: '登录成功' })
    router.push('/forum')
  } catch (error) {
    ElMessage({ type: 'error', message: error.message || '登录失败' })
  }
}

const onRegister = async () => {
  const { username, password, confirmPassword } = registerForm.value
  if (!username || !password || !confirmPassword) {
    ElMessage({ type: 'warning', message: '请填写所有字段' })
    return
  }
  if (password !== confirmPassword) {
    ElMessage({ type: 'error', message: '两次输入的密码不一致' })
    return
  }
  if (password.length < 6 || password.length > 20) {
    ElMessage({ type: 'warning', message: '密码需 6-20 个字符' })
    return
  }
  try {
    await register(username, password)
    ElMessage({ type: 'success', message: '注册成功，请登录' })
    registerForm.value = { username: '', password: '', confirmPassword: '' }
    activeTab.value = 'login'
  } catch (error) {
    ElMessage({ type: 'error', message: error.message || '注册失败' })
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
              <el-input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" prefix-icon="Lock" size="large" show-password @keyup.enter="onRegister" />
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
