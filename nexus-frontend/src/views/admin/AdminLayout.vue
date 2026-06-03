<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const route = useRoute()

const activeMenu = computed(() => {
  if (route.path.startsWith('/admin/users')) return 'users'
  if (route.path.startsWith('/admin/boards')) return 'boards'
  if (route.path.startsWith('/admin/posts')) return 'posts'
  if (route.path.startsWith('/admin/comments')) return 'comments'
  if (route.path.startsWith('/admin/logs')) return 'logs'
  if (route.path.startsWith('/admin/notify')) return 'notify'
  return 'users'
})

const handleMenuSelect = (key) => {
  router.push('/admin/' + key)
}

const goBack = () => {
  router.push('/forum')
}
</script>

<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <div class="admin-sidebar">
      <div class="sidebar-header">
        <h2>管理后台</h2>
      </div>
      <el-menu
        :default-active="activeMenu"
        class="sidebar-menu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409eff"
        @select="handleMenuSelect"
      >
        <el-menu-item index="users">
          <span>用户管理</span>
        </el-menu-item>
        <el-menu-item index="boards">
          <span>吧管理</span>
        </el-menu-item>
        <el-menu-item index="posts">
          <span>帖子管理</span>
        </el-menu-item>
        <el-menu-item index="comments">
          <span>评论管理</span>
        </el-menu-item>
        <el-menu-item index="logs">
          <span>操作日志</span>
        </el-menu-item>
        <el-menu-item index="notify">
          <span>通知管理</span>
        </el-menu-item>
      </el-menu>
      <div class="sidebar-footer">
        <el-button type="default" size="small" @click="goBack">返回首页</el-button>
      </div>
    </div>

    <!-- 内容区 -->
    <div class="admin-main">
      <router-view />
    </div>
  </div>
</template>

<style scoped>
.admin-layout {
  display: flex;
  min-height: calc(100vh - 60px);
}

.admin-sidebar {
  width: 200px;
  background: #304156;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.sidebar-header {
  padding: 20px 16px 12px;
  color: #fff;
}

.sidebar-header h2 {
  font-size: 18px;
  margin: 0;
  font-weight: 600;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-footer .el-button {
  width: 100%;
}

.admin-main {
  flex: 1;
  padding: 20px 24px;
  background: #f5f7fa;
  overflow-y: auto;
}
</style>
