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

    <div class="statistics-container">
      <div class="stat-item total">
        <div class="stat-icon">📚</div>
        <div class="stat-content">
          <div class="stat-number">{{ statistics.total }}</div>
          <div class="stat-label">总记忆数</div>
        </div>
      </div>
      <div class="stat-item positive">
        <div class="stat-icon">😊</div>
        <div class="stat-content">
          <div class="stat-number">{{ statistics.positive }}</div>
          <div class="stat-label">积极</div>
        </div>
      </div>
      <div class="stat-item neutral">
        <div class="stat-icon">😐</div>
        <div class="stat-content">
          <div class="stat-number">{{ statistics.neutral }}</div>
          <div class="stat-label">中性</div>
        </div>
      </div>
      <div class="stat-item negative">
        <div class="stat-icon">😔</div>
        <div class="stat-content">
          <div class="stat-number">{{ statistics.negative }}</div>
          <div class="stat-label">消极</div>
        </div>
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
            <div class="card-footer">
              <div v-if="memory.topicName" class="topic-badge">{{ memory.topicName }}</div>
              <div class="tags">
                <span v-for="tag in formatTags(memory.tags)" :key="tag" class="tag">
                  #{{ tag }}
                </span>
              </div>
            </div>
          </div>
          <div class="card-hover-content">
            <h3 class="hover-title">{{ memory.title }}</h3>
            <div class="hover-detail">
              {{ memory.content }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import axios from 'axios'

const loading = ref(false)
const sentimentFilter = ref('all')
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
    console.log('Gallery API response:', res.data)
    if (res.data && res.data.code === 200 && res.data.data) {
      const newData = res.data.data.records || res.data.data || []
      console.log('Fetched memories:', newData)
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

const statistics = computed(() => {
  const list = memories.value
  return {
    total: list.length,
    positive: list.filter(item => item.sentimentScore > 0.3).length,
    neutral: list.filter(item => item.sentimentScore >= -0.3 && item.sentimentScore <= 0.3).length,
    negative: list.filter(item => item.sentimentScore < -0.3).length
  }
})

const filteredMemories = computed(() => {
  let list = memories.value
  
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

onMounted(() => {
  fetchMemories()
})
</script>

<style lang="scss" scoped>
.gallery-container {
  padding: 40px;
  min-height: calc(100vh - 60px);
}

.museum-header {
  text-align: center;
  margin-bottom: 30px;
  
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

.statistics-container {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-bottom: 40px;
  flex-wrap: wrap;

  .stat-item {
    display: flex;
    align-items: center;
    gap: 10px;
    background: rgba(255, 255, 255, 0.05);
    backdrop-filter: blur(10px);
    border-radius: 12px;
    padding: 12px 24px;
    border: 1px solid rgba(255, 255, 255, 0.1);
    transition: all 0.3s ease;
    min-width: 130px;
    justify-content: center;

    &:hover {
      transform: translateY(-5px);
      box-shadow: 0 10px 30px rgba(127, 90, 240, 0.2);
    }

    &.total {
      border-color: rgba(127, 90, 240, 0.3);
      background: linear-gradient(135deg, rgba(127, 90, 240, 0.1), rgba(157, 78, 221, 0.05));
    }

    &.positive {
      border-color: rgba(239, 68, 68, 0.3);
      background: linear-gradient(135deg, rgba(239, 68, 68, 0.1), rgba(249, 115, 22, 0.05));
    }

    &.neutral {
      border-color: rgba(234, 179, 8, 0.3);
      background: linear-gradient(135deg, rgba(234, 179, 8, 0.1), rgba(132, 204, 22, 0.05));
    }

    &.negative {
      border-color: rgba(59, 130, 246, 0.3);
      background: linear-gradient(135deg, rgba(59, 130, 246, 0.1), rgba(96, 165, 250, 0.05));
    }

    .stat-icon {
      font-size: 24px;
    }

    .stat-content {
      text-align: left;

      .stat-number {
        font-size: 22px;
        font-weight: 700;
        color: #fff;
        line-height: 1.1;
      }

      .stat-label {
        font-size: 12px;
        color: #94a1b2;
        margin-top: 2px;
        text-transform: uppercase;
        letter-spacing: 0.5px;
      }
    }
  }
}

.waterfall-layout {
  column-count: 3;
  column-gap: 28px;
  
  @media (max-width: 1200px) { column-count: 2; }
  @media (max-width: 768px) { column-count: 1; }

  .memory-card-wrapper {
    break-inside: avoid;
    margin-bottom: 40px;
    padding-top: 18px;
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
      background-image: none !important;
      background: linear-gradient(135deg, #1a1a2e, #16213e);
      
      .card-overlay {
        display: none;
      }
      
      .card-content {
        opacity: 0;
        visibility: hidden;
        transform: translateY(-10px);
      }
      
      .card-hover-content {
        opacity: 1;
        visibility: visible;
        transform: translateY(0);
      }
      
      .sentiment-indicator {
        display: none;
      }
    }

    .card-content {
      padding: 40px;
      transition: all 0.3s ease;
    }
    
    .card-hover-content {
      position: absolute;
      top: 12px;
      left: 12px;
      right: 12px;
      bottom: 12px;
      padding: 28px 26px;
      opacity: 0;
      visibility: hidden;
      transform: translateY(10px);
      transition: all 0.3s ease;
      z-index: 5;
      display: flex;
      flex-direction: column;
      //overflow-y: auto;
      box-sizing: border-box;
      
      .hover-title {
        margin: 0 0 24px 0;
        font-size: 22px;
        color: #fff;
        font-weight: 600;
        line-height: 1.4;
        word-wrap: break-word;
        word-break: break-word;
      }
      
      .hover-detail {
        flex: 1;
        font-size: 16px;
        line-height: 1.8;
        color: #94a1b2;
        white-space: normal;
        word-wrap: break-word;
        word-break: break-word;
        overflow-wrap: break-word;
        //overflow-y: auto;
        width: 100%;
        max-width: 100%;
      }
    }

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
      
      .date { 
        font-size: 14px; 
        color: #7f5af0; 
        font-weight: 600;
        letter-spacing: 1px;
      }
      
      .sentiment-indicator {
        width: 14px;
        height: 14px;
        border-radius: 50%;
        box-shadow: 0 0 10px currentColor;
      }
    }

    .title {
      margin: 0 0 24px 0;
      font-size: 22px;
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
      padding-top: 24px;
      border-top: 1px solid rgba(255, 255, 255, 0.05);
      
      .topic-badge {
        background: linear-gradient(135deg, #7f5af0, #9d4edd);
        color: #fff;
        padding: 6px 16px;
        border-radius: 20px;
        font-size: 13px;
        font-weight: 500;
      }
      
      .tags {
        display: flex;
        gap: 10px;
        
        .tag {
          color: #94a1b2;
          font-size: 13px;
          opacity: 0.7;
        }
      }
    }
  }
}

:deep(.el-radio-button) {
  margin: 0 8px;
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
</style>
