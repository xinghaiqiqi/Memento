<template>
  <div class="echo-view museum-fade-in">

    <!-- 页面标题区 -->
    <div class="echo-header">
      <div class="header-inner">
        <h1 class="echo-main-title">时光回声</h1>
        <div class="header-divider"></div>
        <p class="echo-subtitle">回望来路，聆听情绪在时间长河中的回响</p>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="echo-loading">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>正在唤醒沉睡的回声...</span>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="echo-error">
      <el-icon><WarningFilled /></el-icon>
      <span>{{ error }}</span>
      <el-button size="small" @click="fetchMemories">重新加载</el-button>
    </div>

    <!-- 数据不足提示 -->
    <div v-else-if="totalCount === 0" class="echo-empty">
      <el-icon><Clock /></el-icon>
      <span>还没有可供回响的记忆，去记忆陈列室留下第一段时光吧</span>
    </div>

    <!-- 主内容 -->
    <template v-else>

      <!-- 第一部分：统计卡片 -->
      <div class="echo-stats-row">
        <div class="stat-pill">
          <span class="stat-num">{{ earlyMemories.length }}</span>
          <span class="stat-label">早期记忆</span>
        </div>
        <div class="stat-pill">
          <span class="stat-num">{{ recentMemories.length }}</span>
          <span class="stat-label">近期记忆</span>
        </div>
        <div class="stat-pill">
          <span class="stat-num" :style="{ color: scoreColor(earlyAvg) }">{{ earlyAvg.toFixed(2) }}</span>
          <span class="stat-label">早期情绪均值</span>
        </div>
        <div class="stat-pill">
          <span class="stat-num" :style="{ color: scoreColor(recentAvg) }">{{ recentAvg.toFixed(2) }}</span>
          <span class="stat-label">近期情绪均值</span>
        </div>
        <div class="stat-pill trend-pill">
          <span class="trend-icon" :class="trend.cls">{{ trend.arrow }}</span>
          <span class="stat-label">{{ trend.text }}</span>
        </div>
      </div>

      <!-- 第三部分：情绪趋势总结 -->
      <div class="echo-summary">
        <div class="summary-header">
          <el-icon><MagicStick /></el-icon>
          <span>时光寄语</span>
        </div>
        <p class="summary-text">{{ summary }}</p>
      </div>

      <!-- 第二部分：左右对比列表 -->
      <div class="echo-compare">

        <!-- 左侧：早期记忆 -->
        <div class="compare-column early-column">
          <div class="column-header">
            <span class="column-tag">最初的回声</span>
            <h3 class="column-title">早期记忆</h3>
          </div>
          <div class="memory-list">
            <div
              v-for="m in earlyMemories"
              :key="'early-' + m.id"
              class="memory-item"
            >
              <div class="memory-dot" :style="{ background: scoreColor(m.sentimentScore) }"></div>
              <div class="memory-body">
                <span class="memory-title">{{ m.title || '无题' }}</span>
                <span class="memory-date">{{ formatDate(m.createdAt) }}</span>
              </div>
              <span class="memory-score" :style="{ color: scoreColor(m.sentimentScore) }">
                {{ formatScore(m.sentimentScore) }}
              </span>
            </div>
          </div>
        </div>

        <!-- 中间分隔时间轴 -->
        <div class="compare-divider">
          <div class="divider-line"></div>
          <div class="divider-icon">⟶</div>
          <div class="divider-line"></div>
        </div>

        <!-- 右侧：近期记忆 -->
        <div class="compare-column recent-column">
          <div class="column-header">
            <span class="column-tag">当下的回声</span>
            <h3 class="column-title">近期记忆</h3>
          </div>
          <div class="memory-list">
            <div
              v-for="m in recentMemories"
              :key="'recent-' + m.id"
              class="memory-item"
            >
              <div class="memory-dot" :style="{ background: scoreColor(m.sentimentScore) }"></div>
              <div class="memory-body">
                <span class="memory-title">{{ m.title || '无题' }}</span>
                <span class="memory-date">{{ formatDate(m.createdAt) }}</span>
              </div>
              <span class="memory-score" :style="{ color: scoreColor(m.sentimentScore) }">
                {{ formatScore(m.sentimentScore) }}
              </span>
            </div>
          </div>
        </div>

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

// ─── 获取记忆数据 ────────────────────────────────────────
const fetchMemories = async () => {
  loading.value = true
  error.value = null
  try {
    const res = await axios.get('/api/memories', {
      params: { current: 1, size: 200 }
    })
    if (res.data && res.data.code === 200 && res.data.data) {
      allMemories.value = res.data.data.records || res.data.data || []
    } else {
      error.value = '获取记忆数据失败，请稍后重试'
    }
  } catch (e) {
    console.error('[TimeEcho] 获取记忆失败:', e)
    error.value = '无法连接到记忆档案馆，请确认后端服务已启动'
  } finally {
    loading.value = false
  }
}

// ─── 时间排序（按 createdAt 升序） ───────────────────────
const sortedByTime = computed(() => {
  return [...allMemories.value]
    .filter(m => m.createdAt)
    .sort((a, b) => {
      // createdAt 格式 'YYYY-MM-DD HH:mm:ss'，字符串可直接比较
      return a.createdAt < b.createdAt ? -1 : a.createdAt > b.createdAt ? 1 : 0
    })
})

const totalCount = computed(() => sortedByTime.value.length)

// 早期记忆：最早的 10 条
const earlyMemories = computed(() => sortedByTime.value.slice(0, 10))

// 近期记忆：最新的 10 条（保持时间倒序，最新在前）
const recentMemories = computed(() => sortedByTime.value.slice(-10).reverse())

// ─── 情绪均值计算 ────────────────────────────────────────
const avgScore = (list) => {
  if (!list.length) return 0
  const sum = list.reduce((acc, m) => acc + (parseFloat(m.sentimentScore) || 0), 0)
  return sum / list.length
}

const earlyAvg = computed(() => avgScore(earlyMemories.value))
const recentAvg = computed(() => avgScore(recentMemories.value))

// ─── 趋势判断 ────────────────────────────────────────────
const trend = computed(() => {
  const diff = recentAvg.value - earlyAvg.value
  if (diff > 0.1) return { arrow: '↗', cls: 'trend-up', text: '情绪上升' }
  if (diff < -0.1) return { arrow: '↘', cls: 'trend-down', text: '情绪下降' }
  return { arrow: '→', cls: 'trend-flat', text: '情绪平稳' }
})

// ─── 情绪标签（统一规则） ────────────────────────────────
const scoreLabel = (raw) => {
  const s = parseFloat(raw) || 0
  if (s > 0.3) return '积极'
  if (s < -0.3) return '消极'
  return '平静'
}

const scoreColor = (raw) => {
  const s = parseFloat(raw) || 0
  if (s > 0.3) return '#2cb67d'
  if (s < -0.3) return '#ef4565'
  return '#5b8bb0'
}

// ─── 总结文案（前端拼接，保底可运行） ────────────────────
const summary = computed(() => {
  if (totalCount.value === 0) return ''
  const from = scoreLabel(earlyAvg.value)
  const to = scoreLabel(recentAvg.value)
  return `你的情绪从「${from}」（${earlyAvg.value.toFixed(2)}）走向「${to}」（${recentAvg.value.toFixed(2)}），整体呈现${trend.value.text}的趋势。时光流转，每一段记忆都在悄悄塑造着今天的你。`
})

// ─── 格式化工具 ──────────────────────────────────────────
const formatDate = (raw) => {
  if (!raw) return '未知时间'
  return raw.split(' ')[0]
}

const formatScore = (raw) => {
  const s = parseFloat(raw)
  return isNaN(s) ? '0.00' : s.toFixed(2)
}

// ─── 生命周期 ────────────────────────────────────────────
onMounted(() => {
  fetchMemories()
})
</script>

<style lang="scss" scoped>
/* ── 页面容器 ── */
.echo-view {
  padding: 40px;
  max-width: 1400px;
  margin: 0 auto;
}

/* ── 淡入动画 ── */
.museum-fade-in {
  animation: fadeInUp 0.5s ease both;
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ── 页面标题区 ── */
.echo-header {
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
    content: '🌊';
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    font-size: 20px;
    filter: drop-shadow(0 0 10px rgba(157, 80, 187, 0.6));
  }
  &::before { left: 40px; }
  &::after  { right: 40px; }
}

.echo-main-title {
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

.echo-subtitle {
  font-family: var(--font-title);
  font-size: 14px;
  letter-spacing: 3px;
  color: #a0aec0;
  margin: 0;
}

/* ── 加载 / 错误 / 空 ── */
.echo-loading,
.echo-error,
.echo-empty {
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

/* ── 统计卡片 ── */
.echo-stats-row {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
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
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(to bottom, #fff, #cbd5e0);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.stat-label {
  font-size: 12px;
  letter-spacing: 2px;
  color: #718096;
  font-family: var(--font-title);
}

.trend-icon {
  font-size: 32px;
  font-weight: 700;
  line-height: 1;

  &.trend-up   { color: #2cb67d; text-shadow: 0 0 12px rgba(44, 182, 125, 0.6); }
  &.trend-down { color: #ef4565; text-shadow: 0 0 12px rgba(239, 69, 101, 0.6); }
  &.trend-flat { color: #5b8bb0; text-shadow: 0 0 12px rgba(91, 139, 176, 0.6); }
}

/* ── 总结寄语 ── */
.echo-summary {
  background: rgba(15, 18, 24, 0.4);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(157, 80, 187, 0.3);
  border-radius: 16px;
  padding: 24px 28px;
  margin-bottom: 40px;
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

/* ── 左右对比 ── */
.echo-compare {
  display: flex;
  align-items: stretch;
  gap: 24px;
}

.compare-column {
  flex: 1;
  background: rgba(15, 18, 24, 0.3);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(157, 80, 187, 0.25);
  border-radius: 24px;
  padding: 28px;
  transition: all 0.4s;

  &:hover {
    border-color: var(--accent-mystic);
    box-shadow: 0 15px 40px rgba(0, 0, 0, 0.3),
                0 0 24px rgba(157, 80, 187, 0.2);
  }
}

.column-header {
  text-align: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
}

.column-tag {
  display: block;
  font-size: 12px;
  letter-spacing: 2px;
  color: var(--accent-mystic);
  font-family: var(--font-title);
  margin-bottom: 6px;
}

.column-title {
  font-family: var(--font-title);
  font-size: 22px;
  letter-spacing: 3px;
  margin: 0;
  background: linear-gradient(to right, #fff, var(--accent-mystic));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* 中间时间轴分隔 */
.compare-divider {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: 40px;
}

.divider-line {
  flex: 1;
  width: 1px;
  background: linear-gradient(to bottom, transparent, rgba(157, 80, 187, 0.5), transparent);
}

.divider-icon {
  font-size: 20px;
  color: var(--accent-mystic);
  filter: drop-shadow(0 0 8px rgba(157, 80, 187, 0.6));
}

/* ── 记忆列表项 ── */
.memory-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.memory-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  transition: all 0.3s;

  &:hover {
    background: rgba(157, 80, 187, 0.06);
    border-color: rgba(157, 80, 187, 0.25);
    transform: translateX(2px);
  }
}

.memory-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 0 8px currentColor;
}

.memory-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.memory-title {
  font-size: 14px;
  color: #fff;
  font-family: var(--font-title);
  letter-spacing: 1px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.memory-date {
  font-size: 11px;
  color: #718096;
  letter-spacing: 1px;
}

.memory-score {
  font-family: var(--font-title);
  font-size: 16px;
  font-weight: 700;
  flex-shrink: 0;
}

/* ── 响应式 ── */
@media (max-width: 768px) {
  .echo-compare {
    flex-direction: column;
  }
  .compare-divider {
    width: 100%;
    flex-direction: row;
    height: 40px;
  }
  .divider-line {
    width: auto;
    height: 1px;
    flex: 1;
  }
  .divider-icon {
    transform: rotate(90deg);
  }
}
</style>
