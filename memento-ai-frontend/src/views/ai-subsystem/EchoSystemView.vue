<template>
  <div class="echo-system-view museum-fade-in">
    <!-- 页面标题区 -->
    <div class="echo-header">
      <div class="header-inner">
        <h1 class="echo-main-title">心灵回声</h1>
        <div class="header-divider"></div>
        <p class="echo-subtitle">解析灵魂深处的共鸣，聆听时光流转中的心绪回响</p>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="echo-loading">
      <div class="loading-scanner"></div>
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>正在唤醒沉睡的灵魂记忆...</span>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="echo-error">
      <el-icon><WarningFilled /></el-icon>
      <span>{{ error }}</span>
      <el-button size="small" @click="fetchData">重新加载</el-button>
    </div>

    <!-- 主内容区 -->
    <template v-else>
      <!-- 1. 灵魂概览统计 -->
      <div class="echo-stats-grid">
        <div class="stat-card glass-morphism">
          <div class="stat-icon">📊</div>
          <div class="stat-content">
            <span class="stat-label">记忆总数</span>
            <span class="stat-num">{{ totalMemories }}</span>
          </div>
          <div class="card-glow"></div>
        </div>
        <div class="stat-card glass-morphism">
          <div class="stat-icon">🎯</div>
          <div class="stat-content">
            <span class="stat-label">灵魂关键词</span>
            <span class="stat-num">{{ topKeyword?.word || '-' }}</span>
          </div>
          <div class="card-glow"></div>
        </div>
        <div class="stat-card glass-morphism">
          <div class="stat-icon">📈</div>
          <div class="stat-content">
            <span class="stat-label">情绪趋势</span>
            <span class="stat-num" :class="trend.cls">{{ trend.text }}</span>
          </div>
          <div class="card-glow"></div>
        </div>
        <div class="stat-card glass-morphism">
          <div class="stat-icon">🧠</div>
          <div class="stat-content">
            <span class="stat-label">情感均值</span>
            <span class="stat-num">{{ avgSentiment }}</span>
          </div>
          <div class="card-glow"></div>
        </div>
      </div>

      <el-row :gutter="30" class="echo-main-row">
        <!-- 左侧：灵魂解析 -->
        <el-col :span="13">
          <!-- AI 综合洞察 -->
          <div v-if="aiSummary" class="ai-insight-card glass-morphism">
            <div class="insight-header">
              <el-icon><MagicStick /></el-icon>
              <span>灵魂导览寄语</span>
            </div>
            <p class="insight-text">{{ aiSummary }}</p>
            <div class="insight-bg-icon">✨</div>
          </div>

          <div class="museum-exhibit-card analysis-card glass-morphism">
            <div class="card-header">
              <span class="header-title">灵魂色彩与密码</span>
              <div class="header-dot"></div>
            </div>
            
            <div class="charts-container">
              <div class="chart-item">
                <span class="chart-label">情感构成</span>
                <div ref="pieChartRef" class="echart-box"></div>
              </div>
              <div class="chart-item">
                <span class="chart-label">关键词频次</span>
                <div ref="barChartRef" class="echart-box"></div>
              </div>
            </div>

            <div class="echo-compare-section">
              <div class="compare-header">
                <el-icon><RefreshRight /></el-icon>
                <span>时光回响对比</span>
              </div>
              
              <div class="compare-flow">
                <div class="flow-side early">
                  <div class="side-tag">最初</div>
                  <div class="mini-memory-list">
                    <div v-for="m in earlyMemories" :key="'e-'+m.id" class="mini-memory-item">
                      <div class="sentiment-dot" :style="{ background: scoreColor(m.sentimentScore) }"></div>
                      <span class="mini-title">{{ m.title }}</span>
                    </div>
                  </div>
                </div>
                
                <div class="flow-connector">
                  <div class="line"></div>
                  <div class="arrow">✦</div>
                  <div class="line"></div>
                </div>

                <div class="flow-side recent">
                  <div class="side-tag active">当下</div>
                  <div class="mini-memory-list">
                    <div v-for="m in recentMemories" :key="'r-'+m.id" class="mini-memory-item">
                      <div class="sentiment-dot" :style="{ background: scoreColor(m.sentimentScore) }"></div>
                      <span class="mini-title">{{ m.title }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-col>

        <!-- 右侧：心灵对话亭 -->
        <el-col :span="11">
          <div class="museum-exhibit-card dialogue-card glass-morphism">
            <div class="card-header">
              <span class="header-title">心灵对话亭</span>
              <div class="header-dot"></div>
            </div>
            
            <div class="chat-container" ref="chatWindowRef">
              <div v-if="messages.length === 0" class="chat-placeholder">
                <div class="placeholder-icon">🕯️</div>
                <h3>一处静谧角落</h3>
                <p>倾诉此刻的心绪，让 AI 成为你灵魂的回声</p>
                <div class="suggested-prompts">
                  <span @click="inputText = '我今天心情有点复杂...'">"我今天心情有点复杂..."</span>
                  <span @click="inputText = '帮我分析一下最近的情绪。'">"分析一下最近的情绪"</span>
                </div>
              </div>
              
              <div v-for="(msg, index) in messages" :key="index" 
                   :class="['chat-row', msg.role === 'user' ? 'row-user' : 'row-ai']">
                <div class="chat-avatar">{{ msg.role === 'user' ? '👤' : '✨' }}</div>
                <div :class="['chat-bubble', msg.role === 'user' ? 'bubble-user' : 'bubble-ai']">
                  <p class="bubble-text">{{ msg.content }}</p>
                  <div v-if="msg.role === 'ai' && msg.canSave" class="bubble-actions">
                    <button class="save-btn-mini" :disabled="msg.saving || msg.saved" @click="saveAsMemory(msg, index)">
                      <el-icon v-if="msg.saving"><Loading /></el-icon>
                      <el-icon v-else-if="msg.saved"><CircleCheck /></el-icon>
                      <el-icon v-else><Collection /></el-icon>
                      {{ msg.saved ? '已封存' : '封存记忆' }}
                    </button>
                  </div>
                </div>
              </div>
              
              <div v-if="sending" class="chat-row row-ai">
                <div class="chat-avatar">✨</div>
                <div class="chat-bubble bubble-ai typing">
                  <span class="dot"></span><span class="dot"></span><span class="dot"></span>
                </div>
              </div>
            </div>

            <div class="chat-input-wrap">
              <el-input 
                v-model="inputText" 
                type="textarea"
                :rows="2"
                resize="none"
                placeholder="在此输入你的心声..." 
                @keydown.enter.exact.prevent="handleSend"
              >
              </el-input>
              <div class="input-footer">
                <span class="input-hint">Enter 发送</span>
                <el-button type="primary" :loading="sending" @click="handleSend" circle>
                  <el-icon><Promotion /></el-icon>
                </el-button>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'
import { ElMessage } from 'element-plus'

// --- 基础状态 ---
const loading = ref(true)
const error = ref(null)
const allMemories = ref([])
const aiSummary = ref('')

// --- 图表 ---
const pieChartRef = ref(null)
const barChartRef = ref(null)
let pieChart = null
let barChart = null

// --- 对话 ---
const inputText = ref('')
const messages = ref([])
const sending = ref(false)
const chatWindowRef = ref(null)

// --- 数据获取 ---
const fetchData = async () => {
  loading.value = true
  error.value = null
  try {
    const res = await axios.get('/api/memories', { params: { current: 1, size: 200 } })
    if (res.data?.code === 200) {
      allMemories.value = res.data.data.records || res.data.data || []
      // 触发 AI 总结生成
      generateAISummary()
    } else {
      error.value = '记忆库读取失败'
    }
  } catch (e) {
    error.value = '无法连接到灵魂档案馆'
  } finally {
    loading.value = false
  }
}

// --- 计算统计 ---
const totalMemories = computed(() => allMemories.value.length)
const avgSentiment = computed(() => {
  if (allMemories.value.length === 0) return '0.00'
  const sum = allMemories.value.reduce((acc, m) => acc + (parseFloat(m.sentimentScore) || 0), 0)
  return (sum / allMemories.value.length).toFixed(2)
})

const keywordStats = computed(() => {
  const wordCount = {}
  allMemories.value.forEach(m => {
    const text = (m.tags || '') + ' ' + (m.title || '') + ' ' + (m.content || '')
    const words = text.replace(/[^\u4e00-\u9fa5]/g, ' ').split(/\s+/).filter(w => w.length >= 2)
    words.forEach(w => { wordCount[w] = (wordCount[w] || 0) + 1 })
  })
  return Object.entries(wordCount).map(([word, count]) => ({ word, count })).sort((a, b) => b.count - a.count)
})
const topKeyword = computed(() => keywordStats.value[0])

const sortedMemories = computed(() => [...allMemories.value].sort((a, b) => (a.createdAt || '') < (b.createdAt || '') ? -1 : 1))
const earlyMemories = computed(() => sortedMemories.value.slice(0, 5))
const recentMemories = computed(() => sortedMemories.value.slice(-5).reverse())

const trend = computed(() => {
  if (allMemories.value.length < 2) return { text: '尚无起伏', cls: 'trend-flat' }
  const earlyAvg = earlyMemories.value.reduce((a, m) => a + (parseFloat(m.sentimentScore) || 0), 0) / (earlyMemories.value.length || 1)
  const recentAvg = recentMemories.value.reduce((a, m) => a + (parseFloat(m.sentimentScore) || 0), 0) / (recentMemories.value.length || 1)
  const diff = recentAvg - earlyAvg
  if (diff > 0.1) return { text: '情绪升温', cls: 'trend-up' }
  if (diff < -0.1) return { text: '情绪转寒', cls: 'trend-down' }
  return { text: '心境平和', cls: 'trend-flat' }
})

// --- 图表初始化与更新 ---
const initCharts = () => {
  if (!pieChartRef.value || !barChartRef.value) return
  
  // 销毁旧实例
  if (pieChart) pieChart.dispose()
  if (barChart) barChart.dispose()

  pieChart = echarts.init(pieChartRef.value, 'dark')
  barChart = echarts.init(barChartRef.value, 'dark')
  
  updatePieChart()
  updateBarChart()
}

const updatePieChart = () => {
  let pos = 0, neg = 0, neu = 0
  allMemories.value.forEach(m => {
    const s = parseFloat(m.sentimentScore) || 0
    if (s > 0.3) pos++; else if (s < -0.3) neg++; else neu++
  })

  pieChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', backgroundColor: 'rgba(13, 17, 23, 0.8)', borderColor: 'rgba(255,255,255,0.1)' },
    series: [{
      type: 'pie', radius: ['45%', '75%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 6, borderColor: '#0d1117', borderWidth: 2 },
      data: [
        { value: pos, name: '积极', itemStyle: { color: '#2cb67d' } },
        { value: neu, name: '平静', itemStyle: { color: '#72757e' } },
        { value: neg, name: '消极', itemStyle: { color: '#ef4565' } }
      ],
      label: { show: false },
      emphasis: { label: { show: true, fontSize: '12', fontWeight: 'bold' } }
    }]
  })
}

const updateBarChart = () => {
  const top7 = keywordStats.value.slice(0, 7)
  barChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    xAxis: { 
      type: 'category', 
      data: top7.map(i => i.word), 
      axisLabel: { color: '#94a1b2', fontSize: 10, interval: 0 },
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } }
    },
    yAxis: { show: false },
    grid: { top: 10, bottom: 25, left: 10, right: 10 },
    series: [{
      type: 'bar', data: top7.map(i => i.count),
      barWidth: '40%',
      itemStyle: { 
        color: new echarts.graphic.LinearGradient(0,0,0,1, [{offset:0, color:'#7f5af0'}, {offset:1, color:'#2cb67d'}]),
        borderRadius: [4, 4, 0, 0]
      }
    }]
  })
}

// 监听数据变化并重新渲染图表
watch(allMemories, () => {
  if (allMemories.value.length > 0) {
    nextTick(() => initCharts())
  }
})

// --- 对话逻辑 ---
const handleSend = async () => {
  const text = inputText.value.trim()
  if (!text || sending.value) return
  messages.value.push({ role: 'user', content: text })
  inputText.value = ''
  sending.value = true
  scrollToBottom()
  try {
    const res = await axios.post('/api/ai-subsystem/dialogue', { message: text })
    if (res.data?.code === 200) {
      messages.value.push({ 
        role: 'ai', 
        content: res.data.data.reply, 
        canSave: true, 
        userText: text, 
        saving: false, 
        saved: false 
      })
    }
  } catch (e) {
    ElMessage.error('AI 正在沉思中，请稍后再试')
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

const saveAsMemory = async (msg, index) => {
  msg.saving = true
  try {
    const res = await axios.post('/api/memories', {
      title: '心灵对话重现', 
      content: `【倾诉】${msg.userText}\n【共鸣】${msg.content}`, 
      eventDate: new Date().toISOString().split('T')[0],
      sentimentScore: 0.1
    })
    if (res.data?.code === 200) { 
      msg.saved = true; 
      ElMessage.success('这段对话已封存于记忆陈列室'); 
    }
  } catch (e) {
    ElMessage.error('保存失败')
  } finally { msg.saving = false }
}

const scrollToBottom = () => nextTick(() => { 
  if (chatWindowRef.value) {
    chatWindowRef.value.scrollTo({
      top: chatWindowRef.value.scrollHeight,
      behavior: 'smooth'
    })
  }
})

const generateAISummary = async () => {
  if (allMemories.value.length === 0) return
  const words = keywordStats.value.slice(0, 5).map(k => k.word).join('、')
  aiSummary.value = `你近期灵魂深处共鸣最强的是「${words}」。整体心境呈现${trend.value.text}，平均情感值为 ${avgSentiment.value}。时光流转，每一段记忆都在悄悄塑造着今天的你。`
}

const scoreColor = (s) => {
  const v = parseFloat(s) || 0
  if (v > 0.3) return '#2cb67d'; if (v < -0.3) return '#ef4565'; return '#72757e'
}

const handleResize = () => {
  pieChart?.resize()
  barChart?.resize()
}

onMounted(() => {
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  pieChart?.dispose()
  barChart?.dispose()
})
</script>

<style lang="scss" scoped>
.echo-system-view { padding: 30px; max-width: 1400px; margin: 0 auto; min-height: calc(100vh - 70px); }

/* Glassmorphism Effect */
.glass-morphism {
  background: rgba(255, 255, 255, 0.03) !important;
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  box-shadow: 0 8px 32px 0 rgba(0, 0, 0, 0.37);
}

.echo-header { text-align: center; margin-bottom: 35px; }
.echo-main-title { 
  font-family: var(--font-title); font-size: 38px; letter-spacing: 14px; margin-bottom: 12px; 
  background: var(--grad-mystic); -webkit-background-clip: text; background-clip: text; -webkit-text-fill-color: transparent; 
  filter: drop-shadow(0 0 15px rgba(127, 90, 240, 0.4));
}
.header-divider { width: 220px; height: 1px; background: linear-gradient(to right, transparent, var(--accent-mystic), transparent); margin: 0 auto 12px; }
.echo-subtitle { color: #94a1b2; font-size: 15px; letter-spacing: 3px; font-weight: 300; }

.echo-stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin-bottom: 35px; }
.stat-card { 
  padding: 20px 25px; border-radius: 16px; display: flex; align-items: center; gap: 20px; position: relative; overflow: hidden;
  transition: transform 0.3s;
  &:hover { transform: translateY(-5px); }
  .stat-icon { font-size: 24px; opacity: 0.8; }
  .stat-num { font-size: 24px; font-weight: 700; color: #fff; display: block; margin-top: 4px; font-family: 'Courier New', monospace; }
  .stat-label { font-size: 12px; color: #72757e; letter-spacing: 1px; }
  .trend-up { color: #2cb67d; } .trend-down { color: #ef4565; } .trend-flat { color: #7f5af0; }
  .card-glow { position: absolute; top: -50%; left: -50%; width: 200%; height: 200%; background: radial-gradient(circle at center, rgba(127, 90, 240, 0.05), transparent 70%); pointer-events: none; }
}

.analysis-card { padding: 25px; height: auto; }
.charts-container { display: flex; gap: 30px; margin-bottom: 30px; min-height: 250px; }
.chart-item { 
  flex: 1; text-align: center; display: flex; flex-direction: column;
  .chart-label { font-size: 12px; color: #72757e; margin-bottom: 15px; display: block; letter-spacing: 1px; }
  .echart-box { flex: 1; min-height: 220px; width: 100%; }
}

.echo-compare-section {
  margin-top: 10px; padding-top: 20px; border-top: 1px solid rgba(255,255,255,0.05);
  .compare-header { display: flex; align-items: center; gap: 10px; color: #94a1b2; font-size: 13px; margin-bottom: 20px; font-family: var(--font-title); letter-spacing: 2px; }
}

.compare-flow {
  display: flex; align-items: stretch; gap: 20px;
  .flow-side { 
    flex: 1; background: rgba(0,0,0,0.2); border-radius: 12px; padding: 15px; border: 1px solid rgba(255,255,255,0.03);
    .side-tag { 
      font-size: 10px; color: #72757e; margin-bottom: 12px; text-transform: uppercase; letter-spacing: 2px;
      &.active { color: var(--accent-primary); font-weight: bold; }
    }
  }
  .mini-memory-list { display: flex; flex-direction: column; gap: 10px; }
  .mini-memory-item { 
    display: flex; align-items: center; gap: 10px; padding: 8px 12px; background: rgba(255,255,255,0.02); border-radius: 8px; font-size: 12px;
    .mini-title { color: #e2e8f0; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .sentiment-dot { width: 6px; height: 6px; border-radius: 50%; box-shadow: 0 0 5px rgba(0,0,0,0.5); }
  }
  .flow-connector { 
    display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 10px;
    .line { width: 1px; flex: 1; background: linear-gradient(to bottom, transparent, rgba(255,255,255,0.1), transparent); }
    .arrow { color: var(--accent-primary); font-size: 14px; opacity: 0.5; }
  }
}

.ai-insight-card { 
  padding: 25px; position: relative; overflow: hidden; margin-bottom: 25px;
  .insight-header { color: var(--accent-primary); font-weight: 600; font-size: 15px; display: flex; align-items: center; gap: 10px; margin-bottom: 12px; }
  .insight-text { color: #e2e8f0; font-size: 14px; line-height: 1.8; position: relative; z-index: 1; font-weight: 300; }
  .insight-bg-icon { position: absolute; right: -10px; bottom: -20px; font-size: 80px; opacity: 0.03; color: #fff; transform: rotate(-15deg); }
}

.dialogue-card { 
  padding: 0; display: flex; flex-direction: column; height: 100%; min-height: 650px;
  .card-header { padding: 25px; border-bottom: 1px solid rgba(255,255,255,0.05); }
}

.chat-container { 
  flex: 1; overflow-y: auto; padding: 25px; scroll-behavior: smooth;
  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 2px; }
}

.chat-placeholder {
  text-align: center; padding-top: 120px;
  .placeholder-icon { font-size: 40px; margin-bottom: 20px; opacity: 0.5; }
  h3 { font-family: var(--font-title); color: #fff; margin-bottom: 10px; letter-spacing: 2px; }
  p { color: #72757e; font-size: 13px; margin-bottom: 30px; }
  .suggested-prompts {
    display: flex; flex-direction: column; gap: 12px; align-items: center;
    span { 
      padding: 8px 20px; background: rgba(255,255,255,0.05); border: 1px solid rgba(255,255,255,0.1); 
      border-radius: 20px; font-size: 12px; color: #94a1b2; cursor: pointer; transition: all 0.3s;
      &:hover { background: rgba(127, 90, 240, 0.1); border-color: var(--accent-primary); color: #fff; }
    }
  }
}

.chat-row { display: flex; margin-bottom: 25px; gap: 15px; }
.row-user { flex-direction: row-reverse; }
.chat-avatar { font-size: 18px; width: 32px; height: 32px; background: rgba(255,255,255,0.05); border-radius: 50%; display: flex; align-items: center; justify-content: center; border: 1px solid rgba(255,255,255,0.1); flex-shrink: 0; }
.chat-bubble { 
  max-width: 80%; padding: 12px 18px; border-radius: 16px; font-size: 14px; line-height: 1.6; position: relative;
  .bubble-text { margin: 0; white-space: pre-wrap; }
}
.bubble-user { background: var(--grad-mystic); color: #fff; border-bottom-right-radius: 2px; }
.bubble-ai { 
  background: rgba(255,255,255,0.07); color: #e2e8f0; border: 1px solid rgba(255,255,255,0.1); border-bottom-left-radius: 2px; 
  &.typing {
    display: flex; gap: 4px; padding: 15px 20px;
    .dot { width: 6px; height: 6px; background: var(--accent-primary); border-radius: 50%; animation: typing 1.4s infinite; opacity: 0.3; }
    .dot:nth-child(2) { animation-delay: 0.2s; }
    .dot:nth-child(3) { animation-delay: 0.4s; }
  }
}
.bubble-actions { margin-top: 10px; padding-top: 10px; border-top: 1px solid rgba(255,255,255,0.05); }
.save-btn-mini { 
  background: transparent; border: 1px solid rgba(127, 90, 240, 0.4); color: var(--accent-primary); 
  padding: 4px 10px; border-radius: 4px; font-size: 11px; cursor: pointer; display: flex; align-items: center; gap: 6px;
  transition: all 0.3s;
  &:hover { background: rgba(127, 90, 240, 0.1); border-color: var(--accent-primary); }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}

.chat-input-wrap { 
  padding: 20px 25px; border-top: 1px solid rgba(255,255,255,0.05); background: rgba(0,0,0,0.1);
  :deep(.el-textarea__inner) { 
    background: transparent; border: none; color: #fff; font-size: 14px; padding: 10px 0; 
    &::placeholder { color: #555; }
    &:focus { box-shadow: none; }
  }
  .input-footer { 
    display: flex; justify-content: space-between; align-items: center; margin-top: 10px; 
    .input-hint { font-size: 11px; color: #555; }
  }
}

.echo-loading { 
  text-align: center; padding: 150px 0; color: #94a1b2; position: relative;
  .loading-scanner { 
    position: absolute; top: 0; left: 0; width: 100%; height: 2px; background: var(--grad-mystic); 
    animation: scan 2s linear infinite; box-shadow: 0 0 15px var(--accent-primary);
  }
  .loading-icon { font-size: 40px; margin-bottom: 20px; animation: rotating 2s linear infinite; color: var(--accent-primary); }
  span { font-family: var(--font-title); letter-spacing: 4px; }
}

@keyframes scan { 0% { top: 20%; opacity: 0; } 50% { opacity: 1; } 100% { top: 80%; opacity: 0; } }
@keyframes rotating { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
@keyframes typing { 0%, 100% { transform: translateY(0); opacity: 0.3; } 50% { transform: translateY(-5px); opacity: 1; } }

/* Element Plus Overrides */
:deep(.el-button--primary) { background: var(--grad-mystic); border: none; &:hover { opacity: 0.9; } }
</style>
