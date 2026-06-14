<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login, register, sendResetCode, verifyResetCode } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const emit = defineEmits(['login-success'])

const dialogVisible = ref(false)
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

// 忘记密码
const forgotVisible = ref(false)
const forgotStep = ref('input')  // 'input' | 'code'
const forgotUsername = ref('')
const forgotCode = ref('')
const forgotLoading = ref(false)

const handleForgotPassword = async () => {
  if (!forgotUsername.value.trim()) { ElMessage.warning('请输入用户名'); return }
  forgotLoading.value = true
  try {
    await sendResetCode(forgotUsername.value.trim())
    forgotStep.value = 'code'
    ElMessage.success('验证码已发送')
  } catch (err) {
    const msg = err.message || '发送失败'
    if (msg.includes('PHONE_REQUIRED')) {
      ElMessageBox.alert(msg.replace('PHONE_REQUIRED:', ''), '提示', { confirmButtonText: '知道了', type: 'warning' })
    } else {
      ElMessage.error(msg)
    }
  } finally { forgotLoading.value = false }
}

const handleVerifyCode = async () => {
  if (!forgotCode.value.trim()) { ElMessage.warning('请输入验证码'); return }
  forgotLoading.value = true
  try {
    const res = await verifyResetCode(forgotUsername.value.trim(), forgotCode.value.trim())
    ElMessageBox.alert(res.data?.message || '密码已重置', '成功', { confirmButtonText: '好的', type: 'success' })
    forgotVisible.value = false
    forgotStep.value = 'input'
    forgotUsername.value = ''
    forgotCode.value = ''
  } catch (err) { ElMessage.error(err.message || '验证失败') }
  finally { forgotLoading.value = false }
}

const openDialog = (tab = 'login') => {
  activeTab.value = tab
  dialogVisible.value = true
}

const onLogin = async () => {
  if (!loginForm.value.username.trim()) {
    ElMessage({ type: 'warning', message: '请输入用户名' })
    return
  }
  if (!loginForm.value.password) {
    ElMessage({ type: 'warning', message: '请输入密码' })
    return
  }
  try {
    const res = await login(loginForm.value.username.trim(), loginForm.value.password)
    localStorage.setItem('user', JSON.stringify(res.data))
    emit('login-success')
    ElMessage({ type: 'success', message: '登录成功' })
    dialogVisible.value = false
    router.push('/forum')
  } catch (error) {
    ElMessage({ type: 'error', message: error.message || '登录失败' })
  }
}

const onRegister = async () => {
  const username = registerForm.value.username.trim()
  const { password, confirmPassword } = registerForm.value
  if (!username) {
    ElMessage({ type: 'warning', message: '请输入用户名' })
    return
  }
  if (!password) {
    ElMessage({ type: 'warning', message: '请输入密码' })
    return
  }
  if (password.length < 6) {
    ElMessage({ type: 'warning', message: '密码长度不能少于6位' })
    return
  }
  if (!confirmPassword) {
    ElMessage({ type: 'warning', message: '请确认密码' })
    return
  }
  if (password !== confirmPassword) {
    ElMessage({ type: 'error', message: '两次输入的密码不一致' })
    return
  }
  try {
    await register(username, password, confirmPassword)
    ElMessage({ type: 'success', message: '注册成功，请登录' })
    registerForm.value = { username: '', password: '', confirmPassword: '' }
    activeTab.value = 'login'
  } catch (error) {
    ElMessage({ type: 'error', message: error.message || '注册失败' })
  }
}

const features = [
  {
    icon: '📋',
    title: '论坛大厅',
    desc: '浏览所有贴吧，自由发帖交流，支持悬赏帖、精品帖等多种帖子类型，精彩内容一网打尽。',
  },
  {
    icon: '💬',
    title: '实时私聊',
    desc: '基于 WebSocket 的即时通讯，好友间畅快聊天，消息实时推送，未读提醒永不遗漏。',
  },
  {
    icon: '👥',
    title: '好友系统',
    desc: '完整的好友关系管理，在线状态实时可见，支持添加申请、好友搜索与个人资料展示。',
  },
  {
    icon: '🏆',
    title: '积分中心',
    desc: '每日签到赚积分，贴吧活跃冲排行榜，积分可兑换平台权益，让每一次互动都有价值。',
  },
  {
    icon: '🛡️',
    title: '贴吧管理',
    desc: '吧主与吧务分级管理，支持置顶、加精、隐藏帖子，回收站保护误删数据，管理井井有条。',
  },
  {
    icon: '⚙️',
    title: '系统后台',
    desc: '系统管理员拥有全平台管控能力，操作日志可追溯，用户、帖子、评论一站式管理。',
  },
]
</script>

<template>
  <div class="landing">
    <!-- ========== 导航栏 ========== -->
    <header class="landing-header">
      <div class="header-inner">
        <a class="logo" href="#">Nexus</a>
        <nav class="header-nav">
          <a href="#features">功能</a>
          <a href="#about">关于</a>
        </nav>
        <div class="header-actions">
          <el-button text size="large" @click="openDialog('login')">登录</el-button>
          <el-button type="primary" size="large" round @click="openDialog('register')">立即注册</el-button>
        </div>
      </div>
    </header>

    <!-- ========== 主视觉区 ========== -->
    <section class="hero">
      <div class="hero-content">
        <h1 class="hero-title">
          欢迎来到 <span class="hero-highlight">Nexus</span>
        </h1>
        <p class="hero-subtitle">
          一个集论坛交流、即时通讯、好友社交、积分激励于一体的新一代社区平台。
        </p>
        <p class="hero-desc">
          无论你是想寻找志同道合的朋友，还是想建立属于自己的贴吧社区，<br />Nexus 都能为你提供最流畅、最丰富的体验。
        </p>
        <div class="hero-actions">
          <el-button type="primary" size="large" round class="hero-btn" @click="openDialog('register')">
            免费注册，即刻开始 →
          </el-button>
          <el-button text size="large" class="hero-btn-secondary" @click="openDialog('login')">
            已有账号？去登录
          </el-button>
        </div>
        <div class="hero-stats">
          <div class="stat-item">
            <span class="stat-num">7</span>
            <span class="stat-label">核心模块</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-num">WebSocket</span>
            <span class="stat-label">实时通信</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-num">RBAC</span>
            <span class="stat-label">权限管控</span>
          </div>
        </div>
      </div>
      <div class="hero-visual">
        <div class="visual-orb orb-1"></div>
        <div class="visual-orb orb-2"></div>
        <div class="visual-orb orb-3"></div>
        <div class="visual-card card-1">📋 海量贴吧</div>
        <div class="visual-card card-2">💬 实时聊天</div>
        <div class="visual-card card-3">🏆 积分排行</div>
      </div>
    </section>

    <!-- ========== 功能区 ========== -->
    <section id="features" class="features-section">
      <div class="section-header">
        <h2>全方位功能，连接你我</h2>
        <p>从内容创作到社交互动，Nexus 提供一站式的社区体验</p>
      </div>
      <div class="features-grid">
        <div v-for="f in features" :key="f.title" class="feature-card">
          <div class="feature-icon">{{ f.icon }}</div>
          <h3>{{ f.title }}</h3>
          <p>{{ f.desc }}</p>
        </div>
      </div>
    </section>

    <!-- ========== 底部 ========== -->
    <footer id="about" class="landing-footer">
      <div class="footer-inner">
        <div class="footer-brand">
          <h3>Nexus</h3>
          <p>新一代社区平台 — 论坛 · 社交 · 积分 · 管理，四位一体。</p>
        </div>
        <div class="footer-copy">© 2025 Nexus Team. All rights reserved.</div>
      </div>
    </footer>

    <!-- ========== 登录 / 注册弹窗 ========== -->
    <el-dialog
      v-model="dialogVisible"
      :title="null"
      width="420px"
      :close-on-click-modal="false"
      align-center
      destroy-on-close
      class="auth-dialog"
    >
      <div class="dialog-header">
        <h2 class="dialog-logo">Nexus</h2>
        <p class="dialog-slogan">连接每一个有趣的灵魂</p>
      </div>

      <!-- 登录表单 -->
      <template v-if="activeTab === 'login'">
        <el-form :model="loginForm" label-width="0">
          <el-form-item>
            <el-input v-model="loginForm.username" placeholder="用户名" size="large" />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="loginForm.password"
              type="password"
              placeholder="密码"
              size="large"
              show-password
              @keyup.enter="onLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" class="auth-btn" @click="onLogin">登 录</el-button>
          </el-form-item>
        </el-form>
        <p class="switch-tip">
          没有账号？<a href="#" @click.prevent="activeTab = 'register'">立即注册</a> | <a href="#" @click.prevent="forgotVisible = true">忘记密码</a>
        </p>
      </template>

      <!-- 注册表单 -->
      <template v-else>
        <el-form :model="registerForm" label-width="0">
          <el-form-item>
            <el-input v-model="registerForm.username" placeholder="用户名" size="large" />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="registerForm.password"
              type="password"
              placeholder="密码（不少于6位）"
              size="large"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-input
              v-model="registerForm.confirmPassword"
              type="password"
              placeholder="确认密码"
              size="large"
              show-password
              @keyup.enter="onRegister"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="success" size="large" class="auth-btn" @click="onRegister">注 册</el-button>
          </el-form-item>
        </el-form>
        <p class="switch-tip">
          已有账号？<a href="#" @click.prevent="activeTab = 'login'">立即登录</a>
        </p>
      </template>
    </el-dialog>

    <!-- 忘记密码弹窗 -->
    <el-dialog v-model="forgotVisible" title="忘记密码" width="400px" :close-on-click-modal="false" align-center destroy-on-close>
      <template v-if="forgotStep === 'input'">
        <el-form label-width="0">
          <el-form-item>
            <el-input v-model="forgotUsername" placeholder="请输入用户名" size="large" @keyup.enter="handleForgotPassword" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" class="auth-btn" :loading="forgotLoading" @click="handleForgotPassword">发送验证码</el-button>
          </el-form-item>
        </el-form>
        <p style="font-size:13px;color:#909399;text-align:center;">
          需要绑定手机号才能重置密码<br />未绑定手机号？请联系管理员<br />📱 19219785122
        </p>
      </template>
      <template v-else>
        <el-form label-width="0">
          <el-form-item>
            <el-input v-model="forgotCode" placeholder="请输入6位验证码" size="large" maxlength="6" @keyup.enter="handleVerifyCode" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" class="auth-btn" :loading="forgotLoading" @click="handleVerifyCode">验证并重置密码</el-button>
          </el-form-item>
        </el-form>
        <p style="font-size:13px;color:#909399;text-align:center;">验证码10分钟内有效</p>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ==================== 全局基础 ==================== */
.landing {
  min-height: 100vh;
  background: #f8fafc;
  color: #334155;
  font-family: 'Helvetica Neue', 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ==================== 导航栏 ==================== */
.landing-header {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid #e8ecf1;
}

.header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  height: 64px;
  display: flex;
  align-items: center;
  gap: 48px;
}

.logo {
  font-size: 22px;
  font-weight: 800;
  color: #2563eb;
  text-decoration: none;
  letter-spacing: -0.5px;
}

.header-nav {
  display: flex;
  gap: 32px;
}

.header-nav a {
  text-decoration: none;
  color: #64748b;
  font-size: 14px;
  transition: color 0.2s;
}

.header-nav a:hover {
  color: #2563eb;
}

.header-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
}

/* ==================== 主视觉区 ==================== */
.hero {
  max-width: 1200px;
  margin: 0 auto;
  padding: 80px 24px 100px;
  display: flex;
  align-items: center;
  gap: 80px;
  min-height: calc(100vh - 64px - 200px);
}

.hero-content {
  flex: 1;
  max-width: 560px;
}

.hero-title {
  font-size: 44px;
  font-weight: 800;
  line-height: 1.2;
  margin-bottom: 20px;
  color: #0f172a;
}

.hero-highlight {
  background: linear-gradient(135deg, #2563eb, #7c3aed);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.hero-subtitle {
  font-size: 18px;
  color: #475569;
  line-height: 1.6;
  margin-bottom: 12px;
  font-weight: 500;
}

.hero-desc {
  font-size: 15px;
  color: #64748b;
  line-height: 1.8;
  margin-bottom: 36px;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 48px;
}

.hero-btn {
  padding: 14px 32px !important;
  font-size: 16px !important;
  font-weight: 600 !important;
}

.hero-btn-secondary {
  font-size: 15px !important;
  color: #475569 !important;
}

.hero-stats {
  display: flex;
  align-items: center;
  gap: 24px;
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.stat-num {
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.stat-label {
  font-size: 12px;
  color: #94a3b8;
}

.stat-divider {
  width: 1px;
  height: 32px;
  background: #e2e8f0;
}

/* ==================== 右侧装饰 ==================== */
.hero-visual {
  flex: 1;
  position: relative;
  height: 420px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.visual-orb {
  position: absolute;
  border-radius: 50%;
  opacity: 0.5;
}

.orb-1 {
  width: 280px;
  height: 280px;
  background: radial-gradient(circle, rgba(37, 99, 235, 0.25), transparent 70%);
  top: 20px;
  left: 40px;
  animation: float 6s ease-in-out infinite;
}

.orb-2 {
  width: 200px;
  height: 200px;
  background: radial-gradient(circle, rgba(124, 58, 237, 0.2), transparent 70%);
  bottom: 40px;
  right: 20px;
  animation: float 8s ease-in-out infinite reverse;
}

.orb-3 {
  width: 160px;
  height: 160px;
  background: radial-gradient(circle, rgba(14, 165, 233, 0.2), transparent 70%);
  top: 120px;
  right: 80px;
  animation: float 7s ease-in-out infinite 2s;
}

@keyframes float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-20px); }
}

.visual-card {
  position: absolute;
  background: #fff;
  padding: 12px 20px;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  font-size: 14px;
  font-weight: 600;
  color: #334155;
  z-index: 2;
}

.card-1 { top: 60px; left: 10px; animation: float 5s ease-in-out infinite 0.5s; }
.card-2 { top: 160px; right: 0; animation: float 5.5s ease-in-out infinite 1.5s; }
.card-3 { bottom: 80px; left: 40px; animation: float 6.5s ease-in-out infinite 1s; }

/* ==================== 功能区 ==================== */
.features-section {
  max-width: 1200px;
  margin: 0 auto;
  padding: 80px 24px 100px;
}

.section-header {
  text-align: center;
  margin-bottom: 56px;
}

.section-header h2 {
  font-size: 32px;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 12px;
}

.section-header p {
  font-size: 15px;
  color: #64748b;
}

.features-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}

.feature-card {
  background: #fff;
  border-radius: 16px;
  padding: 36px 28px;
  border: 1px solid #f1f5f9;
  transition: all 0.3s ease;
}

.feature-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.08);
  border-color: #e2e8f0;
}

.feature-icon {
  font-size: 36px;
  margin-bottom: 16px;
}

.feature-card h3 {
  font-size: 17px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 8px;
}

.feature-card p {
  font-size: 14px;
  color: #64748b;
  line-height: 1.7;
}

/* ==================== 底部 ==================== */
.landing-footer {
  border-top: 1px solid #e8ecf1;
  background: #fff;
}

.footer-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 32px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.footer-brand h3 {
  font-size: 16px;
  font-weight: 700;
  color: #2563eb;
  margin-bottom: 4px;
}

.footer-brand p {
  font-size: 13px;
  color: #94a3b8;
}

.footer-copy {
  font-size: 13px;
  color: #94a3b8;
}

/* ==================== 登录弹窗 ==================== */
.dialog-header {
  text-align: center;
  margin-bottom: 8px;
}

.dialog-logo {
  font-size: 26px;
  font-weight: 800;
  color: #2563eb;
  margin: 0 0 4px 0;
  letter-spacing: -0.5px;
}

.dialog-slogan {
  font-size: 13px;
  color: #94a3b8;
  margin: 0;
}

.auth-btn {
  width: 100%;
}

.switch-tip {
  text-align: center;
  font-size: 13px;
  color: #94a3b8;
  margin: 0;
}

.switch-tip a {
  color: #2563eb;
  text-decoration: none;
  font-weight: 500;
  cursor: pointer;
}

.switch-tip a:hover {
  text-decoration: underline;
}

/* ==================== 响应式 ==================== */
@media (max-width: 768px) {
  .hero {
    flex-direction: column;
    padding: 40px 20px 60px;
    gap: 40px;
    min-height: auto;
  }

  .hero-title {
    font-size: 32px;
  }

  .hero-visual {
    height: 260px;
    order: -1;
  }

  .features-grid {
    grid-template-columns: 1fr;
  }

  .header-nav {
    display: none;
  }

  .header-actions :first-child {
    display: none;
  }
}
</style>

<!-- 弹窗全局样式（el-dialog 被 teleport 到 body，需非 scoped） -->
<style>
/* 弹窗圆角 */
.el-dialog.auth-dialog {
  border-radius: 20px;
  overflow: hidden;
}

/* 弹窗打开时背景虚化 — :has() 精确定位包含此弹窗的 overlay */
.el-overlay:has(.el-dialog.auth-dialog) {
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  background-color: rgba(0, 0, 0, 0.3) !important;
}

/* 输入框圆角 */
.el-dialog.auth-dialog .el-input .el-input__wrapper {
  border-radius: 12px;
}

/* 按钮圆角 */
.el-dialog.auth-dialog .auth-btn {
  border-radius: 12px;
}
</style>
