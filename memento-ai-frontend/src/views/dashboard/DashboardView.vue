<template>
  <div class="museum-dashboard museum-fade-in">
    <!-- 顶部欢迎区：私人博物馆铭牌 -->
    <div class="museum-plaque">
      <div class="plaque-content">
        <h1 class="museum-name">The Memento Archive</h1>
        <div class="plaque-divider"></div>
        <p class="curator-info">馆长: {{ userStore.userInfo.nickname }}</p>
        <p class="curator-date">记录始于 {{ today }}</p>
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
            <el-table :data="recentMemories" class="museum-table">
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
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import dayjs from 'dayjs'

const router = useRouter()
const userStore = useUserStore()
const today = dayjs().format('YYYY / MM / DD')

const stats = ref([
  { label: '记忆结晶', value: '128', icon: 'Collection', trend: 12 },
  { label: '重要锚点', value: '15', icon: 'Flag', trend: 5 },
  { label: '编织叙事', value: '8', icon: 'Notebook', trend: 22 },
  { label: '情感共鸣', value: '85%', icon: 'Connection', trend: -2 }
])

const recentMemories = ref([
  { eventDate: '2024.05.12', title: '西湖边的落日，在那一刻时间仿佛凝固', sentimentScore: 0.8 },
  { eventDate: '2024.05.10', title: '深更半夜的代码调试，虽然疲惫但终于通透', sentimentScore: 0.5 },
  { eventDate: '2024.05.08', title: '一场大雨淋湿了心情，却也洗净了街道', sentimentScore: -0.4 },
  { eventDate: '2024.05.05', title: '老街里的咖啡香，和不期而喻的重逢', sentimentScore: 0.9 },
  { eventDate: '2024.05.01', title: '山顶的晨曦，是给早起者最慷慨的奖励', sentimentScore: 0.7 }
])

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
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 4px;
  padding: 40px;
  text-align: center;
  position: relative;
  margin-bottom: 60px;
  
  &::before, &::after {
    content: '◈';
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    color: var(--accent-primary);
    font-size: 20px;
  }
  &::before { left: 40px; }
  &::after { right: 40px; }

  .museum-name {
    font-family: var(--font-title);
    font-size: 48px;
    letter-spacing: 12px;
    margin: 0 0 20px 0;
    background: linear-gradient(to bottom, #fff, #94a1b2);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
  
  .plaque-divider {
    width: 200px;
    height: 1px;
    background: linear-gradient(to right, transparent, var(--accent-primary), transparent);
    margin: 0 auto 20px;
  }

  .curator-info, .curator-date {
    margin: 5px 0;
    font-family: var(--font-title);
    letter-spacing: 2px;
    font-size: 14px;
    color: #94a1b2;
  }
}

.exhibit-case {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.05);
  padding: 30px;
  border-radius: 20px;
  position: relative;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  
  &:hover {
    transform: translateY(-10px);
    background: rgba(255, 255, 255, 0.05);
    border-color: var(--accent-primary);
    .case-glow { opacity: 1; }
  }

  .case-header {
    display: flex;
    align-items: center;
    gap: 10px;
    color: var(--accent-primary);
    margin-bottom: 15px;
    .label { font-size: 12px; letter-spacing: 2px; text-transform: uppercase; color: #94a1b2; }
  }

  .case-value {
    font-size: 42px;
    font-weight: 700;
    font-family: var(--font-title);
    margin-bottom: 10px;
  }

  .case-footer {
    .trend {
      font-size: 12px;
      &.up { color: var(--accent-secondary); }
      &.down { color: var(--accent-tertiary); }
    }
  }

  .case-glow {
    position: absolute;
    bottom: -50px; right: -50px;
    width: 150px; height: 150px;
    background: radial-gradient(circle, rgba(127, 90, 240, 0.1) 0%, transparent 70%);
    opacity: 0;
    transition: opacity 0.4s;
  }
}

.curated-section {
  margin-top: 60px;
}

.museum-curio-cabinet, .museum-status-monitor {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 24px;
  padding: 30px;
  height: 100%;
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
    background: var(--accent-primary);
    border: none;
    color: #fff;
    &:hover { transform: scale(1.02); box-shadow: 0 5px 15px rgba(127, 90, 240, 0.4); }
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
