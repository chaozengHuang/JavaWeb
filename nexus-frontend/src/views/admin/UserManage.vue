<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, resetUserPassword, updateUserStatus, hardDeleteUser } from '@/api/admin'

const router = useRouter()
const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const size = ref(10)
const filters = reactive({ keyword: '', status: '', role: '' })

const load = async () => {
  loading.value = true
  try { const r = await getUsers({ page: page.value, size: size.value, ...filters }); list.value = r.data?.records || []; total.value = r.data?.total || 0 } catch (e) { ElMessage.error(e.message) } finally { loading.value = false }
}

const handleResetPwd = (row) => {
  ElMessageBox.confirm(`确定重置用户「${row.username}」的密码为 123456？`, '重置密码', { type: 'warning' }).then(async () => {
    await resetUserPassword(row.id, '123456'); ElMessage.success('密码已重置为 123456')
  }).catch(() => {})
}

const handleToggle = (row) => {
  const s = row.status === 'BANNED' ? 'ACTIVE' : 'BANNED'
  const a = s === 'BANNED' ? '封禁' : '解封'
  ElMessageBox.confirm(`确定${a}用户「${row.username}」？`, '操作确认', { type: 'warning' }).then(async () => {
    await updateUserStatus(row.id, s); row.status = s; ElMessage.success(`${a}成功`)
  }).catch(() => {})
}

const handleHardDelete = (row) => {
  ElMessageBox.confirm(`⚠ 确定从数据库彻底清除用户「${row.username}」？不可恢复！`, '清理确认', { confirmButtonText: '确认清理', type: 'error' }).then(async () => {
    await hardDeleteUser(row.id); ElMessage.success('已清理'); load()
  }).catch(() => {})
}

const roleLabel = (r) => r === 'SYS_ADMIN' ? '管理员' : r === 'NOTIFY_ADMIN' ? '通知' : '用户'
onMounted(load)
</script>

<template>
  <div>
    <h3>用户管理</h3>
    <div style="display:flex;gap:10px;margin-bottom:12px;flex-wrap:wrap;">
      <el-input v-model="filters.keyword" placeholder="搜索用户名" clearable style="width:180px" @keyup.enter="load" />
      <el-select v-model="filters.status" placeholder="状态" clearable style="width:110px" @change="load"><el-option label="正常" value="ACTIVE" /><el-option label="已封禁" value="BANNED" /></el-select>
      <el-button type="primary" @click="load">搜索</el-button>
      <el-button @click="filters.keyword='';filters.status='';load()">重置</el-button>
    </div>
    <el-table :data="list" v-loading="loading" border stripe>
      <el-table-column prop="username" label="用户名" min-width="140" />
      <el-table-column label="角色" width="80"><template #default="{row}"><el-tag size="small" :type="row.globalRole==='SYS_ADMIN'?'danger':'info'">{{ roleLabel(row.globalRole) }}</el-tag></template></el-table-column>
      <el-table-column label="状态" width="80"><template #default="{row}"><el-tag size="small" :type="row.status==='BANNED'?'danger':'success'">{{ row.status==='BANNED'?'已封禁':'正常' }}</el-tag></template></el-table-column>
      <el-table-column prop="points" label="积分" width="80" />
      <el-table-column prop="createdAt" label="注册时间" width="160"><template #default="{row}">{{ (row.createdAt||'').replace('T',' ').slice(0,19) }}</template></el-table-column>
      <el-table-column label="操作" width="320" fixed="right">
        <template #default="{row}">
          <div class="action-row">
            <el-button size="small" text type="primary" @click="router.push('/user/'+row.id)">主页</el-button>
            <el-button size="small" text type="warning" @click="handleResetPwd(row)">重置密码</el-button>
            <el-button size="small" text :type="row.status==='BANNED'?'success':'danger'" @click="handleToggle(row)">{{ row.status==='BANNED'?'解封':'封禁' }}</el-button>
            <el-button size="small" text type="danger" @click="handleHardDelete(row)">清理</el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <div style="text-align:center;padding:16px 0;" v-if="total > size">
      <el-pagination :current-page="page" :page-size="size" :total="total" layout="prev,pager,next" background @current-change="(p) => { page = p; load() }" />
    </div>
  </div>
</template>

<style scoped>
.action-row {
  display: flex;
  align-items: center;
  gap: 2px;
  white-space: nowrap;
}
</style>
