<template>
  <div class="keywords-view museum-fade-in">
    
    <!-- 页面标题区 -->
    <div class="keywords-header">
      <div class="header-inner">
        <h1 class="keywords-main-title">灵魂关键词</h1>
        <div class="header-divider"></div>
        <p class="keywords-subtitle">解析记忆深处的密码，发现内心的真实声音</p>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="keywords-loading">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>正在解析记忆密码...</span>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="keywords-error">
      <el-icon><WarningFilled /></el-icon>
      <span>{{ error }}</span>
      <el-button size="small" @click="fetchMemories">重新解析</el-button>
    </div>

    <!-- 主内容 -->
    <template v-else>

      <!-- 统计概览 -->
      <div class="keywords-stats-row">
        <div class="stat-card">
          <div class="stat-icon">📊</div>
          <div class="stat-content">
            <span class="stat-num">{{ totalMemories }}</span>
            <span class="stat-label">记忆总数</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🏷️</div>
          <div class="stat-content">
            <span class="stat-num">{{ totalKeywords }}</span>
            <span class="stat-label">关键词数</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">💭</div>
          <div class="stat-content">
            <span class="stat-num">{{ avgSentiment }}</span>
            <span class="stat-label">平均情感</span>
          </div>
        </div>
        <div class="stat-card">
          <div class="stat-icon">🎯</div>
          <div class="stat-content">
            <span class="stat-num">{{ topKeyword?.word || '-' }}</span>
            <span class="stat-label">热门词汇</span>
          </div>
        </div>
      </div>

      <!-- AI 总结（可选） -->
      <div v-if="aiSummary" class="ai-summary-card">
        <div class="summary-header">
          <el-icon><MagicStick /></el-icon>
          <span>AI 洞察</span>
        </div>
        <p class="summary-text">{{ aiSummary }}</p>
      </div>

      <!-- 图表区域 -->
      <el-row :gutter="30" class="charts-section">
        
        <!-- 关键词柱状图 -->
        <el-col :span="14">
          <div class="museum-exhibit-card chart-card">
            <div class="card-header">
              <span class="header-title">关键词频次分析</span>
              <div class="header-dot"></div>
            </div>
            <div ref="barChartRef" class="chart-box"></div>
          </div>
        </el-col>

        <!-- 情感分布饼图 -->
        <el-col :span="10">
          <div class="museum-exhibit-card chart-card">
            <div class="card-header">
              <span class="header-title">情感色彩分布</span>
              <div class="header-dot"></div>
            </div>
            <div ref="pieChartRef" class="chart-box"></div>
          </div>
        </el-col>

      </el-row>

      <!-- 关键词详细列表 -->
      <div class="keywords-list-section">
        <div class="museum-exhibit-card">
          <div class="card-header">
            <span class="header-title">关键词详细统计</span>
            <div class="header-dot"></div>
          </div>
          
          <div class="keywords-grid">
            <div
              v-for="(item, index) in topKeywords"
              :key="item.word"
              class="keyword-item"
            >
              <div class="keyword-rank">{{ index + 1 }}</div>
              <div class="keyword-content">
                <span class="keyword-text">{{ item.word }}</span>
                <div class="keyword-bar">
                  <div 
                    class="keyword-fill"
                    :style="{ width: (item.count / topKeywords[0].count * 100) + '%' }"
                  ></div>
                </div>
              </div>
              <div class="keyword-count">{{ item.count }}</div>
            </div>
          </div>

        </div>
      </div>

    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'
import { ElMessage } from 'element-plus'

// ─── 状态 ───────────────────────────────────────────────
const loading = ref(false)
const error = ref(null)
const allMemories = ref([])
const aiSummary = ref('')

// ECharts 实例
const barChartRef = ref(null)
const pieChartRef = ref(null)
let barChart = null
let pieChart = null

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
      
      // 数据加载完成后，延迟初始化图表
      setTimeout(() => {
        if (barChart && pieChart) {
          updateCharts()
        } else {
          initCharts()
        }
      }, 100)
      
      // 可选：生成 AI 总结
      if (allMemories.value.length > 0) {
        generateAISummary()
      }
    } else {
      error.value = '获取记忆数据失败，请稍后重试'
    }
  } catch (e) {
    console.error('[KeywordsView] 获取记忆失败:', e)
    error.value = '无法连接到记忆档案馆，请确认后端服务已启动'
  } finally {
    loading.value = false
  }
}

// ─── 关键词提取与统计 ────────────────────────────────────
const extractKeywords = (text) => {
  if (!text) return []
  
  // 简单的中文分词：按标点符号和空格分割，过滤短词和数字
  const words = text
    .replace(/[，。！？；：""''（）【】《》\s\d+]/g, ' ')
    .split(/\s+/)
    .filter(word => word.length >= 2 && word.length <= 8)
    .filter(word => !/^\d+$/.test(word))
  
  return words
}

const keywordStats = computed(() => {
  const wordCount = {}
  
  allMemories.value.forEach(memory => {
    let words = []
    
    // 优先使用 tags 字段
    if (memory.tags && memory.tags.trim()) {
      words = memory.tags.split(/[,，\s]+/).filter(tag => tag.trim())
    } else {
      // 从 title 和 content 提取关键词
      const titleWords = extractKeywords(memory.title || '')
      const contentWords = extractKeywords(memory.content || '')
      words = [...titleWords, ...contentWords]
    }
    
    // 统计词频
    words.forEach(word => {
      const cleanWord = word.trim()
      if (cleanWord) {
        wordCount[cleanWord] = (wordCount[cleanWord] || 0) + 1
      }
    })
  })
  
  // 转换为数组并排序
  return Object.entries(wordCount)
    .map(([word, count]) => ({ word, count }))
    .sort((a, b) => b.count - a.count)
})

const topKeywords = computed(() => keywordStats.value.slice(0, 10))

// ─── 情感分布统计 ────────────────────────────────────────
const sentimentStats = computed(() => {
  let positive = 0, negative = 0, neutral = 0
  
  allMemories.value.forEach(memory => {
    const score = parseFloat(memory.sentimentScore) || 0
    if (score > 0.3) positive++
    else if (score < -0.3) negative++
    else neutral++
  })
  
  return { positive, negative, neutral }
})

// ─── 计算属性 ────────────────────────────────────────────
const totalMemories = computed(() => allMemories.value.length)
const totalKeywords = computed(() => keywordStats.value.length)
const topKeyword = computed(() => topKeywords.value[0])

const avgSentiment = computed(() => {
  if (allMemories.value.length === 0) return '0.0'
  const sum = allMemories.value.reduce((acc, memory) => {
    return acc + (parseFloat(memory.sentimentScore) || 0)
  }, 0)
  return (sum / allMemories.value.length).toFixed(1)
})

// ─── AI 总结生成（调用我自己的 DeepSeek，失败自动降级） ──
const generateAISummary = async () => {
  try {
    const sentiment = sentimentStats.value
    const res = await axios.post('/api/ai-subsystem/insight/keywords', {
      keywords: topKeywords.value,
      positive: sentiment.positive,
      neutral: sentiment.neutral,
      negative: sentiment.negative,
      total: totalMemories.value
    })
    if (res.data && res.data.code === 200 && res.data.data) {
      aiSummary.value = res.data.data
    } else {
      // 降级：本地生成
      aiSummary.value = buildFallbackSummary()
    }
  } catch (e) {
    console.log('[KeywordsView] AI 洞察调用失败，使用本地降级文案')
    aiSummary.value = buildFallbackSummary()
  }
}

const buildFallbackSummary = () => {
  if (!topKeywords.value.length) return ''
  return `在 ${totalMemories.value} 段记忆中，「${topKeywords.value[0]?.word}」是你内心最深的印记，平均情感值 ${avgSentiment.value} 映射着你独特的人生色彩。`
}

// ─── ECharts 初始化与更新 ────────────────────────────────
const initCharts = () => {
  barChart = echarts.init(barChartRef.value, 'dark')
  pieChart = echarts.init(pieChartRef.value, 'dark')
  
  updateCharts()
}

const updateCharts = () => {
  // 柱状图：Top 10 关键词
  const barData = topKeywords.value.map(item => ({
    name: item.word,
    value: item.count
  }))
  
  barChart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(15, 18, 24, 0.9)',
      borderColor: 'rgba(157, 80, 187, 0.3)',
      textStyle: { color: '#fff' }
    },
    xAxis: {
      type: 'category',
      data: barData.map(item => item.name),
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
      axisLabel: { 
        color: '#94a1b2',
        interval: 0,
        rotate: barData.length > 6 ? 45 : 0
      }
    },
    yAxis: {
      type: 'value',
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } },
      axisLabel: { color: '#94a1b2' }
    },
    series: [{
      data: barData.map(item => ({
        value: item.value,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(157, 80, 187, 0.8)' },
            { offset: 1, color: 'rgba(157, 80, 187, 0.3)' }
          ])
        }
      })),
      type: 'bar',
      barWidth: '60%',
      itemStyle: {
        borderRadius: [4, 4, 0, 0]
      }
    }]
  })
  
  // 饼图：情感分布
  const sentiment = sentimentStats.value
  pieChart.setOption({
    backgroundColor: 'transparent',
    tooltip: {
      trigger: 'item',
      backgroundColor: 'rgba(15, 18, 24, 0.9)',
      borderColor: 'rgba(157, 80, 187, 0.3)',
      textStyle: { color: '#fff' }
    },
    series: [{
      type: 'pie',
      radius: ['50%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 8,
        borderColor: '#0b0e14',
        borderWidth: 2
      },
      label: {
        show: true,
        position: 'outside',
        color: '#94a1b2',
        fontSize: 12
      },
      data: [
        { 
          value: sentiment.positive, 
          name: '积极', 
          itemStyle: { color: '#2cb67d' } 
        },
        { 
          value: sentiment.neutral, 
          name: '中性', 
          itemStyle: { color: '#72757e' } 
        },
        { 
          value: sentiment.negative, 
          name: '消极', 
          itemStyle: { color: '#ef4565' } 
        }
      ]
    }]
  })
}

const handleResize = () => {
  barChart?.resize()
  pieChart?.resize()
}

// ─── 生命周期 ────────────────────────────────────────────
onMounted(() => {
  initCharts()
  fetchMemories()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  pieChart?.dispose()
})
</script>

<style lang="scss" scoped>
/* ── 页面容器 ── */
.keywords-view {
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
.keywords-header {
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
    content: '🔍';
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    font-size: 20px;
    filter: drop-shadow(0 0 10px rgba(157, 80, 187, 0.6));
  }
  &::before { left: 40px; }
  &::after  { right: 40px; }
}

.keywords-main-title {
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

.keywords-subtitle {
  font-family: var(--font-title);
  font-size: 14px;
  letter-spacing: 3px;
  color: #a0aec0;
  margin: 0;
}

/* ── 加载 / 错误 ── */
.keywords-loading,
.keywords-error {
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
.keywords-stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 20px;
  margin-bottom: 30px;
}

.stat-card {
  background: rgba(15, 18, 24, 0.4);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  transition: all 0.3s;

  &:hover {
    border-color: rgba(157, 80, 187, 0.4);
    transform: translateY(-2px);
  }
}

.stat-icon {
  font-size: 32px;
  line-height: 1;
}

.stat-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-num {
  font-family: var(--font-title);
  font-size: 24px;
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

/* ── AI 总结卡片 ── */
.ai-summary-card {
  background: rgba(15, 18, 24, 0.4);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(157, 80, 187, 0.3);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 30px;
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
  line-height: 1.7;
  font-style: italic;
  margin: 0;
}

/* ── 图表区域 ── */
.charts-section {
  margin-bottom: 40px;
}

.museum-exhibit-card {
  background: rgba(15, 18, 24, 0.3);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 24px;
  padding: 30px;
  transition: all 0.4s;
  
  &:hover {
    background: rgba(15, 18, 24, 0.4);
    border-color: var(--accent-mystic);
    box-shadow: 0 15px 40px rgba(0, 0, 0, 0.3);
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 30px;
    padding-bottom: 15px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    
    .header-title {
      font-family: var(--font-title);
      color: #fff;
      font-size: 18px;
      letter-spacing: 2px;
    }
    .header-dot {
      width: 6px; height: 6px;
      background: var(--accent-mystic);
      border-radius: 50%;
      box-shadow: 0 0 10px var(--accent-mystic);
    }
  }
}

.chart-box {
  height: 380px;
}

/* ── 关键词列表 ── */
.keywords-list-section {
  margin-top: 40px;
}

.keywords-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.keyword-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 20px;
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  transition: all 0.3s;

  &:hover {
    background: rgba(157, 80, 187, 0.05);
    border-color: rgba(157, 80, 187, 0.2);
  }
}

.keyword-rank {
  width: 32px;
  height: 32px;
  background: var(--grad-mystic);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-title);
  font-size: 14px;
  font-weight: 700;
  color: #fff;
  flex-shrink: 0;
}

.keyword-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.keyword-text {
  font-family: var(--font-title);
  font-size: 16px;
  color: #fff;
  letter-spacing: 1px;
}

.keyword-bar {
  height: 4px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 2px;
  overflow: hidden;
}

.keyword-fill {
  height: 100%;
  background: var(--grad-mystic);
  border-radius: 2px;
  transition: width 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.keyword-count {
  font-family: var(--font-title);
  font-size: 18px;
  font-weight: 700;
  color: var(--accent-mystic);
  flex-shrink: 0;
}
</style>