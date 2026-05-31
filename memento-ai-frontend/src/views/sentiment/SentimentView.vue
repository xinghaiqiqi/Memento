<template>
  <div class="advisor-page museum-fade-in">
    <!-- 页头 -->
    <header class="page-hero">
      <div class="hero-badge">MEMORY ADVISOR</div>
      <h1 class="hero-title">记忆顾问</h1>
      <p class="hero-subtitle">探索记忆的奥秘，聆听智慧的低语</p>
      <div class="hero-divider">
        <span class="divider-gem"></span>
      </div>
    </header>

    <!-- 步骤引导 -->
    <div class="step-flow">
      <div class="step-item" :class="{ active: !!selectedDate, done: !!selectedDate }">
        <span class="step-num">01</span>
        <span class="step-label">选择日期</span>
      </div>
      <div class="step-line" :class="{ active: !!selectedDate }"></div>
      <div class="step-item" :class="{ active: !!selectedMemory, done: !!selectedMemory }">
        <span class="step-num">02</span>
        <span class="step-label">挑选记忆</span>
      </div>
      <div class="step-line" :class="{ active: !!selectedMemory }"></div>
      <div class="step-item" :class="{ active: !!aiResponse, done: !!aiResponse }">
        <span class="step-num">03</span>
        <span class="step-label">获取启示</span>
      </div>
    </div>

    <div class="main-grid">
      <!-- 左栏：日期 + 记忆列表 -->
      <aside class="left-panel">
        <section class="glass-panel date-panel">
          <div class="panel-head">
            <div class="panel-icon purple"><el-icon><Calendar /></el-icon></div>
            <div>
              <h2 class="panel-title">时光回溯</h2>
              <p class="panel-desc">选定某一天，唤醒沉睡的记忆</p>
            </div>
          </div>

          <div class="date-row">
            <el-date-picker
              v-model="selectedDate"
              type="date"
              placeholder="选择日期"
              value-format="YYYY-MM-DD"
              :disabled-date="disabledDate"
              class="date-picker"
              @change="handleDateChange"
            />
            <el-button class="explore-btn" @click="fetchMemories" :loading="isLoading">
              <el-icon><Search /></el-icon>
            </el-button>
          </div>

          <div v-if="availableDates.length" class="date-chips">
            <span class="chips-label">有记忆的日期</span>
            <div class="chips-wrap">
              <button
                v-for="date in availableDates.slice(0, 10)"
                :key="date"
                class="chip"
                :class="{ selected: selectedDate === date }"
                @click="selectAvailableDate(date)"
              >
                {{ formatChipDate(date) }}
              </button>
            </div>
          </div>
        </section>

        <!-- 记忆列表 -->
        <section class="glass-panel list-panel" v-loading="isLoading">
          <div class="panel-head compact">
            <div class="panel-icon gold"><el-icon><Collection /></el-icon></div>
            <div class="panel-head-text">
              <h2 class="panel-title">当日记忆</h2>
              <span v-if="memories.length" class="count-badge">{{ memories.length }} 条</span>
            </div>
          </div>

          <div v-if="memories.length" class="memory-timeline">
            <div
              v-for="(memory, idx) in memories"
              :key="memory.id"
              class="timeline-item"
              :class="{ active: selectedMemory?.id === memory.id }"
              @click="selectMemory(memory)"
            >
              <div class="timeline-rail">
                <span class="rail-dot" :style="{ background: getSentimentColor(memory.sentimentScore) }"></span>
                <span v-if="idx < memories.length - 1" class="rail-line"></span>
              </div>
              <div class="timeline-body">
                <div class="item-top">
                  <h4 class="item-title">{{ memory.title }}</h4>
                  <span class="sentiment-pill" :style="{ color: getSentimentColor(memory.sentimentScore), borderColor: getSentimentColor(memory.sentimentScore) }">
                    {{ getSentimentLabel(memory.sentimentScore) }}
                  </span>
                </div>
                <p class="item-preview">{{ formatPreview(memory.content) }}</p>
                <div v-if="formatTags(memory.tags).length" class="item-tags">
                  <span v-for="tag in formatTags(memory.tags)" :key="tag" class="tag">#{{ tag }}</span>
                </div>
              </div>
            </div>
          </div>

          <div v-else-if="showEmpty" class="panel-empty">
            <el-icon class="empty-icon"><Moon /></el-icon>
            <p>这一天还没有记忆</p>
            <span>试试点击上方日期标签</span>
          </div>

          <div v-else class="panel-placeholder">
            <el-icon><Pointer /></el-icon>
            <p>选择日期后，记忆将在此呈现</p>
          </div>
        </section>
      </aside>

      <!-- 右栏：顾问对话区 -->
      <main class="right-panel">
        <div class="dialog-stage">
          <section class="glass-panel advisor-panel">
          <div class="panel-head">
            <div class="panel-icon warm"><el-icon><MagicStick /></el-icon></div>
            <div>
              <h2 class="panel-title">智慧对话</h2>
              <p class="panel-desc">基于您的记忆，AI 将给出温暖的启示</p>
            </div>
          </div>

          <!-- 选中记忆展示 -->
          <div v-if="selectedMemory" class="memory-showcase">
            <div class="showcase-accent" :style="{ background: getSentimentColor(selectedMemory.sentimentScore) }"></div>
            <div class="showcase-inner">
              <div class="showcase-meta">
                <span class="showcase-date">{{ selectedMemory.eventDate }}</span>
                <span class="showcase-badge" :style="{ background: getSentimentColor(selectedMemory.sentimentScore) }">
                  {{ getSentimentLabel(selectedMemory.sentimentScore) }}
                </span>
              </div>
              <h3 class="showcase-title">{{ selectedMemory.title }}</h3>
              <p class="showcase-content">{{ selectedMemory.content }}</p>
              <div v-if="selectedMemory.photoUrl" class="showcase-photo">
                <img :src="selectedMemory.photoUrl" alt="记忆图片" />
              </div>
            </div>
          </div>

          <div v-else class="showcase-empty">
            <div class="empty-orb"></div>
            <p class="empty-title">等待一段记忆</p>
            <p class="empty-desc">从左侧列表中选择，开启对话</p>
          </div>

          <!-- 提问区 -->
          <div class="chat-section">
            <div class="chat-label">
              <el-icon><ChatDotRound /></el-icon>
              <span>向 AI 提问</span>
            </div>
            <el-input
              v-model="question"
              type="textarea"
              :rows="3"
              placeholder="例如：这段记忆对我意味着什么？"
              class="chat-input"
              resize="none"
              @keyup.enter.ctrl="askAI"
            />
            <div class="quick-row">
              <button
                v-for="q in quickQuestions"
                :key="q"
                class="quick-chip"
                :disabled="!selectedMemory"
                @click="question = q; askAI()"
              >
                {{ q }}
              </button>
            </div>
            <el-button
              class="submit-btn"
              :loading="isAsking"
              :disabled="!selectedMemory || !question.trim()"
              @click="askAI"
            >
              <el-icon><Opportunity /></el-icon>
              获取智慧启示
            </el-button>
          </div>

          <!-- AI 回答 -->
          <transition name="fade-up">
            <div v-if="aiResponse" class="response-block">
              <div class="response-head">
                <span class="response-quote">"</span>
                <span class="response-label">智慧启示</span>
              </div>
              <p class="response-text">{{ aiResponse }}</p>
              <div class="response-foot">
                <span class="foot-line"></span>
                <span class="foot-sign">— 记忆顾问</span>
              </div>
            </div>
          </transition>
          </section>
        </div>
      </main>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import dayjs from 'dayjs'
import { ElMessage } from 'element-plus'
import {
  Search, Calendar, Collection, MagicStick, Opportunity,
  ChatDotRound, Moon, Pointer
} from '@element-plus/icons-vue'

const selectedDate = ref('')
const availableDates = ref([])
const memories = ref([])
const selectedMemory = ref(null)
const question = ref('')
const aiResponse = ref('')
const isLoading = ref(false)
const isAsking = ref(false)
const showEmpty = ref(false)

const quickQuestions = [
  '这段记忆的意义是什么？',
  '我能从中学到什么？',
  '未来我该怎么做？',
  '给我一个生活建议'
]

const disabledDate = (time) => time.getTime() > Date.now()

const formatDateParam = (date) => {
  if (!date) return ''
  if (typeof date === 'string') return date.slice(0, 10)
  return dayjs(date).format('YYYY-MM-DD')
}

const formatChipDate = (date) => dayjs(date).format('MM.DD')

const formatPreview = (content) => {
  if (!content) return '暂无内容'
  const text = content.trim()
  return text.length > 60 ? text.slice(0, 60) + '…' : text
}

const fetchAvailableDates = async () => {
  try {
    const res = await axios.get('/api/sentiment/dates')
    if (res.data.code === 200) {
      availableDates.value = res.data.data || []
    }
  } catch (error) {
    console.error('获取可用日期失败:', error)
  }
}

const selectAvailableDate = (date) => {
  selectedDate.value = date
  fetchMemories()
}

const handleDateChange = () => {
  if (selectedDate.value) fetchMemories()
}

const fetchMemories = async () => {
  const dateStr = formatDateParam(selectedDate.value)
  if (!dateStr) {
    ElMessage.warning('请先选择日期')
    return
  }

  isLoading.value = true
  showEmpty.value = false

  try {
    const res = await axios.get('/api/sentiment/memories', { params: { date: dateStr } })
    if (res.data.code === 200) {
      memories.value = res.data.data || []
      selectedMemory.value = null
      aiResponse.value = ''
      showEmpty.value = memories.value.length === 0
    } else {
      ElMessage.error(res.data.message || '无法获取记忆数据')
      memories.value = []
      showEmpty.value = true
    }
  } catch (error) {
    console.error('获取记忆失败:', error)
    ElMessage.error('无法获取记忆数据')
    memories.value = []
    showEmpty.value = true
  } finally {
    isLoading.value = false
  }
}

onMounted(() => fetchAvailableDates())

const selectMemory = (memory) => {
  selectedMemory.value = memory
  aiResponse.value = ''
}

const formatTags = (tags) => tags ? tags.split(',').map(t => t.trim()).filter(Boolean).slice(0, 3) : []

const getSentimentColor = (score) => {
  if (!score && score !== 0) return '#9ca3af'
  if (score > 0.5) return '#ef4444'
  if (score > 0.2) return '#f97316'
  if (score > -0.2) return '#eab308'
  if (score > -0.5) return '#84cc16'
  return '#3b82f6'
}

const getSentimentLabel = (score) => {
  if (!score && score !== 0) return '未知'
  if (score > 0.3) return '积极'
  if (score < -0.3) return '消极'
  return '中性'
}

const askAI = async () => {
  if (!selectedMemory.value) {
    ElMessage.warning('请先选择一段记忆')
    return
  }
  if (!question.value.trim()) {
    ElMessage.warning('请输入您的问题')
    return
  }

  isAsking.value = true

  try {
    const res = await axios.get('/api/sentiment/advice', {
      params: {
        memoryContent: selectedMemory.value.content,
        question: question.value
      }
    })
    if (res.data.code === 200) {
      aiResponse.value = res.data.data.advice
    }
  } catch (error) {
    console.error('获取AI建议失败:', error)
    ElMessage.error('无法获取AI建议')
    aiResponse.value = '很抱歉，暂时无法获取建议，请稍后再试。'
  } finally {
    isAsking.value = false
  }
}
</script>

<style lang="scss" scoped>
.advisor-page {
  padding: 40px 24px 56px;
  max-width: 1280px;
  margin: 0 auto;
}

/* ── 页头 ── */
.page-hero {
  text-align: center;
  margin-bottom: 36px;

  .hero-badge {
    display: inline-block;
    font-size: 11px;
    letter-spacing: 5px;
    color: var(--accent-primary);
    opacity: 0.7;
    margin-bottom: 12px;
  }

  .hero-title {
    font-family: var(--font-title);
    font-size: 44px;
    margin: 0;
    background: linear-gradient(180deg, #fff 30%, #94a1b2);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .hero-subtitle {
    color: #94a1b2;
    font-size: 14px;
    letter-spacing: 3px;
    margin: 14px 0 0;
  }

  .hero-divider {
    display: flex;
    justify-content: center;
    margin-top: 28px;

    .divider-gem {
      width: 6px;
      height: 6px;
      background: var(--accent-primary);
      transform: rotate(45deg);
      box-shadow: 0 0 14px var(--accent-primary);
    }
  }
}

/* ── 步骤条 ── */
.step-flow {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin-bottom: 40px;

  .step-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    opacity: 0.35;
    transition: opacity 0.3s;

    &.active, &.done { opacity: 1; }

    .step-num {
      font-family: var(--font-title);
      font-size: 13px;
      color: var(--accent-primary);
      letter-spacing: 1px;
    }

    .step-label {
      font-size: 12px;
      color: #94a1b2;
      letter-spacing: 1px;
    }
  }

  .step-line {
    width: 80px;
    height: 1px;
    background: rgba(255, 255, 255, 0.08);
    margin: 0 16px 18px;
    transition: background 0.4s;

    &.active {
      background: linear-gradient(90deg, var(--accent-primary), rgba(127, 90, 240, 0.2));
    }
  }
}

/* ── 主布局：左右等比，对话区固定宽度 ── */
.main-grid {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  gap: 24px;

  @media (max-width: 960px) {
    flex-direction: column;
    align-items: stretch;
  }
}

.left-panel {
  width: 520px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 18px;
  position: sticky;
  top: 24px;

  @media (max-width: 960px) {
    width: 100%;
    position: static;
  }
}

.right-panel {
  width: 680px;
  flex-shrink: 0;

  @media (max-width: 960px) {
    width: 100%;
  }
}

.dialog-stage {
  position: relative;

  &::before {
    content: '';
    position: absolute;
    inset: -1px;
    border-radius: 22px;
    background: linear-gradient(160deg, rgba(127, 90, 240, 0.18), rgba(249, 115, 22, 0.08));
    opacity: 0.6;
    z-index: 0;
  }
}

/* ── 通用玻璃面板 ── */
.glass-panel {
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.025);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 20px;
  padding: 22px;
  backdrop-filter: blur(12px);
  transition: border-color 0.3s;

  &:hover {
    border-color: rgba(127, 90, 240, 0.2);
  }
}

.panel-head {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  margin-bottom: 22px;

  &.compact { margin-bottom: 18px; }

  .panel-icon {
    width: 40px;
    height: 40px;
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    font-size: 18px;
    color: #fff;

    &.purple { background: rgba(127, 90, 240, 0.2); color: #a78bfa; }
    &.gold   { background: rgba(191, 149, 63, 0.15); color: var(--gold-mid); }
    &.warm   { background: rgba(249, 115, 22, 0.15); color: #fb923c; }
  }

  .panel-title {
    margin: 0;
    font-family: var(--font-title);
    font-size: 17px;
    color: #fff;
    letter-spacing: 1px;
  }

  .panel-desc {
    margin: 4px 0 0;
    font-size: 12px;
    color: #64748b;
  }

  .panel-head-text {
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .count-badge {
    font-size: 12px;
    color: #a78bfa;
    background: rgba(127, 90, 240, 0.12);
    padding: 2px 10px;
    border-radius: 20px;
  }
}

/* ── 日期面板 ── */
.date-row {
  display: flex;
  gap: 10px;

  .date-picker {
    flex: 1;

    :deep(.el-input__wrapper) {
      background: rgba(0, 0, 0, 0.3);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 12px;
      box-shadow: none;

      &:hover, &.is-focus {
        border-color: rgba(127, 90, 240, 0.5);
      }
    }

    :deep(.el-input__inner) { color: #fff; }
  }

  .explore-btn {
    width: 44px;
    height: 44px;
    padding: 0;
    border: none;
    border-radius: 12px;
    background: linear-gradient(135deg, #7f5af0, #9d4edd);
    color: #fff;
    flex-shrink: 0;

    &:hover {
      transform: translateY(-1px);
      box-shadow: 0 6px 18px rgba(127, 90, 240, 0.45);
    }
  }
}

.date-chips {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);

  .chips-label {
    display: block;
    font-size: 11px;
    color: #64748b;
    letter-spacing: 1px;
    margin-bottom: 10px;
    text-transform: uppercase;
  }

  .chips-wrap {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .chip {
    background: rgba(127, 90, 240, 0.08);
    border: 1px solid rgba(127, 90, 240, 0.2);
    color: #a78bfa;
    border-radius: 20px;
    padding: 5px 14px;
    font-size: 13px;
    cursor: pointer;
    transition: all 0.2s;
    font-family: var(--font-main);

    &:hover, &.selected {
      background: rgba(127, 90, 240, 0.22);
      border-color: rgba(127, 90, 240, 0.5);
      color: #fff;
    }
  }
}

/* ── 记忆时间线 ── */
.list-panel {
  min-height: 280px;
}

.memory-timeline {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 420px;
  overflow-y: auto;
  padding-right: 4px;

  &::-webkit-scrollbar { width: 4px; }
  &::-webkit-scrollbar-thumb {
    background: rgba(127, 90, 240, 0.4);
    border-radius: 2px;
  }
}

.timeline-item {
  display: flex;
  gap: 14px;
  padding: 14px 12px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.25s;
  border: 1px solid transparent;

  &:hover {
    background: rgba(255, 255, 255, 0.04);
  }

  &.active {
    background: rgba(127, 90, 240, 0.1);
    border-color: rgba(127, 90, 240, 0.35);
  }

  .timeline-rail {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 12px;
    flex-shrink: 0;
    padding-top: 6px;

    .rail-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      box-shadow: 0 0 8px currentColor;
    }

    .rail-line {
      flex: 1;
      width: 1px;
      background: rgba(255, 255, 255, 0.08);
      margin-top: 6px;
      min-height: 20px;
    }
  }

  .timeline-body {
    flex: 1;
    min-width: 0;
  }

  .item-top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 8px;
    margin-bottom: 6px;
  }

  .item-title {
    margin: 0;
    font-size: 15px;
    color: #fff;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .sentiment-pill {
    flex-shrink: 0;
    font-size: 11px;
    padding: 2px 8px;
    border-radius: 20px;
    border: 1px solid;
    opacity: 0.85;
  }

  .item-preview {
    margin: 0 0 8px;
    font-size: 13px;
    color: #64748b;
    line-height: 1.55;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .item-tags {
    display: flex;
    gap: 6px;

    .tag {
      font-size: 11px;
      color: #7f5af0;
      opacity: 0.75;
    }
  }
}

.panel-empty, .panel-placeholder {
  text-align: center;
  padding: 40px 16px;
  color: #64748b;

  .empty-icon { font-size: 36px; color: #7f5af0; opacity: 0.4; margin-bottom: 12px; }
  p { margin: 0 0 6px; color: #94a1b2; font-size: 14px; }
  span { font-size: 12px; }

  .el-icon { font-size: 32px; color: #7f5af0; opacity: 0.3; margin-bottom: 10px; }
}

/* ── 右侧顾问面板 ── */
.advisor-panel {
  min-height: 560px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.memory-showcase {
  position: relative;
  border-radius: 14px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.28);

  .showcase-accent {
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 3px;
  }

  .showcase-inner {
    padding: 18px 18px 18px 22px;
  }

  .showcase-meta {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 10px;
  }

  .showcase-date {
    font-size: 11px;
    color: #64748b;
    letter-spacing: 1px;
  }

  .showcase-badge {
    font-size: 10px;
    color: #fff;
    padding: 2px 8px;
    border-radius: 20px;
  }

  .showcase-title {
    margin: 0 0 10px;
    font-family: var(--font-title);
    font-size: 18px;
    color: #fff;
    letter-spacing: 0.5px;
    line-height: 1.4;
  }

  .showcase-content {
    margin: 0;
    font-size: 14px;
    color: #94a1b2;
    line-height: 1.75;
    white-space: pre-wrap;
    max-height: 160px;
    overflow-y: auto;
    padding-right: 4px;

    &::-webkit-scrollbar { width: 3px; }
    &::-webkit-scrollbar-thumb {
      background: rgba(127, 90, 240, 0.35);
      border-radius: 2px;
    }
  }

  .showcase-photo {
    margin-top: 14px;
    border-radius: 10px;
    overflow: hidden;

    img {
      width: 100%;
      max-height: 160px;
      object-fit: cover;
    }
  }
}

.showcase-empty {
  text-align: center;
  padding: 36px 16px;
  position: relative;
  border-radius: 14px;
  background: rgba(0, 0, 0, 0.15);
  border: 1px dashed rgba(255, 255, 255, 0.06);

  .empty-orb {
    width: 52px;
    height: 52px;
    margin: 0 auto 16px;
    border-radius: 50%;
    background: radial-gradient(circle, rgba(127, 90, 240, 0.25) 0%, transparent 70%);
    animation: pulse 3s ease-in-out infinite;
  }

  .empty-title {
    margin: 0 0 6px;
    color: #94a1b2;
    font-size: 15px;
  }

  .empty-desc {
    margin: 0;
    color: #64748b;
    font-size: 12px;
  }
}

@keyframes pulse {
  0%, 100% { transform: scale(1); opacity: 0.6; }
  50% { transform: scale(1.08); opacity: 1; }
}

/* ── 对话区 ── */
.chat-section {
  background: rgba(0, 0, 0, 0.22);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 14px;
  padding: 18px;

  .chat-label {
    display: flex;
    align-items: center;
    gap: 8px;
    color: #fff;
    font-size: 13px;
    font-weight: 600;
    margin-bottom: 12px;
    letter-spacing: 1px;
  }

  .chat-input {
    margin-bottom: 12px;

    :deep(.el-textarea__inner) {
      background: rgba(255, 255, 255, 0.04);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 10px;
      color: #fff;
      box-shadow: none;
      line-height: 1.65;
      font-size: 14px;

      &:focus {
        border-color: rgba(127, 90, 240, 0.5);
      }
    }
  }

  .quick-row {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 8px;
    margin-bottom: 14px;
  }

  .quick-chip {
    background: rgba(127, 90, 240, 0.08);
    border: 1px solid rgba(127, 90, 240, 0.18);
    color: #a78bfa;
    border-radius: 10px;
    padding: 8px 10px;
    font-size: 11px;
    cursor: pointer;
    transition: all 0.2s;
    font-family: var(--font-main);
    text-align: left;
    line-height: 1.4;

    &:hover:not(:disabled) {
      background: rgba(127, 90, 240, 0.2);
      color: #fff;
    }

    &:disabled {
      opacity: 0.35;
      cursor: not-allowed;
    }
  }

  .submit-btn {
    width: 100%;
    height: 42px;
    border: none;
    border-radius: 10px;
    background: linear-gradient(135deg, #f97316 0%, #ef4444 100%);
    color: #fff;
    font-size: 14px;
    font-weight: 600;
    letter-spacing: 1px;

    &:hover:not(:disabled) {
      transform: translateY(-1px);
      box-shadow: 0 6px 20px rgba(249, 115, 22, 0.3);
    }

    &:disabled { opacity: 0.45; }
  }
}

/* ── AI 回答 ── */
.response-block {
  background: linear-gradient(135deg, rgba(127, 90, 240, 0.08) 0%, rgba(157, 78, 221, 0.04) 100%);
  border: 1px solid rgba(127, 90, 240, 0.16);
  border-radius: 14px;
  padding: 20px 20px 16px;
  position: relative;

  .response-head {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 12px;
  }

  .response-quote {
    font-family: Georgia, serif;
    font-size: 36px;
    line-height: 1;
    color: rgba(127, 90, 240, 0.35);
    margin-top: -6px;
  }

  .response-label {
    font-family: var(--font-title);
    font-size: 14px;
    color: #fff;
    letter-spacing: 1px;
  }

  .response-text {
    margin: 0;
    font-size: 14px;
    color: #cbd5e1;
    line-height: 1.85;
    font-family: 'Noto Serif SC', serif;
  }

  .response-foot {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-top: 16px;

    .foot-line {
      flex: 1;
      height: 1px;
      background: rgba(255, 255, 255, 0.06);
    }

    .foot-sign {
      font-size: 11px;
      color: #64748b;
      letter-spacing: 1px;
      font-style: italic;
    }
  }
}

.fade-up-enter-active {
  transition: all 0.5s ease;
}
.fade-up-enter-from {
  opacity: 0;
  transform: translateY(16px);
}
</style>
