<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { checkIn, getCheckInStatus, recharge, getRechargeList } from '@/api/points'

const checkedIn = ref(false)
const recentRecords = ref([])
const checkInLoading = ref(false)
const rechargeAmount = ref(10)
const rechargeLoading = ref(false)
const rechargeRecords = ref([])
const rechargePage = ref(1)
const rechargeTotal = ref(0)

const currentUser = () => {
  const stored = localStorage.getItem('user')
  if (stored) {
    try {
      return JSON.parse(stored).user || {}
    } catch { return {} }
  }
  return {}
}

const loadCheckInStatus = async () => {
  try {
    const res = await getCheckInStatus()
    checkedIn.value = res.data?.checkedIn || false
    recentRecords.value = res.data?.recentRecords || []
  } catch { /* ignore */ }
}

const handleCheckIn = async () => {
  checkInLoading.value = true
  try {
    const res = await checkIn()
    const data = res.data
    ElMessage.success(`签到成功！获得 ${data.pointsAwarded} 积分，当前总积分 ${data.totalPoints}`)
    checkedIn.value = true
    // 更新 localStorage
    const stored = JSON.parse(localStorage.getItem('user') || '{}')
    if (stored.user) {
      stored.user.points = data.totalPoints
      localStorage.setItem('user', JSON.stringify(stored))
    }
    loadCheckInStatus()
  } catch (err) {
    ElMessage.error(err.message || '签到失败')
  } finally {
    checkInLoading.value = false
  }
}

const handleRecharge = async () => {
  if (rechargeAmount.value <= 0) {
    ElMessage.warning('请输入有效的充值金额')
    return
  }
  rechargeLoading.value = true
  try {
    const res = await recharge(rechargeAmount.value)
    const data = res.data
    ElMessage.success(`充值成功！获得 ${data.points} 积分，当前总积分 ${data.totalPoints}`)
    const stored = JSON.parse(localStorage.getItem('user') || '{}')
    if (stored.user) {
      stored.user.points = data.totalPoints
      localStorage.setItem('user', JSON.stringify(stored))
    }
    loadRechargeRecords()
  } catch (err) {
    ElMessage.error(err.message || '充值失败')
  } finally {
    rechargeLoading.value = false
  }
}

const loadRechargeRecords = async (page = 1) => {
  rechargePage.value = page
  try {
    const res = await getRechargeList(page, 10)
    rechargeRecords.value = res.data?.records || []
    rechargeTotal.value = res.data?.total || 0
  } catch { /* ignore */ }
}

onMounted(() => {
  loadCheckInStatus()
  loadRechargeRecords()
})
</script>

<template>
  <div class="points-page">
    <h2>积分中心</h2>

    <!-- 每日签到 -->
    <el-card shadow="hover" class="section-card">
      <template #header>
        <div class="card-header">
          <span>每日签到</span>
        </div>
      </template>
      <div class="check-in-area">
        <el-button
          type="primary"
          size="large"
          :loading="checkInLoading"
          :disabled="checkedIn"
          @click="handleCheckIn"
        >
          {{ checkedIn ? '今日已签到' : '签到领积分' }}
        </el-button>
        <p class="check-in-tip">每天可签到一次，随机获得 5-15 积分</p>
      </div>
      <div v-if="recentRecords.length > 0" class="recent-records">
        <h4>最近签到记录</h4>
        <div v-for="rec in recentRecords" :key="rec.id" class="record-item">
          <span>{{ rec.checkInDate }}</span>
          <span class="points-earned">+{{ rec.pointsAwarded }} 积分</span>
        </div>
      </div>
    </el-card>

    <!-- 充值 -->
    <el-card shadow="hover" class="section-card">
      <template #header>
        <div class="card-header">
          <span>积分充值</span>
          <span class="rate-tip">1元 = 10积分</span>
        </div>
      </template>
      <div class="recharge-area">
        <div class="recharge-form">
          <span>充值金额：</span>
          <el-input-number
            v-model="rechargeAmount"
            :min="1"
            :max="9999"
            :step="10"
            style="width:180px;margin-right:12px;"
          />
          <span style="margin-right:8px;">元</span>
          <el-button type="primary" :loading="rechargeLoading" @click="handleRecharge">
            立即充值
          </el-button>
        </div>
        <p class="check-in-tip">模拟充值，即刻到账</p>
      </div>
    </el-card>

    <!-- 充值记录 -->
    <el-card shadow="hover" class="section-card" v-if="rechargeRecords.length > 0">
      <template #header>
        <span>充值记录</span>
      </template>
      <el-table :data="rechargeRecords" stripe>
        <el-table-column prop="orderNo" label="订单号" width="220" />
        <el-table-column prop="amount" label="金额(元)" width="100" />
        <el-table-column prop="points" label="获得积分" width="100" />
        <el-table-column prop="createdAt" label="时间" width="180">
          <template #default="{ row }">
            {{ row.createdAt?.replace('T', ' ').substring(0, 19) || '-' }}
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.points-page {
  max-width: 800px;
  margin: 0 auto;
}

.points-page h2 {
  margin-bottom: 20px;
  color: #303133;
}

.section-card {
  margin-bottom: 20px;
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.rate-tip {
  font-size: 13px;
  color: #909399;
}

.check-in-area {
  text-align: center;
  padding: 16px 0;
}

.check-in-tip {
  margin-top: 10px;
  font-size: 13px;
  color: #909399;
}

.recent-records {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.recent-records h4 {
  margin: 0 0 8px 0;
  font-size: 14px;
  color: #303133;
}

.record-item {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 13px;
  color: #606266;
}

.points-earned {
  color: #67c23a;
  font-weight: 500;
}

.recharge-area {
  padding: 8px 0;
}

.recharge-form {
  display: flex;
  align-items: center;
  font-size: 14px;
}
</style>
