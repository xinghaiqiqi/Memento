<template>
  <div class="museum-dashboard museum-fade-in">
    <!-- 顶部欢迎区：私人博物馆铭牌 -->
    <div class="museum-plaque">
      <div class="plaque-content">
        <h1 class="museum-name">The Memento Archive</h1>
        <div class="plaque-divider"></div>
        <p class="curator-info">馆长: {{ userStore.userInfo.nickname }}</p>
        <p class="curator-date">记录始于 {{ startDate }}</p>
      </div>
    </div>

    <!-- 核心展厅：艺术化统计 -->
    <el-row :gutter="30" class="gallery-hall">
      <el-col :span="6" v-for="item in stats" :key="item.label">
        <div class="exhibit-case">
          <div class="case-header">
            <el-icon><component :is="item.icon" /></el-icon>
            <span class="label">{{ item.label }}</span>
          </div>
          <div class="case-value">{{ item.value }}</div>
          <div class="case-footer">
            <span :class="['trend', item.trend > 0 ? 'up' : 'down']">
              {{ item.trend > 0 ? '↗' : '↘' }} {{ Math.abs(item.trend) }}%
            </span>
          </div>
          <div class="case-glow"></div>
        </div>
      </el-col>
    </el-row>

    <!-- 重点陈列：最近记录与动态 -->
    <el-row :gutter="30" class="curated-section">
      <el-col :span="15">
        <div class="museum-curio-cabinet">
          <div class="cabinet-header">
            <h3>近期馆藏珍品</h3>
            <button class="museum-btn-link" @click="router.push('/memories')">浏览全馆</button>
          </div>
          <div class="cabinet-body">
            <el-table v-loading="loading" :data="recentMemories" class="museum-table">
              <el-table-column label="影像" width="80">
                <template #default="{ row }">
                  <div v-if="row.photoUrl" class="table-photo-preview" :style="{ backgroundImage: `url(${row.photoUrl})` }"></div>
                  <div v-else class="table-photo-none"></div>
                </template>
              </el-table-column>
              <el-table-column prop="eventDate" label="时间印记" width="150" />
              <el-table-column prop="title" label="记忆之名" show-overflow-tooltip />
              <el-table-column prop="sentimentScore" label="情感色彩" width="120">
                <template #default="{ row }">
                  <div class="sentiment-dot" :style="{ background: getSentimentColor(row.sentimentScore) }"></div>
                  <span class="sentiment-label">{{ getSentimentLabel(row.sentimentScore) }}</span>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </div>
      </el-col>
      
      <el-col :span="9">
        <div class="museum-status-monitor">
          <div class="monitor-header">
            <h3>博物馆运行状态</h3>
          </div>
          <div class="monitor-body">
            <div class="status-gauge">
              <div class="gauge-info">
                <span class="label">AI 核心连接</span>
                <el-tag effect="dark" class="museum-tag">ACTIVE</el-tag>
              </div>
              <div class="gauge-info">
                <span class="label">存储空间</span>
                <span class="value">124 MB / 1 GB</span>
              </div>
              <div class="progress-bar-container">
                <div class="progress-fill" style="width: 12%"></div>
              </div>
            </div>
            
            <div class="museum-inspiration">
              <p class="quote">“我们不是在记录过去，而是在为未来的自己搭建一座避难所。”</p>
              <span class="author">— Memento AI</span>
            </div>
            
            <div class="museum-actions">
              <button class="action-btn-gold" @click="router.push('/memories')">
                <el-icon><Plus /></el-icon> 增添馆藏
              </button>
              <button class="action-btn-glass" @click="router.push('/narrative')">
                <el-icon><MagicStick /></el-icon> 编织叙事
              </button>
            </div>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const startDate = ref('...')

const stats = ref([
  { label: '记忆结晶', value: '0', icon: 'Collection', trend: 0 },
  { label: '重要锚点', value: '0', icon: 'Flag', trend: 0 },
  { label: '编织叙事', value: '0', icon: 'Notebook', trend: 0 },
  { label: '情感共鸣', value: '0%', icon: 'Connection', trend: 0 }
])

const recentMemories = ref([])
const loading = ref(false)

const fetchDashboardData = async () => {
  loading.value = true
  try {
    const [statsRes, recentRes] = await Promise.all([
      axios.get('/api/dashboard/stats'),
      axios.get('/api/dashboard/recent')
    ])
    
    // 更新统计数据
    const statsData = statsRes.data.data
    stats.value[0].value = statsData.totalMemories
    stats.value[1].value = statsData.totalMilestones
    stats.value[2].value = statsData.totalNarratives
    stats.value[3].value = statsData.resonance
    
    // 更新起始日期
    if (statsData.earliestDate) {
      startDate.value = dayjs(statsData.earliestDate).format('YYYY / MM / DD')
    } else {
      startDate.value = dayjs().format('YYYY / MM / DD')
    }
    
    // 更新最近记忆
    recentMemories.value = recentRes.data.data
  } catch (error) {
    console.error('获取首页数据失败:', error)
    ElMessage.error('获取首页数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchDashboardData()
})

const getSentimentColor = (score) => {
  if (score > 0.3) return '#2cb67d'
  if (score < -0.3) return '#ef4565'
  return '#72757e'
}

const getSentimentLabel = (score) => {
  if (score > 0.3) return '悦'
  if (score < -0.3) return '忧'
  return '平'
}
</script>

<style lang="scss" scoped>
.museum-dashboard {
  padding: 40px;
  max-width: 1400px;
  margin: 0 auto;
}

.museum-plaque {
  background: rgba(15, 18, 24, 0.4);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 50px;
  text-align: center;
  position: relative;
  margin-bottom: 60px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5), inset 0 0 40px rgba(157, 80, 187, 0.05);
  
  &::before, &::after {
    content: '◈';
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    color: var(--accent-mystic);
    font-size: 24px;
    text-shadow: 0 0 15px rgba(157, 80, 187, 0.4);
  }
  &::before { left: 40px; }
  &::after { right: 40px; }

  .museum-name {
    font-family: var(--font-title);
    font-size: 56px;
    letter-spacing: 16px;
    margin: 0 0 25px 0;
    background: var(--grad-mystic);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    filter: drop-shadow(0 0 10px rgba(157, 80, 187, 0.3));
  }
  
  .plaque-divider {
    width: 300px;
    height: 1px;
    background: linear-gradient(to right, transparent, var(--accent-mystic), transparent);
    margin: 0 auto 25px;
    opacity: 0.6;
  }

  .curator-info, .curator-date {
    margin: 8px 0;
    font-family: var(--font-title);
    letter-spacing: 3px;
    font-size: 15px;
    color: #a0aec0;
  }
}

.exhibit-case {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 35px;
  border-radius: 24px;
  position: relative;
  overflow: hidden;
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
  
  &:hover {
    transform: translateY(-12px);
    background: rgba(157, 80, 187, 0.05);
    border-color: var(--accent-mystic);
    box-shadow: 0 25px 50px rgba(0, 0, 0, 0.4);
    .case-glow { opacity: 0.6; }
    .case-header .el-icon { transform: scale(1.2) rotate(10deg); }
  }

  .case-header {
    display: flex;
    align-items: center;
    gap: 12px;
    color: var(--accent-mystic);
    margin-bottom: 20px;
    .el-icon { font-size: 20px; transition: transform 0.4s; }
    .label { font-size: 13px; letter-spacing: 2px; text-transform: uppercase; color: #718096; font-weight: 600; }
  }

  .case-value {
    font-size: 48px;
    font-weight: 700;
    font-family: var(--font-title);
    margin-bottom: 15px;
    background: linear-gradient(to bottom, #fff, #cbd5e0);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }

  .case-glow {
    position: absolute;
    bottom: -50px; right: -50px;
    width: 150px; height: 150px;
    background: radial-gradient(circle, var(--accent-mystic), transparent 70%);
    opacity: 0.2;
    filter: blur(40px);
    transition: opacity 0.5s;
    pointer-events: none;
  }
}

/* Cabinet & Monitor */
.museum-curio-cabinet, .museum-status-monitor {
  background: rgba(15, 18, 24, 0.3);
  backdrop-filter: blur(30px);
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 32px;
  padding: 35px;
  height: 100%;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);

  h3 {
    font-family: var(--font-title);
    font-size: 22px;
    letter-spacing: 2px;
    margin-bottom: 30px;
    background: linear-gradient(to right, #fff, #718096);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
  }
}

.curated-section {
  margin-top: 60px;
}

.cabinet-header, .monitor-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  h3 { font-family: var(--font-title); letter-spacing: 2px; margin: 0; }
}

.museum-btn-link {
  background: transparent;
  border: none;
  color: var(--accent-primary);
  font-family: var(--font-title);
  cursor: pointer;
  &:hover { color: #fff; text-decoration: underline; }
}

.museum-table {
  background: transparent !important;
  --el-table-border-color: rgba(255, 255, 255, 0.05);
  --el-table-header-bg-color: rgba(255, 255, 255, 0.02);
  --el-table-tr-bg-color: transparent;
  --el-table-text-color: #a0aec0;
  --el-table-header-text-color: #fff;
  
  &::before { display: none; }
}

.table-photo-preview {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background-size: cover;
  background-position: center;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.table-photo-none {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px dashed rgba(255, 255, 255, 0.1);
}

.sentiment-dot {
  display: inline-block;
  width: 6px; height: 6px;
  border-radius: 50%;
  margin-right: 8px;
  box-shadow: 0 0 10px currentColor;
}

.sentiment-label { font-size: 12px; font-family: var(--font-title); }

.gauge-info {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
  font-size: 14px;
  .label { color: #94a1b2; }
}

.progress-bar-container {
  height: 4px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 2px;
  margin-bottom: 40px;
  .progress-fill {
    height: 100%;
    background: var(--accent-primary);
    box-shadow: 0 0 10px var(--accent-primary);
    border-radius: 2px;
  }
}

.museum-inspiration {
  border-left: 2px solid var(--accent-primary);
  padding-left: 20px;
  margin-bottom: 40px;
  .quote { font-style: italic; color: #94a1b2; line-height: 1.8; margin-bottom: 10px; }
  .author { font-size: 12px; color: var(--accent-primary); font-family: var(--font-title); }
}

.museum-actions {
  display: flex;
  flex-direction: column;
  gap: 15px;
  
  button {
    padding: 15px;
    border-radius: 12px;
    font-family: var(--font-title);
    letter-spacing: 2px;
    cursor: pointer;
    transition: all 0.3s;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
  }
  
  .action-btn-gold {
    background: var(--grad-mystic);
    border: none;
    color: #fff;
    font-weight: 700;
    padding: 15px;
    border-radius: 12px;
    font-family: var(--font-title);
    cursor: pointer;
    transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 10px;
    box-shadow: 0 10px 25px rgba(157, 80, 187, 0.3);
    &:hover { 
      transform: translateY(-3px) scale(1.02); 
      box-shadow: 0 15px 35px rgba(157, 80, 187, 0.5); 
    }
  }
  
  .action-btn-glass {
    background: transparent;
    border: 1px solid rgba(255, 255, 255, 0.1);
    color: #fff;
    &:hover { background: rgba(255, 255, 255, 0.05); border-color: #fff; }
  }
}

.museum-tag {
  background: rgba(44, 182, 125, 0.2) !important;
  border: 1px solid var(--accent-secondary) !important;
  color: var(--accent-secondary) !important;
}
</style>
