<template>
  <div class="gallery-container">
    <div class="museum-header">
      <h1 class="museum-title">时光画廊</h1>
      <p class="museum-subtitle">每一张卡片都是一段珍藏的记忆</p>
      
      <div class="header-actions">
        <el-radio-group v-model="sentimentFilter" @change="handleFilterChange">
          <el-radio-button value="all">全部</el-radio-button>
          <el-radio-button value="positive">积极</el-radio-button>
          <el-radio-button value="neutral">中性</el-radio-button>
          <el-radio-button value="negative">消极</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div class="waterfall-layout" v-loading="loading">
      <div v-if="filteredMemories.length === 0" class="empty-state">
        <el-empty description="暂无相关记忆" />
      </div>
      <div 
        v-for="memory in filteredMemories" 
        :key="memory.id" 
        class="memory-card-wrapper"
        @click="handleView(memory)"
      >
        <div 
          class="memory-card"
          :style="{ 
            borderLeftColor: memory.sentimentColor,
            backgroundImage: memory.photoUrl ? `url(${memory.photoUrl})` : 'none'
          }"
          :class="{ 'has-photo': !!memory.photoUrl }"
        >
          <div class="card-overlay" v-if="memory.photoUrl"></div>
          <div class="card-content">
            <div class="card-header">
              <span class="date">{{ memory.eventDate }}</span>
              <div class="sentiment-indicator" :style="{ backgroundColor: memory.sentimentColor }"></div>
            </div>
            <h3 class="title">{{ memory.title }}</h3>
            <p class="summary">{{ memory.summary }}</p>
            <div class="card-footer">
              <div v-if="memory.topicName" class="topic-badge">{{ memory.topicName }}</div>
              <div class="tags">
                <span v-for="tag in formatTags(memory.tags)" :key="tag" class="tag">
                  #{{ tag }}
                </span>
              </div>
            </div>
          </div>
        </div>
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
import axios from 'axios'

const loading = ref(false)
const sentimentFilter = ref('all')
const drawerVisible = ref(false)
const selectedMemory = ref(null)
const memories = ref([
  { id: 1, title: '西湖边的落日', summary: '今天和朋友一起去了西湖，落日真的很美。那一抹橘红染红了整个湖面，微风拂过...', content: '今天和朋友一起去了西湖，落日真的很美。那一抹橘红染红了整个湖面，微风拂过，泛起阵阵涟漪。远处的雷峰塔在夕阳的映照下显得格外壮丽，湖边的柳树轻轻摇曳，仿佛在诉说着古老的故事。', eventDate: '2024-05-12', tags: '旅行,落日', sentimentScore: 0.8, sentimentColor: '#ef4444', topicName: '旅行足迹' },
  { id: 2, title: '完成项目', summary: '终于把这个艰巨的任务完成了。这个过程虽然辛苦，但看到成果的那一刻...', content: '终于把这个艰巨的任务完成了。这个过程虽然辛苦，但看到成果的那一刻，所有的付出都值得了。团队协作非常顺利，每个人都发挥了自己的专长，最终交付了一个让客户满意的产品。', eventDate: '2024-05-10', tags: '工作,成就感', sentimentScore: 0.5, sentimentColor: '#f97316', topicName: '职场成长' },
  { id: 3, title: '面试失败', summary: '今天面试没过，有点难受。感觉自己准备得还不够充分...', content: '今天面试没过，有点难受。感觉自己准备得还不够充分，下次要更加努力才行。这次经历让我意识到自己还有很多需要学习的地方，我会认真总结经验教训。', eventDate: '2024-05-08', tags: '生活,挫折', sentimentScore: -0.6, sentimentColor: '#3b82f6' },
  { id: 4, title: '晨跑记录', summary: '清晨的空气真清新，跑完5公里感觉整个人都清爽了...', content: '清晨的空气真清新，跑完5公里感觉整个人都清爽了。阳光透过树叶洒在跑道上，形成斑驳的光影，鸟儿在枝头欢快地歌唱，这是一天中最美好的时刻。', eventDate: '2024-05-07', tags: '运动,健康', sentimentScore: 0.4, sentimentColor: '#84cc16', topicName: '生活感悟' },
  { id: 5, title: '深夜食堂', summary: '路边的一碗热汤面，治愈了深夜的疲惫...', content: '路边的一碗热汤面，治愈了深夜的疲惫。老板是一位和蔼的阿姨，她总是记得我的口味，多加一勺辣椒油。在这个寒冷的夜晚，一碗热腾腾的面温暖了我的胃，也温暖了我的心。', eventDate: '2024-05-06', tags: '美食,生活', sentimentScore: 0.3, sentimentColor: '#eab308' },
  { id: 6, title: '错过的公交', summary: '眼睁睁看着公交车开走，今天又是迟到的一天...', content: '眼睁睁看着公交车开走，今天又是迟到的一天。昨晚熬夜工作导致早上起晚了，下次一定要注意时间管理，不能再这样了。迟到不仅影响自己，也影响团队的工作节奏。', eventDate: '2024-05-04', tags: '倒霉', sentimentScore: -0.4, sentimentColor: '#60a5fa' },
])

const fetchMemories = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/gallery?size=50')
    if (res.data && res.data.code === 200 && res.data.data) {
      const newData = res.data.data.records || res.data.data || []
      if (newData.length > 0) {
        memories.value = newData
      }
    }
  } catch (error) {
    console.error('Fetch gallery failed:', error)
  } finally {
    loading.value = false
  }
}

const filteredMemories = computed(() => {
  let list = memories.value.filter(m => !!m.photoUrl) // 强制只显示有照片的记忆
  
  if (sentimentFilter.value === 'all') return list
  return list.filter(item => {
    if (!item.sentimentScore) return true
    if (sentimentFilter.value === 'positive') return item.sentimentScore > 0.3
    if (sentimentFilter.value === 'negative') return item.sentimentScore < -0.3
    return item.sentimentScore >= -0.3 && item.sentimentScore <= 0.3
  })
})

const formatTags = (tags) => tags ? tags.split(',') : []

const getSentimentLabel = (score) => {
  if (!score) return '未知'
  if (score > 0.3) return '积极'
  if (score < -0.3) return '消极'
  return '中性'
}

const handleFilterChange = () => {
  loading.value = true
  setTimeout(() => {
    loading.value = false
  }, 300)
}

const handleView = (memory) => {
  selectedMemory.value = memory
  drawerVisible.value = true
}

onMounted(() => {
  fetchMemories()
})
</script>

<style lang="scss" scoped>
.gallery-container {
  padding: 40px;
  min-height: calc(100vh - 60px);
  background: linear-gradient(135deg, #0f0f23 0%, #1a1a2e 50%, #16213e 100%);
}

.museum-header {
  text-align: center;
  margin-bottom: 60px;
  
  .museum-title {
    font-size: 42px;
    background: linear-gradient(to bottom, #fff, #94a1b2);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    margin: 0;
  }

  .museum-subtitle {
    color: var(--accent-mystic);
    font-size: 14px;
    letter-spacing: 2px;
    margin-top: 15px;
    opacity: 0.8;
    text-transform: uppercase;
  }

  .header-actions {
    display: flex;
    justify-content: center;
    gap: 15px;
    margin-top: 30px;
  }
}

.waterfall-layout {
  column-count: 3;
  column-gap: 28px;
  
  @media (max-width: 1200px) { column-count: 2; }
  @media (max-width: 768px) { column-count: 1; }

  .memory-card-wrapper {
    break-inside: avoid;
    margin-bottom: 28px;
    cursor: pointer;
  }

  .memory-card {
    background: rgba(255, 255, 255, 0.03);
    backdrop-filter: blur(10px);
    border-left: 4px solid;
    border-radius: 16px;
    overflow: hidden;
    transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.2);
    position: relative;
    background-size: cover;
    background-position: center;
    
    &.has-photo {
      border-left: none;
      .card-content {
        position: relative;
        z-index: 2;
      }
    }

    .card-overlay {
      position: absolute;
      top: 0; left: 0; width: 100%; height: 100%;
      background: linear-gradient(to bottom, rgba(0,0,0,0.2), rgba(0,0,0,0.7));
      z-index: 1;
    }

    &:hover {
      transform: translateY(-10px) scale(1.02);
      box-shadow: 0 15px 40px rgba(127, 90, 240, 0.2);
    }

    .card-content {
      padding: 28px;
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      
      .date { 
        font-size: 13px; 
        color: #7f5af0; 
        font-weight: 600;
        letter-spacing: 1px;
      }
      
      .sentiment-indicator {
        width: 12px;
        height: 12px;
        border-radius: 50%;
        box-shadow: 0 0 10px currentColor;
      }
    }

    .title {
      margin: 0 0 14px 0;
      font-size: 20px;
      color: #fff;
      font-weight: 600;
      line-height: 1.4;
    }

    .summary {
      margin: 0 0 20px 0;
      font-size: 15px;
      color: #94a1b2;
      line-height: 1.7;
      display: -webkit-box;
      -webkit-line-clamp: 4;
      line-clamp: 4;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }

    .card-footer {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding-top: 16px;
      border-top: 1px solid rgba(255, 255, 255, 0.05);
      
      .topic-badge {
        background: linear-gradient(135deg, #7f5af0, #9d4edd);
        color: #fff;
        padding: 4px 12px;
        border-radius: 20px;
        font-size: 12px;
        font-weight: 500;
      }
      
      .tags {
        display: flex;
        gap: 8px;
        
        .tag {
          color: #94a1b2;
          font-size: 12px;
          opacity: 0.7;
        }
      }
    }
  }
}

.memory-detail {
  padding: 0 20px;
  color: #fff;
  
  .detail-meta {
    display: flex;
    gap: 12px;
    margin-bottom: 24px;
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

:deep(.el-radio-button__inner) {
  background: rgba(255, 255, 255, 0.05);
  color: #94a1b2;
  border-color: rgba(255, 255, 255, 0.1);
  border-radius: 20px !important;
  padding: 8px 20px;
}

:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background: linear-gradient(135deg, #7f5af0, #9d4edd);
  border-color: #7f5af0;
  color: #fff;
}

:deep(.memory-drawer .el-drawer__header) {
  background: rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

:deep(.memory-drawer .el-drawer__body) {
  background: #0f0f23;
}
</style>