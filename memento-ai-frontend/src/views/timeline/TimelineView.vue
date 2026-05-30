<template>
  <div class="timeline-container">
    <div class="museum-header">
      <div class="header-inner">
        <h1 class="museum-title">时光长河</h1>
        <div class="header-divider"></div>
        <p class="museum-subtitle">追溯过往，见证成长的每一个瞬间</p>
      </div>
      
      <div class="header-actions">
        <el-button-group class="view-toggle">
          <el-button :type="viewType === 'vertical' ? 'primary' : ''" @click="viewType = 'vertical'">
            <el-icon><Sort /></el-icon>纵向
          </el-button>
          <el-button :type="viewType === 'horizontal' ? 'primary' : ''" @click="viewType = 'horizontal'">
            <el-icon><Operation /></el-icon>横向
          </el-button>
        </el-button-group>
      </div>
    </div>

    <div class="timeline-content" :class="viewType">
      <!-- 纵向时间轴 -->
      <div v-if="viewType === 'vertical'" class="vertical-timeline">
        <div v-if="timelineData.length === 0" class="empty-state">
          <el-empty description="暂无时光记录" />
        </div>
        <div 
          v-for="item in sortedTimeline" 
          :key="item.id" 
          class="timeline-item"
          :class="{ 'is-milestone': item.isMilestone }"
        >
          <div class="timeline-line"></div>
          <div 
            class="timeline-marker" 
            :style="{ backgroundColor: item.isMilestone ? '#f59e0b' : item.sentimentColor }"
            @click="handleView(item)"
          >
            <span v-if="item.isMilestone" class="milestone-icon">⭐</span>
          </div>
          <div 
            class="timeline-card" 
            @click="handleView(item)"
            :style="{ 
              backgroundImage: item.photoUrl ? `url(${item.photoUrl})` : 'none',
              backgroundSize: 'cover',
              backgroundPosition: 'center'
            }"
            :class="{ 'has-photo': !!item.photoUrl }"
          >
            <div class="card-overlay" v-if="item.photoUrl"></div>
            <div class="card-content">
              <div class="card-header">
              <span class="date">{{ item.eventDate }}</span>
              <el-tag v-if="item.isMilestone" class="milestone-tag" effect="dark">
                {{ item.milestone?.nodeType || '里程碑' }}
              </el-tag>
            </div>
            <h3 class="title">{{ item.title }}</h3>
            <p class="summary">{{ item.content }}</p>
            <div class="card-footer">
              <div class="tags">
                <span v-for="tag in formatTags(item.tags)" :key="tag" class="tag">#{{ tag }}</span>
              </div>
              <div class="sentiment-bar" :style="{ backgroundColor: item.sentimentColor }"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 横向时间轴 -->
      <div v-else class="horizontal-timeline">
        <div class="timeline-track">
          <div class="timeline-axis"></div>
          <div 
            v-for="(item, index) in sortedTimeline" 
            :key="item.id"
            class="horizontal-item"
            :style="{ left: ((index + 1) / (sortedTimeline.length + 1)) * 100 + '%' }"
          >
            <div 
              class="horizontal-marker"
              :class="{ 'is-milestone': item.isMilestone }"
              :style="{ backgroundColor: item.isMilestone ? '#f59e0b' : item.sentimentColor }"
              @click="handleView(item)"
            >
              <span v-if="item.isMilestone">⭐</span>
            </div>
            <div class="horizontal-tooltip">
              <span class="tooltip-date">{{ item.eventDate }}</span>
              <span class="tooltip-title">{{ item.title }}</span>
            </div>
          </div>
        </div>
        <div class="horizontal-scroll-hint">← 拖动查看更多 →</div>
      </div>
    </div>

    <el-drawer
      v-model="drawerVisible"
      :title="selectedMemory?.title"
      size="45%"
      class="memory-drawer"
    >
      <div v-if="selectedMemory" class="memory-detail">
        <div class="detail-meta">
          <el-tag type="info">{{ selectedMemory.eventDate }}</el-tag>
          <el-tag v-if="selectedMemory.isMilestone" type="warning">
            {{ selectedMemory.milestone?.nodeType || '里程碑' }}
          </el-tag>
          <el-tag :style="{ backgroundColor: selectedMemory.sentimentColor }">
            {{ getSentimentLabel(selectedMemory.sentimentScore) }}
          </el-tag>
        </div>
        <div class="detail-content">
          {{ selectedMemory.content }}
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Sort, Operation } from '@element-plus/icons-vue'
import axios from 'axios'

const viewType = ref('vertical')
const drawerVisible = ref(false)
const selectedMemory = ref(null)
const timelineData = ref([
  { id: 1, title: '开启新征程', content: '入职新公司第一天，感觉充满了挑战。团队氛围很好，期待未来的工作。', eventDate: '2024-05-13', tags: '工作,入职', sentimentScore: 0.9, sentimentColor: '#ef4444', isMilestone: true, milestone: { nodeType: '入职', customTitle: '新的开始' } },
  { id: 2, title: '西湖游玩', content: '和朋友在西湖边漫步，风景很好。雷峰塔、苏堤、断桥，每一处都美不胜收。', eventDate: '2024-05-12', tags: '旅行,休闲', sentimentScore: 0.8, sentimentColor: '#ef4444', isMilestone: false },
  { id: 3, title: '项目上线', content: '忙碌了三个月的项目终于成功上线了！这是我参与的第一个完整项目，收获满满。', eventDate: '2024-05-10', tags: '工作,成就', sentimentScore: 0.95, sentimentColor: '#ef4444', isMilestone: true, milestone: { nodeType: '项目里程碑', customTitle: '项目上线' } },
  { id: 4, title: '感冒了', content: '身体不太舒服，在家休息。喝了很多热水，希望早日康复。', eventDate: '2024-05-08', tags: '健康', sentimentScore: -0.4, sentimentColor: '#3b82f6', isMilestone: false },
  { id: 5, title: '毕业五周年', content: '时间过得真快，转眼已经毕业五年了。和老同学聚会，感慨万千。', eventDate: '2024-05-01', tags: '纪念,感慨', sentimentScore: 0.6, sentimentColor: '#f97316', isMilestone: true, milestone: { nodeType: '纪念日', customTitle: '毕业五周年' } },
  { id: 6, title: '晨练打卡', content: '坚持晨练一个月了，感觉身体状态越来越好。', eventDate: '2024-04-28', tags: '运动,健康', sentimentScore: 0.5, sentimentColor: '#f97316', isMilestone: false },
  { id: 7, title: '读完一本书', content: '终于把那本《百年孤独》读完了，马尔克斯的文笔真是太震撼了。', eventDate: '2024-04-25', tags: '阅读,感悟', sentimentScore: 0.7, sentimentColor: '#f97316', isMilestone: false },
])

const fetchTimeline = async () => {
  try {
    const res = await axios.get('/api/timeline')
    if (res.data && res.data.code === 200 && res.data.data) {
      const newData = res.data.data || []
      if (newData.length > 0) {
        timelineData.value = newData
      }
    }
  } catch (error) {
    console.error('Fetch timeline failed:', error)
  }
}

const sortedTimeline = computed(() => {
  return [...timelineData.value].sort((a, b) => new Date(b.eventDate) - new Date(a.eventDate))
})

const formatTags = (tags) => tags ? tags.split(',') : []

const getSentimentLabel = (score) => {
  if (!score) return '未知'
  if (score > 0.3) return '积极'
  if (score < -0.3) return '消极'
  return '中性'
}

const handleView = (item) => {
  selectedMemory.value = item
  drawerVisible.value = true
}

onMounted(() => {
  fetchTimeline()
})
</script>

<style lang="scss" scoped>
.timeline-container {
  padding: 40px;
  min-height: calc(100vh - 60px);
  background: linear-gradient(135deg, #0f0f23 0%, #1a1a2e 50%, #16213e 100%);
}

.museum-header {
  text-align: center;
  margin-bottom: 60px;
  
  .museum-title {
    font-family: var(--font-title);
    font-size: 38px;
    letter-spacing: 12px;
    margin: 0 0 15px 0;
    background: var(--grad-mystic);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    filter: drop-shadow(0 0 15px rgba(127, 90, 240, 0.4));
  }

  .header-divider {
    width: 220px;
    height: 1px;
    background: linear-gradient(to right, transparent, var(--accent-mystic), transparent);
    margin: 0 auto 12px;
  }

  .museum-subtitle {
    color: #94a1b2;
    font-size: 15px;
    letter-spacing: 3px;
    font-weight: 300;
  }

  .header-actions {
    display: flex;
    justify-content: center;
    gap: 15px;
    margin-top: 30px;
    
    .view-toggle {
      :deep(.el-button) {
        background: rgba(255, 255, 255, 0.05);
        border-color: rgba(255, 255, 255, 0.1);
        color: #94a1b2;
        
        &.el-button--primary {
          background: linear-gradient(135deg, #7f5af0, #9d4edd);
          border-color: #7f5af0;
          color: #fff;
        }
      }
    }
  }
}

.timeline-content {
  .vertical-timeline {
    position: relative;
    max-width: 800px;
    margin: 0 auto;
    padding-left: 40px;
    
    .timeline-item {
      position: relative;
      margin-bottom: 30px;
      
      .timeline-line {
        position: absolute;
        left: -30px;
        top: 20px;
        width: 2px;
        height: calc(100% + 10px);
        background: linear-gradient(180deg, #7f5af0, rgba(127, 90, 240, 0.2));
      }
      
      .timeline-marker {
        position: absolute;
        left: -34px;
        top: 15px;
        width: 10px;
        height: 10px;
        border-radius: 50%;
        cursor: pointer;
        transition: all 0.3s;
        box-shadow: 0 0 10px currentColor;
        
        &:hover {
          transform: scale(1.5);
        }
        
        .milestone-icon {
          position: absolute;
          top: 50%;
          left: 50%;
          transform: translate(-50%, -50%);
          font-size: 10px;
        }
      }
      
      &.is-milestone {
        .timeline-marker {
          width: 20px;
          height: 20px;
          left: -39px;
          top: 10px;
          animation: pulse 2s infinite;
        }
      }
      
      .timeline-card {
        background: rgba(255, 255, 255, 0.03);
        backdrop-filter: blur(10px);
        border-radius: 16px;
        padding: 24px;
        cursor: pointer;
        transition: all 0.3s;
        border: 1px solid rgba(255, 255, 255, 0.05);
        position: relative;
        overflow: hidden;

        &.has-photo {
          border: none;
          .card-content {
            position: relative;
            z-index: 2;
          }
        }

        .card-overlay {
          position: absolute;
          top: 0; left: 0; width: 100%; height: 100%;
          background: linear-gradient(to bottom, rgba(0,0,0,0.1), rgba(0,0,0,0.8));
          z-index: 1;
        }
        
        &:hover {
          transform: translateX(15px);
          border-color: rgba(127, 90, 240, 0.5);
          background: rgba(127, 90, 240, 0.05);
        }
        
        .card-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 12px;
          
          .date { 
            font-size: 13px; 
            color: #7f5af0; 
            font-weight: 600;
          }
          
          .milestone-tag {
            background: linear-gradient(135deg, #f59e0b, #d97706);
            border: none;
            color: #fff;
          }
        }
        
        .title {
          margin: 0 0 12px 0;
          font-size: 19px;
          color: #fff;
          font-weight: 600;
        }
        
        .summary {
          margin: 0 0 16px 0;
          font-size: 15px;
          color: #94a1b2;
          line-height: 1.6;
          display: -webkit-box;
    -webkit-line-clamp: 3;
    line-clamp: 3;
    -webkit-box-orient: vertical;
          overflow: hidden;
        }
        
        .card-footer {
          display: flex;
          justify-content: space-between;
          align-items: center;
          
          .tags {
            display: flex;
            gap: 8px;
            
            .tag {
              color: #94a1b2;
              font-size: 12px;
              opacity: 0.7;
            }
          }
          
          .sentiment-bar {
            width: 40px;
            height: 6px;
            border-radius: 3px;
            box-shadow: 0 0 10px currentColor;
          }
        }
      }
    }
  }
  
  .horizontal-timeline {
    .timeline-track {
      position: relative;
      height: 100px;
      border-bottom: 2px solid rgba(127, 90, 240, 0.3);
      margin-bottom: 20px;
    }
    
    .timeline-axis {
      position: absolute;
      bottom: 0;
      left: 0;
      right: 0;
      height: 2px;
      background: linear-gradient(90deg, rgba(127, 90, 240, 0.2), #7f5af0, rgba(127, 90, 240, 0.2));
    }
    
    .horizontal-item {
      position: absolute;
      bottom: 0;
      transform: translateX(-50%);
      
      .horizontal-marker {
        width: 16px;
        height: 16px;
        border-radius: 50%;
        cursor: pointer;
        transition: all 0.3s;
        box-shadow: 0 0 15px currentColor;
        
        &:hover {
          transform: scale(1.5);
        }
        
        &.is-milestone {
          width: 24px;
          height: 24px;
          animation: pulse 2s infinite;
        }
      }
      
      .horizontal-tooltip {
        position: absolute;
        bottom: 25px;
        left: 50%;
        transform: translateX(-50%);
        background: rgba(15, 15, 35, 0.95);
        backdrop-filter: blur(10px);
        padding: 12px 16px;
        border-radius: 10px;
        white-space: nowrap;
        opacity: 0;
        visibility: hidden;
        transition: all 0.3s;
        border: 1px solid rgba(127, 90, 240, 0.3);
        
        .tooltip-date {
          display: block;
          font-size: 12px;
          color: #7f5af0;
          margin-bottom: 4px;
        }
        
        .tooltip-title {
          font-size: 14px;
          color: #fff;
          font-weight: 500;
        }
      }
      
      &:hover .horizontal-tooltip {
        opacity: 1;
        visibility: visible;
        bottom: 30px;
      }
    }
    
    .horizontal-scroll-hint {
      text-align: center;
      color: #64748b;
      font-size: 13px;
    }
  }
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(245, 158, 11, 0.7); }
  50% { box-shadow: 0 0 0 10px rgba(245, 158, 11, 0); }
}

.memory-detail {
  padding: 0 20px;
  color: #fff;
  
  .detail-meta {
    display: flex;
    gap: 12px;
    margin-bottom: 24px;
    flex-wrap: wrap;
  }
  
  .detail-content {
    font-size: 17px;
    line-height: 1.8;
    color: #fffffe;
    white-space: pre-wrap;
    background: rgba(255, 255, 255, 0.03);
    padding: 20px;
    border-radius: 12px;
  }
}

:deep(.memory-drawer .el-drawer__header) {
  background: rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

:deep(.memory-drawer .el-drawer__body) {
  background: #0f0f23;
}
</style>