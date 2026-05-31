<template>
  <div class="badge-content">
    <!-- 加载状态 -->
    <div v-if="loading" class="badge-loading">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>正在检索记忆档案...</span>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="badge-error">
      <el-icon><WarningFilled /></el-icon>
      <span>{{ error }}</span>
      <el-button size="small" @click="fetchMemories">重新加载</el-button>
    </div>

    <!-- 主内容 -->
    <template v-else>
      <!-- 统计概览 -->
      <div class="badge-stats-row">
        <div class="stat-pill">
          <span class="stat-num">{{ totalMemories }}</span>
          <span class="stat-label">记忆总数</span>
        </div>
        <div class="stat-pill">
          <span class="stat-num unlocked">{{ unlockedCount }}</span>
          <span class="stat-label">已获勋章</span>
        </div>
        <div class="stat-pill">
          <span class="stat-num locked">{{ badges.length - unlockedCount }}</span>
          <span class="stat-label">待解锁</span>
        </div>
        <div class="stat-pill">
          <span class="stat-num">{{ progressPercent }}%</span>
          <span class="stat-label">完成度</span>
        </div>
      </div>

      <!-- 进度条 -->
      <div class="badge-progress-wrap">
        <div class="badge-progress-bar">
          <div class="badge-progress-fill" :style="{ width: progressPercent + '%' }"></div>
        </div>
        <span class="progress-text">{{ unlockedCount }} / {{ badges.length }}</span>
      </div>

      <!-- 勋章网格 -->
      <div class="badge-grid">
        <div v-for="badge in badges" :key="badge.id" :class="['badge-card', badge.unlocked ? 'badge-unlocked' : 'badge-locked']">
          <div v-if="badge.unlocked" class="badge-glow"></div>
          <div :class="['badge-check', badge.unlocked ? 'check-yes' : 'check-no']">{{ badge.unlocked ? '✔' : '✖' }}</div>
          <div class="badge-icon-wrap">
            <div :class="['badge-icon-ring', badge.unlocked ? 'ring-active' : 'ring-inactive']">
              <span class="badge-emoji">{{ badge.icon }}</span>
            </div>
          </div>
          <div class="badge-info">
            <h3 :class="['badge-name', badge.unlocked ? 'name-active' : 'name-locked']">{{ badge.name }}</h3>
            <p class="badge-condition">{{ badge.condition }}</p>
            <div :class="['badge-status-tag', badge.unlocked ? 'tag-unlocked' : 'tag-locked']">{{ badge.unlocked ? '已解锁' : '未解锁' }}</div>
          </div>
        </div>
      </div>

      <!-- 成长寄语 -->
      <div v-if="summary" class="badge-summary">
        <div class="summary-header">
          <el-icon><MagicStick /></el-icon>
          <span>成长寄语</span>
        </div>
        <p class="summary-text">{{ summary }}</p>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'

const loading = ref(false)
const error = ref(null)
const allMemories = ref([])
const summary = ref('')

const fetchMemories = async () => {
  loading.value = true
  error.value = null
  try {
    const res = await axios.get('/api/memories', { params: { current: 1, size: 200 } })
    if (res.data && res.data.code === 200 && res.data.data) {
      allMemories.value = res.data.data.records || res.data.data || []
      await buildSummary()
    } else {
      error.value = '获取记忆数据失败'
    }
  } catch (e) {
    error.value = '无法连接到服务器'
  } finally {
    loading.value = false
  }
}

const totalMemories = computed(() => allMemories.value.length)
const avgSentiment = computed(() => {
  if (allMemories.value.length === 0) return 0
  return allMemories.value.reduce((acc, m) => acc + (parseFloat(m.sentimentScore) || 0), 0) / allMemories.value.length
})

const hasNightMemory = computed(() => {
  return allMemories.value.some(m => {
    if (!m.createdAt) return false
    const hour = parseInt(m.createdAt.split(' ')[1]?.split(':')[0], 10)
    return hour >= 0 && hour < 5
  })
})

const hasPositive = computed(() => allMemories.value.some(m => (parseFloat(m.sentimentScore) || 0) > 0.3))
const hasNegative = computed(() => allMemories.value.some(m => (parseFloat(m.sentimentScore) || 0) < -0.3))

const badges = computed(() => [
  { id: 'beginner', name: '入门记录者', icon: '📖', condition: '累计记录 3 条记忆', unlocked: totalMemories.value >= 3 },
  { id: 'steady', name: '稳定记录者', icon: '🗂️', condition: '累计记录 10 条记忆', unlocked: totalMemories.value >= 10 },
  { id: 'nightwalker', name: '夜行者', icon: '🌙', condition: '在凌晨 00:00 ~ 05:00 创建过记忆', unlocked: hasNightMemory.value },
  { id: 'joy-collector', name: '情绪收集者', icon: '☀️', condition: '平均情感分值大于 0.5', unlocked: avgSentiment.value > 0.5 },
  { id: 'observer', name: '情绪观察者', icon: '🎭', condition: '同时拥有积极与消极的记忆', unlocked: hasPositive.value && hasNegative.value }
])

const unlockedCount = computed(() => badges.value.filter(b => b.unlocked).length)
const progressPercent = computed(() => badges.value.length ? Math.round((unlockedCount.value / badges.value.length) * 100) : 0)

const buildSummary = async () => {
  if (totalMemories.value === 0) {
    summary.value = '你的博物馆尚是一片空白，去记录第一段记忆吧。'
    return
  }
  try {
    const res = await axios.post('/api/ai-subsystem/insight/badges', {
      unlocked: badges.value.filter(b => b.unlocked).map(b => b.name),
      total: totalMemories.value,
      avgSentiment: avgSentiment.value
    })
    if (res.data?.code === 200) summary.value = res.data.data
    else summary.value = `你已获得 ${unlockedCount.value} 枚勋章，继续探索内心的力量。`
  } catch (e) {
    summary.value = `你已获得 ${unlockedCount.value} 枚勋章，继续探索。`
  }
}

onMounted(() => fetchMemories())
</script>

<style lang="scss" scoped>
.badge-stats-row { display: flex; flex-wrap: wrap; gap: 15px; margin-bottom: 25px; }
.stat-pill { background: rgba(255, 255, 255, 0.05); border-radius: 20px; padding: 8px 15px; display: flex; align-items: center; gap: 8px; border: 1px solid rgba(255, 255, 255, 0.1); .stat-num { font-weight: 700; color: #fff; font-size: 16px; } .stat-label { font-size: 11px; color: #94a1b2; } }
.badge-progress-wrap { margin-bottom: 30px; display: flex; align-items: center; gap: 15px; .badge-progress-bar { flex: 1; height: 8px; background: rgba(255,255,255,0.05); border-radius: 4px; overflow: hidden; .badge-progress-fill { height: 100%; background: var(--grad-mystic); border-radius: 4px; box-shadow: 0 0 10px var(--accent-mystic); } } .progress-text { color: #94a1b2; font-size: 12px; } }
.badge-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 20px; margin-bottom: 30px; }
.badge-card { background: rgba(255, 255, 255, 0.03); border: 1px solid rgba(255, 255, 255, 0.05); border-radius: 15px; padding: 20px; text-align: center; position: relative; transition: all 0.3s; }
.badge-unlocked { border-color: var(--accent-primary); background: rgba(127, 90, 240, 0.05); }
.badge-locked { opacity: 0.6; grayscale: 1; }
.badge-glow { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: radial-gradient(circle at center, rgba(127, 90, 240, 0.2), transparent 70%); pointer-events: none; }
.badge-check { position: absolute; top: 10px; right: 10px; width: 20px; height: 20px; border-radius: 50%; font-size: 10px; display: flex; align-items: center; justify-content: center; }
.check-yes { background: #2cb67d; color: #fff; }
.check-no { background: rgba(255,255,255,0.1); color: #72757e; }
.badge-icon-wrap { margin-bottom: 15px; .badge-icon-ring { width: 70px; height: 70px; border-radius: 50%; margin: 0 auto; display: flex; align-items: center; justify-content: center; border: 2px solid; .badge-emoji { font-size: 32px; } } .ring-active { border-color: var(--accent-primary); box-shadow: 0 0 15px var(--accent-primary); } .ring-inactive { border-color: rgba(255,255,255,0.1); } }
.badge-info { .badge-name { font-family: var(--font-title); font-size: 16px; margin-bottom: 8px; } .name-active { color: #fff; } .name-locked { color: #94a1b2; } .badge-condition { font-size: 11px; color: #72757e; line-height: 1.4; margin-bottom: 12px; } .badge-status-tag { display: inline-block; padding: 3px 8px; border-radius: 4px; font-size: 10px; } .tag-unlocked { background: rgba(44, 182, 125, 0.2); color: #2cb67d; } .tag-locked { background: rgba(255,255,255,0.05); color: #72757e; } }
.badge-summary { background: rgba(127, 90, 240, 0.05); border-radius: 10px; padding: 15px; border: 1px solid rgba(127, 90, 240, 0.2); .summary-header { display: flex; align-items: center; gap: 8px; color: var(--accent-primary); font-size: 14px; margin-bottom: 8px; } .summary-text { color: #e2e8f0; font-size: 13px; line-height: 1.6; } }
.badge-loading, .badge-error { text-align: center; padding: 40px; color: #94a1b2; font-size: 14px; }
</style>
