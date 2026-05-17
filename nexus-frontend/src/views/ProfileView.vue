<script setup>
import { ref } from 'vue'

const user = ref(JSON.parse(localStorage.getItem('user') || '{}').user || {})
</script>

<template>
  <div class="profile-container">
    <h1>个人中心</h1>
    <el-card>
      <p><strong>用户名：</strong>{{ user.username || '未登录' }}</p>
      <p><strong>ID：</strong>{{ user.id || '-' }}</p>
    </el-card>
  </div>
</template>

<style scoped>
.profile-container {
  padding: 20px;
}
</style>