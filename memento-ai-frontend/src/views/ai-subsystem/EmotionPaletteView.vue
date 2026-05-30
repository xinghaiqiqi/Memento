<template>
  <div class="palette-view museum-fade-in">

    <!-- 页面标题区 -->
    <div class="palette-header">
      <div class="header-inner">
        <h1 class="palette-main-title">情绪调色板</h1>
        <div class="header-divider"></div>
        <p class="palette-subtitle">用色彩描绘心境，让每一段记忆都有自己的颜色</p>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="palette-loading">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>正在调和记忆的色彩...</span>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="palette-error">
      <el-icon><WarningFilled /></el-icon>
      <span>{{ error }}</span>
      <el-button size="small" @click="fetchMemories">重新加载</el-button>
    </div>

    <!-- 主内容 -->
    <template v-else>

      <!-- 统计概览 -->
      <div class="palette-stats-row">
        <div class="stat-pill">
          <span class="stat-num">{{ totalCount }}</span>
          <span class="stat-label">记忆总数</span>
        </div>
        <div class="stat-pill">
          <span class="stat-dot" style="background:#2cb67d"></span>
          <span class="stat-num positive">{{ positiveCount }}</span>
          <span class="stat-label">积极</span>
        </div>
        <div class="stat-pill">
          <span class="stat-dot" style="background:#5b8bb0"></span>
          <span class="stat-num neutral">{{ neutralCount }}</span>
          <span class="stat-label">中性</span>
        </div>
        <div class="stat-pill">
          <span class="stat-dot" style="background:#ef4565"></span>
          <span class="stat-num negative">{{ negativeCount }}</span>
          <span class="stat-label">消极</span>
        </div>
      </div>

      <!-- 空数据提示 -->
      <div v-if="totalCount === 0" class="palette-empty">
        <el-icon><Picture /></el-icon>
        <span>还没有记忆色块，去记忆陈列室添加第一段记忆吧</span>
      </div>

      <!-- 色块网格 -->
      <div v-else class="palette-grid">
        <div
          v-for="memory in allMemories"
          :key="memory.id"
          class="color-block"
          :style="{
            background: getColor(memory.sentimentScore).bg,
            borderColor: getColor(memory.sentimentScore).border
          }"
        >
          <!-- 默认显示 -->
          <div class="block-front">
            <span class="block-title">{{ memory.title || '无题' }}</span>
            <span class="block-date">{{ formatDate(memory.createdAt) }}</span>
          </div>

          <!-- hover 显示完整信息 -->
          <div class="block-hover">
            <span class="hover-tag" :style="{ color: getColor(memory.sentimentScore).border }">
              {{ getColor(memory.sentimentScore).label }}
            </span>
            <h4 class="hover-title">{{ memory.title || '无题' }}</h4>
            <p class="hover-content">{{ memory.content || '（无内容）' }}</p>
            <div class="hover-meta">
              <span>{{ formatDate(memory.createdAt) }}</span>
              <span>情感 {{ formatScore(memory.sentimentScore) }}</span>
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
      // MyBatis-Plus 分页返回 { records: [], total: n }
      allMemories.value = res.data.data.records || res.data.data || []
    } else {
      error.value = '获取记忆数据失败，请稍后重试'
    }
  } catch (e) {
    console.error('[EmotionPalette] 获取记忆失败:', e)
    error.value = '无法连接到记忆档案馆，请确认后端服务已启动'
  } finally {
    loading.value = false
  }
}

// ─── 情绪色彩映射（核心规则） ────────────────────────────
const getColor = (rawScore) => {
  const score = parseFloat(rawScore) || 0
  if (score > 0.3) {
    return {
      label: '积极',
      bg: 'linear-gradient(135deg, rgba(44,182,125,0.25), rgba(44,182,125,0.08))',
      border: '#2cb67d'
    }
  }
  if (score < -0.3) {
    return {
      label: '消极',
      bg: 'linear-gradient(135deg, rgba(239,69,101,0.25), rgba(239,69,101,0.08))',
      border: '#ef4565'
    }
  }
  return {
    label: '中性',
    bg: 'linear-gradient(135deg, rgba(91,139,176,0.22), rgba(91,139,176,0.06))',
    border: '#5b8bb0'
  }
}

// ─── 统计计算 ────────────────────────────────────────────
const totalCount = computed(() => allMemories.value.length)

const positiveCount = computed(() =>
  allMemories.value.filter(m => (parseFloat(m.sentimentScore) || 0) > 0.3).length
)
const negativeCount = computed(() =>
  allMemories.value.filter(m => (parseFloat(m.sentimentScore) || 0) < -0.3).length
)
const neutralCount = computed(() =>
  totalCount.value - positiveCount.value - negativeCount.value
)

// ─── 格式化工具 ──────────────────────────────────────────
const formatDate = (raw) => {
  if (!raw) return '未知时间'
  // createdAt 格式：'2024-05-20 22:30:00'，只取日期部分
  return raw.split(' ')[0]
}

const formatScore = (raw) => {
  const score = parseFloat(raw)
  return isNaN(score) ? '0.00' : score.toFixed(2)
}

// ─── 生命周期 ────────────────────────────────────────────
onMounted(() => {
  fetchMemories()
})
</script>

<style lang="scss" scoped>
/* ── 页面容器 ── */
.palette-view {
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
.palette-header {
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
    content: '🎨';
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    font-size: 20px;
    filter: drop-shadow(0 0 10px rgba(157, 80, 187, 0.6));
  }
  &::before { left: 40px; }
  &::after  { right: 40px; }
}

.palette-main-title {
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

.palette-subtitle {
  font-family: var(--font-title);
  font-size: 14px;
  letter-spacing: 3px;
  color: #a0aec0;
  margin: 0;
}

/* ── 加载 / 错误 / 空 ── */
.palette-loading,
.palette-error,
.palette-empty {
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
.palette-stats-row {
  display: flex;
  gap: 20px;
  margin-bottom: 40px;
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

.stat-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  box-shadow: 0 0 8px currentColor;
}

.stat-num {
  font-family: var(--font-title);
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(to bottom, #fff, #cbd5e0);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;

  &.positive { color: #2cb67d; -webkit-text-fill-color: #2cb67d; }
  &.neutral  { color: #5b8bb0; -webkit-text-fill-color: #5b8bb0; }
  &.negative { color: #ef4565; -webkit-text-fill-color: #ef4565; }
}

.stat-label {
  font-size: 12px;
  letter-spacing: 2px;
  color: #718096;
  font-family: var(--font-title);
}

/* ── 色块网格 ── */
.palette-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 20px;
}

/* ── 色块卡片 ── */
.color-block {
  position: relative;
  height: 140px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 20px;
  overflow: hidden;
  cursor: default;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-6px);
    box-shadow: 0 16px 40px rgba(0, 0, 0, 0.4),
                0 0 24px rgba(157, 80, 187, 0.25);

    .block-front { opacity: 0; }
    .block-hover { opacity: 1; }
  }
}

/* 默认正面 */
.block-front {
  position: absolute;
  inset: 0;
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  transition: opacity 0.3s;
}

.block-title {
  font-family: var(--font-title);
  font-size: 16px;
  color: #fff;
  letter-spacing: 1px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.5);
}

.block-date {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  letter-spacing: 1px;
}

/* hover 详情层 */
.block-hover {
  position: absolute;
  inset: 0;
  padding: 18px;
  background: rgba(10, 13, 20, 0.92);
  backdrop-filter: blur(8px);
  display: flex;
  flex-direction: column;
  gap: 6px;
  opacity: 0;
  transition: opacity 0.3s;
}

.hover-tag {
  font-size: 11px;
  font-family: var(--font-title);
  letter-spacing: 1px;
  font-weight: 600;
}

.hover-title {
  font-family: var(--font-title);
  font-size: 15px;
  color: #fff;
  margin: 0;
  letter-spacing: 1px;
}

.hover-content {
  flex: 1;
  font-size: 12px;
  color: #a0aec0;
  line-height: 1.6;
  margin: 0;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.hover-meta {
  display: flex;
  justify-content: space-between;
  font-size: 11px;
  color: #718096;
  letter-spacing: 1px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
  padding-top: 8px;
}
</style>
