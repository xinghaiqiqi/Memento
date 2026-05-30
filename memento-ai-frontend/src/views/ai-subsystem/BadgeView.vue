<template>
  <div class="badge-view museum-fade-in">

    <!-- 页面标题区 -->
    <div class="badge-header">
      <div class="header-inner">
        <h1 class="badge-main-title">时光勋章</h1>
        <div class="header-divider"></div>
        <p class="badge-subtitle">记录你的情绪成长轨迹</p>
      </div>
    </div>

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
          <div
            class="badge-progress-fill"
            :style="{ width: progressPercent + '%' }"
          ></div>
        </div>
        <span class="progress-text">{{ unlockedCount }} / {{ badges.length }}</span>
      </div>

      <!-- 勋章网格 -->
      <div class="badge-grid">
        <div
          v-for="badge in badges"
          :key="badge.id"
          :class="['badge-card', badge.unlocked ? 'badge-unlocked' : 'badge-locked']"
        >
          <!-- 解锁光晕 -->
          <div v-if="badge.unlocked" class="badge-glow"></div>

          <!-- 解锁/锁定标识 -->
          <div :class="['badge-check', badge.unlocked ? 'check-yes' : 'check-no']">
            {{ badge.unlocked ? '✔' : '✖' }}
          </div>

          <!-- 勋章图标区 -->
          <div class="badge-icon-wrap">
            <div :class="['badge-icon-ring', badge.unlocked ? 'ring-active' : 'ring-inactive']">
              <span class="badge-emoji">{{ badge.icon }}</span>
            </div>
          </div>

          <!-- 勋章信息 -->
          <div class="badge-info">
            <h3 :class="['badge-name', badge.unlocked ? 'name-active' : 'name-locked']">
              {{ badge.name }}
            </h3>
            <p class="badge-condition">{{ badge.condition }}</p>
            <div :class="['badge-status-tag', badge.unlocked ? 'tag-unlocked' : 'tag-locked']">
              {{ badge.unlocked ? '已解锁' : '未解锁' }}
            </div>
          </div>
        </div>
      </div>

      <!-- AI 总结（可选，无法连接时自动降级为本地文案） -->
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

// ─── 状态 ───────────────────────────────────────────────
const loading = ref(false)
const error = ref(null)
const allMemories = ref([])
const summary = ref('')

// ─── 获取记忆数据 ────────────────────────────────────────
const fetchMemories = async () => {
  loading.value = true
  error.value = null
  try {
    const res = await axios.get('/api/memories', {
      params: { current: 1, size: 200 }
    })
    if (res.data && res.data.code === 200 && res.data.data) {
      // MyBatis-Plus 分页返回 { records: [], total: n }
      allMemories.value = res.data.data.records || res.data.data || []
      await buildSummary()
    } else {
      error.value = '获取记忆数据失败，请稍后重试'
    }
  } catch (e) {
    console.error('[BadgeView] 获取记忆失败:', e)
    error.value = '无法连接到记忆档案馆，请确认后端服务已启动'
  } finally {
    loading.value = false
  }
}

// ─── 派生统计 ────────────────────────────────────────────
const totalMemories = computed(() => allMemories.value.length)

// 平均情感分值
const avgSentiment = computed(() => {
  if (allMemories.value.length === 0) return 0
  const sum = allMemories.value.reduce((acc, m) => {
    return acc + (parseFloat(m.sentimentScore) || 0)
  }, 0)
  return sum / allMemories.value.length
})

// 是否存在凌晨（00:00 - 05:00）创建的记忆
const hasNightMemory = computed(() => {
  return allMemories.value.some(m => {
    if (!m.createdAt) return false
    // createdAt 格式：'2024-05-20 03:30:00'
    const timeStr = m.createdAt.split(' ')[1]
    if (!timeStr) return false
    const hour = parseInt(timeStr.split(':')[0], 10)
    return hour >= 0 && hour < 5
  })
})

// 是否存在积极记录（sentimentScore > 0.3）
const hasPositive = computed(() => {
  return allMemories.value.some(m => (parseFloat(m.sentimentScore) || 0) > 0.3)
})

// 是否存在消极记录（sentimentScore < -0.3）
const hasNegative = computed(() => {
  return allMemories.value.some(m => (parseFloat(m.sentimentScore) || 0) < -0.3)
})

// ─── 勋章规则定义 ────────────────────────────────────────
const badges = computed(() => [
  {
    id: 'beginner',
    name: '入门记录者',
    icon: '📖',
    condition: '累计记录 3 条记忆',
    unlocked: totalMemories.value >= 3
  },
  {
    id: 'steady',
    name: '稳定记录者',
    icon: '🗂️',
    condition: '累计记录 10 条记忆',
    unlocked: totalMemories.value >= 10
  },
  {
    id: 'nightwalker',
    name: '夜行者',
    icon: '🌙',
    condition: '在凌晨 00:00 ~ 05:00 创建过记忆',
    unlocked: hasNightMemory.value
  },
  {
    id: 'joy-collector',
    name: '情绪收集者',
    icon: '☀️',
    condition: '平均情感分值大于 0.5',
    unlocked: avgSentiment.value > 0.5
  },
  {
    id: 'observer',
    name: '情绪观察者',
    icon: '🎭',
    condition: '同时拥有积极与消极的记忆',
    unlocked: hasPositive.value && hasNegative.value
  }
])

// ─── 统计计算 ────────────────────────────────────────────
const unlockedCount = computed(() => badges.value.filter(b => b.unlocked).length)

const progressPercent = computed(() => {
  if (badges.value.length === 0) return 0
  return Math.round((unlockedCount.value / badges.value.length) * 100)
})

// ─── 成长寄语（调用我自己的 DeepSeek，失败自动降级） ────
const buildSummary = async () => {
  if (totalMemories.value === 0) {
    summary.value = '你的博物馆尚是一片空白，去记录第一段记忆，点亮第一枚勋章吧。'
    return
  }

  const unlockedNames = badges.value.filter(b => b.unlocked).map(b => b.name)
  if (unlockedNames.length === 0) {
    summary.value = `你已收藏 ${totalMemories.value} 段记忆，继续记录，第一枚勋章正在等待你。`
    return
  }

  try {
    const res = await axios.post('/api/ai-subsystem/insight/badges', {
      unlocked: unlockedNames,
      total: totalMemories.value,
      avgSentiment: avgSentiment.value
    })
    if (res.data && res.data.code === 200 && res.data.data) {
      summary.value = res.data.data
    } else {
      summary.value = buildFallbackSummary()
    }
  } catch (e) {
    console.log('[BadgeView] AI 寄语调用失败，使用本地降级文案')
    summary.value = buildFallbackSummary()
  }
}

const buildFallbackSummary = () => {
  const got = unlockedCount.value
  const total = badges.value.length
  return `你已收藏 ${totalMemories.value} 段记忆，点亮了 ${got} / ${total} 枚时光勋章，平均情感分值为 ${avgSentiment.value.toFixed(2)}。每一枚勋章，都是你与时光缔结的契约。`
}

// ─── 生命周期 ────────────────────────────────────────────
onMounted(() => {
  fetchMemories()
})
</script>

<style lang="scss" scoped>
/* ── 页面容器 ── */
.badge-view {
  padding: 40px;
  max-width: 1200px;
  margin: 0 auto;
}

/* ── 淡入动画（与其他页面保持一致） ── */
.museum-fade-in {
  animation: fadeInUp 0.5s ease both;
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ── 页面标题区 ── */
.badge-header {
  background: rgba(15, 18, 24, 0.4);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 50px;
  text-align: center;
  position: relative;
  margin-bottom: 40px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5),
              inset 0 0 40px rgba(157, 80, 187, 0.05);

  &::before, &::after {
    content: '✦';
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    color: var(--accent-mystic);
    font-size: 20px;
    text-shadow: 0 0 15px rgba(157, 80, 187, 0.6);
  }
  &::before { left: 40px; }
  &::after  { right: 40px; }
}

.badge-main-title {
  font-family: var(--font-title);
  font-size: 48px;
  letter-spacing: 14px;
  margin: 0 0 20px 0;
  background: var(--grad-mystic);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 0 12px rgba(157, 80, 187, 0.4));
}

.header-divider {
  width: 260px;
  height: 1px;
  background: linear-gradient(to right, transparent, var(--accent-mystic), transparent);
  margin: 0 auto 20px;
  opacity: 0.6;
}

.badge-subtitle {
  font-family: var(--font-title);
  font-size: 14px;
  letter-spacing: 3px;
  color: #a0aec0;
  margin: 0;
}

/* ── 加载 / 错误 ── */
.badge-loading,
.badge-error {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 80px 0;
  color: #718096;
  font-family: var(--font-title);
  letter-spacing: 2px;
  font-size: 15px;
}

.loading-icon {
  font-size: 22px;
  animation: spin 1.2s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}

/* ── 统计概览 ── */
.badge-stats-row {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.stat-pill {
  flex: 1;
  min-width: 120px;
  background: rgba(15, 18, 24, 0.4);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  transition: border-color 0.3s;

  &:hover { border-color: rgba(157, 80, 187, 0.4); }
}

.stat-num {
  font-family: var(--font-title);
  font-size: 36px;
  font-weight: 700;
  background: linear-gradient(to bottom, #fff, #cbd5e0);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;

  &.unlocked {
    background: var(--grad-mystic);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    filter: drop-shadow(0 0 8px rgba(157, 80, 187, 0.5));
  }
  &.locked {
    background: linear-gradient(to bottom, #718096, #4a5568);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.stat-label {
  font-size: 12px;
  letter-spacing: 2px;
  color: #718096;
  font-family: var(--font-title);
}

/* ── 进度条 ── */
.badge-progress-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 40px;
}

.badge-progress-bar {
  flex: 1;
  height: 6px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 3px;
  overflow: hidden;
}

.badge-progress-fill {
  height: 100%;
  background: var(--grad-mystic);
  border-radius: 3px;
  box-shadow: 0 0 12px rgba(157, 80, 187, 0.6);
  transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.progress-text {
  font-family: var(--font-title);
  font-size: 13px;
  color: #718096;
  letter-spacing: 1px;
  white-space: nowrap;
}

/* ── 勋章网格 ── */
.badge-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 24px;
}

/* ── 勋章卡片基础 ── */
.badge-card {
  position: relative;
  background: rgba(15, 18, 24, 0.4);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 32px 24px;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  gap: 18px;
}

/* 已解锁卡片：紫色渐变高亮 */
.badge-unlocked {
  border: 1px solid rgba(157, 80, 187, 0.4);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4),
              0 0 0 1px rgba(157, 80, 187, 0.1) inset;

  &:hover {
    transform: translateY(-8px);
    border-color: var(--accent-mystic);
    box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5),
                0 0 30px rgba(157, 80, 187, 0.2);
    .badge-glow { opacity: 0.8; }
  }
}

/* 未解锁卡片：灰色 + 模糊 */
.badge-locked {
  border: 1px solid rgba(255, 255, 255, 0.06);
  filter: grayscale(0.9) blur(0.4px);
  opacity: 0.55;

  &:hover {
    opacity: 0.75;
    border-color: rgba(255, 255, 255, 0.12);
  }
}

/* 解锁光晕 */
.badge-glow {
  position: absolute;
  bottom: -60px;
  right: -60px;
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, var(--accent-mystic), transparent 70%);
  opacity: 0.3;
  filter: blur(40px);
  transition: opacity 0.5s;
  pointer-events: none;
}

/* 解锁/锁定标识 */
.badge-check {
  position: absolute;
  top: 16px;
  right: 18px;
  font-size: 16px;
  font-weight: 700;
  line-height: 1;

  &.check-yes { color: var(--accent-secondary); text-shadow: 0 0 10px rgba(44, 182, 125, 0.6); }
  &.check-no  { color: #4a5568; }
}

/* ── 图标区 ── */
.badge-icon-wrap {
  display: flex;
  justify-content: center;
}

.badge-icon-ring {
  width: 76px;
  height: 76px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.4s;
}

.ring-active {
  background: rgba(157, 80, 187, 0.15);
  border: 2px solid rgba(157, 80, 187, 0.5);
  box-shadow: 0 0 20px rgba(157, 80, 187, 0.3),
              inset 0 0 20px rgba(157, 80, 187, 0.05);

  .badge-card:hover & {
    box-shadow: 0 0 30px rgba(157, 80, 187, 0.5),
                inset 0 0 20px rgba(157, 80, 187, 0.1);
    transform: scale(1.08) rotate(5deg);
  }
}

.ring-inactive {
  background: rgba(255, 255, 255, 0.03);
  border: 2px solid rgba(255, 255, 255, 0.1);
}

.badge-emoji {
  font-size: 34px;
  line-height: 1;
  display: block;
}

/* ── 勋章信息 ── */
.badge-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
  text-align: center;
  align-items: center;
}

.badge-name {
  font-family: var(--font-title);
  font-size: 18px;
  letter-spacing: 3px;
  margin: 0;

  &.name-active {
    background: linear-gradient(to right, #fff, var(--accent-mystic));
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }
  &.name-locked {
    color: #4a5568;
  }
}

.badge-condition {
  font-size: 12px;
  color: #718096;
  line-height: 1.6;
  margin: 0;
}

.badge-status-tag {
  display: inline-block;
  padding: 4px 14px;
  border-radius: 20px;
  font-size: 11px;
  font-family: var(--font-title);
  letter-spacing: 1px;
  font-weight: 600;

  &.tag-unlocked {
    background: rgba(157, 80, 187, 0.15);
    color: var(--accent-mystic);
    border: 1px solid rgba(157, 80, 187, 0.3);
  }
  &.tag-locked {
    background: rgba(114, 117, 126, 0.12);
    color: #72757e;
    border: 1px solid rgba(114, 117, 126, 0.25);
  }
}

/* ── 成长寄语 ── */
.badge-summary {
  margin-top: 40px;
  background: rgba(15, 18, 24, 0.4);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(157, 80, 187, 0.3);
  border-radius: 16px;
  padding: 24px 28px;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    height: 2px;
    background: var(--grad-mystic);
  }
}

.summary-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  color: var(--accent-mystic);
  font-family: var(--font-title);
  font-size: 14px;
  letter-spacing: 2px;
}

.summary-text {
  color: #e2e8f0;
  line-height: 1.8;
  font-style: italic;
  margin: 0;
}
</style>
