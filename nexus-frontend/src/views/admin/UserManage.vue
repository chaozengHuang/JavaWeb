<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, getUserDetail, resetUserPassword, updateUserStatus } from '@/api/admin'

// ==================== 列表状态 ====================
const loading = ref(false)
const userList = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(10)

const filters = reactive({
  keyword: '',
  status: '',
  role: '',
})

// ==================== 详情弹窗 ====================
const detailVisible = ref(false)
const detailUser = ref(null)
const detailPostCount = ref(0)
const detailCommentCount = ref(0)

// ==================== 重置密码弹窗 ====================
const passwordVisible = ref(false)
const passwordTarget = ref(null)
const newPassword = ref('')
const passwordSubmitting = ref(false)

// ==================== 数据加载 ====================
const loadUsers = async () => {
  loading.value = true
  try {
    const params = { page: page.value, size: pageSize.value }
    if (filters.keyword) params.keyword = filters.keyword
    if (filters.status) params.status = filters.status
    if (filters.role) params.role = filters.role
    const res = await getUsers(params)
    userList.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (err) {
    ElMessage({ type: 'error', message: err.message || '加载失败' })
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadUsers()
}

const handleReset = () => {
  filters.keyword = ''
  filters.status = ''
  filters.role = ''
  page.value = 1
  loadUsers()
}

const handlePageChange = (p) => {
  page.value = p
  loadUsers()
}

// ==================== 查看详情 ====================
const handleViewDetail = async (user) => {
  try {
    const res = await getUserDetail(user.id)
    detailUser.value = res.data?.user || res.data
    detailPostCount.value = res.data?.postCount ?? 0
    detailCommentCount.value = res.data?.commentCount ?? 0
    detailVisible.value = true
  } catch (err) {
    ElMessage({ type: 'error', message: err.message || '加载失败' })
  }
}

// ==================== 重置密码 ====================
const handleOpenPassword = (user) => {
  passwordTarget.value = user
  newPassword.value = ''
  passwordVisible.value = true
}

const handleResetPassword = async () => {
  if (!newPassword.value || newPassword.value.length < 6) {
    ElMessage({ type: 'warning', message: '密码长度不能少于6位' })
    return
  }
  passwordSubmitting.value = true
  try {
    await resetUserPassword(passwordTarget.value.id, newPassword.value)
    ElMessage({ type: 'success', message: '密码重置成功' })
    passwordVisible.value = false
  } catch (err) {
    ElMessage({ type: 'error', message: err.message || '操作失败' })
  } finally {
    passwordSubmitting.value = false
  }
}

// ==================== 封禁/解封 ====================
const handleToggleStatus = (user) => {
  const isBan = user.status !== 'BANNED'
  const action = isBan ? '封禁' : '解封'
  const targetStatus = isBan ? 'BANNED' : 'ACTIVE'

  ElMessageBox.confirm(
    `确定要${action}用户「${user.username}」吗？`,
    `${action}确认`,
    { confirmButtonText: action, cancelButtonText: '取消', type: 'warning' }
  ).then(async () => {
    try {
      await updateUserStatus(user.id, targetStatus)
      user.status = targetStatus
      ElMessage({ type: 'success', message: `${action}成功` })
    } catch (err) {
      ElMessage({ type: 'error', message: err.message || '操作失败' })
    }
  }).catch(() => {})
}

// ==================== 格式化 ====================
const formatTime = (time) => {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

const statusLabel = (status) => {
  return status === 'BANNED' ? '已封禁' : '正常'
}

const roleLabel = (role) => {
  return role === 'SYS_ADMIN' ? '管理员' : '普通用户'
}

onMounted(() => {
  loadUsers()
})
</script>

<template>
  <div class="user-manage">
    <h3>用户管理</h3>

    <!-- 搜索筛选 -->
    <div class="filter-bar">
      <el-input
        v-model="filters.keyword"
        placeholder="搜索用户名/邮箱"
        clearable
        style="width: 220px"
        @keyup.enter="handleSearch"
      />
      <el-select v-model="filters.status" placeholder="状态筛选" clearable style="width: 140px">
        <el-option label="正常" value="ACTIVE" />
        <el-option label="已封禁" value="BANNED" />
      </el-select>
      <el-select v-model="filters.role" placeholder="角色筛选" clearable style="width: 140px">
        <el-option label="普通用户" value="USER" />
        <el-option label="管理员" value="SYS_ADMIN" />
      </el-select>
      <el-button type="primary" @click="handleSearch">搜索</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <!-- 用户表格 -->
    <el-table v-loading="loading" :data="userList" border stripe style="width: 100%">
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" min-width="120" />
      <el-table-column prop="email" label="邮箱" min-width="160" />
      <el-table-column label="角色" width="100">
        <template #default="{ row }">
          <el-tag :type="row.globalRole === 'SYS_ADMIN' ? 'danger' : 'info'" size="small">
            {{ roleLabel(row.globalRole) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 'BANNED' ? 'danger' : 'success'" size="small">
            {{ statusLabel(row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="points" label="积分" width="80" />
      <el-table-column label="注册时间" width="170">
        <template #default="{ row }">{{ formatTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" size="small" text @click="handleViewDetail(row)">详情</el-button>
          <el-button type="warning" size="small" text @click="handleOpenPassword(row)">重置密码</el-button>
          <el-button
            :type="row.status === 'BANNED' ? 'success' : 'danger'"
            size="small"
            text
            @click="handleToggleStatus(row)"
          >
            {{ row.status === 'BANNED' ? '解封' : '封禁' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        :current-page="page"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next, total"
        background
        @current-change="handlePageChange"
      />
    </div>

    <!-- 用户详情弹窗 -->
    <el-dialog v-model="detailVisible" title="用户详情" width="500px">
      <template v-if="detailUser">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="ID">{{ detailUser.id }}</el-descriptions-item>
          <el-descriptions-item label="用户名">{{ detailUser.username }}</el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag :type="detailUser.globalRole === 'SYS_ADMIN' ? 'danger' : 'info'" size="small">
              {{ roleLabel(detailUser.globalRole) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="detailUser.status === 'BANNED' ? 'danger' : 'success'" size="small">
              {{ statusLabel(detailUser.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="积分">{{ detailUser.points ?? 0 }}</el-descriptions-item>
          <el-descriptions-item label="发帖数">{{ detailPostCount }}</el-descriptions-item>
          <el-descriptions-item label="评论数">{{ detailCommentCount }}</el-descriptions-item>
          <el-descriptions-item label="注册时间">{{ formatTime(detailUser.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="邮箱">{{ detailUser.email || '-' }}</el-descriptions-item>
          <el-descriptions-item label="手机">{{ detailUser.phone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="所在地">{{ detailUser.location || '-' }}</el-descriptions-item>
        </el-descriptions>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog v-model="passwordVisible" title="重置密码" width="400px">
      <p style="margin-bottom: 12px; color: #606266;">
        为用户 <strong>{{ passwordTarget?.username }}</strong> 设置新密码：
      </p>
      <el-input
        v-model="newPassword"
        type="password"
        placeholder="请输入新密码（至少6位）"
        show-password
        minlength="6"
      />
      <template #footer>
        <el-button @click="passwordVisible = false">取消</el-button>
        <el-button type="primary" :loading="passwordSubmitting" @click="handleResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.user-manage h3 {
  margin: 0 0 16px 0;
  font-size: 18px;
  color: #303133;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  align-items: center;
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}
</style>
