<template>
  <div class="gallery-container">
    <div class="gallery-header">
      <div class="title-section">
        <h2>时光画廊</h2>
        <p>每一张卡片都是一段珍藏的记忆</p>
      </div>
      <div class="filter-actions">
        <el-radio-group v-model="sentimentFilter" @change="handleFilterChange">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="positive">积极</el-radio-button>
          <el-radio-button label="neutral">中性</el-radio-button>
          <el-radio-button label="negative">消极</el-radio-button>
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
        <el-card 
          :body-style="{ padding: '0px' }" 
          shadow="hover" 
          class="memory-card"
          :class="getSentimentClass(memory.sentimentScore)"
        >
          <div class="card-content">
            <div class="card-header">
              <span class="date">{{ memory.eventDate }}</span>
              <el-icon class="sentiment-icon">
                <component :is="getSentimentIcon(memory.sentimentScore)" />
              </el-icon>
            </div>
            <h3 class="title">{{ memory.title }}</h3>
            <p class="summary">{{ memory.content }}</p>
            <div class="card-footer">
              <div class="tags">
                <el-tag v-for="tag in formatTags(memory.tags)" :key="tag" size="small" effect="plain">
                  {{ tag }}
                </el-tag>
              </div>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <el-drawer
      v-model="drawerVisible"
      :title="selectedMemory?.title"
      size="45%"
    >
      <div v-if="selectedMemory" class="memory-detail">
        <div class="detail-meta">
          <el-tag type="info">{{ selectedMemory.eventDate }}</el-tag>
          <el-tag :type="getSentimentType(selectedMemory.sentimentScore)">
            情感: {{ getSentimentLabel(selectedMemory.sentimentScore) }}
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
import { ref, computed } from 'vue'

const loading = ref(false)
const sentimentFilter = ref('all')
const drawerVisible = ref(false)
const selectedMemory = ref(null)

const memories = ref([
  { id: 1, title: '西湖边的落日', content: '今天和朋友一起去了西湖，落日真的很美。那一抹橘红染红了整个湖面，微风拂过，泛起阵阵涟漪。', eventDate: '2024-05-12', tags: '旅行,落日', sentimentScore: 0.8 },
  { id: 2, title: '完成项目', content: '终于把这个艰巨的任务完成了。这个过程虽然辛苦，但看到成果的那一刻，所有的付出都值得了。', eventDate: '2024-05-10', tags: '工作,成就感', sentimentScore: 0.5 },
  { id: 3, title: '面试失败', content: '今天面试没过，有点难受。感觉自己准备得还不够充分，下次要更加努力才行。', eventDate: '2024-05-08', tags: '生活,挫折', sentimentScore: -0.6 },
  { id: 4, title: '晨跑记录', content: '清晨的空气真清新，跑完5公里感觉整个人都清爽了。', eventDate: '2024-05-07', tags: '运动,健康', sentimentScore: 0.4 },
  { id: 5, title: '深夜食堂', content: '路边的一碗热汤面，治愈了深夜的疲惫。', eventDate: '2024-05-06', tags: '美食,生活', sentimentScore: 0.3 },
  { id: 6, title: '错过的公交', content: '眼睁睁看着公交车开走，今天又是迟到的一天。', eventDate: '2024-05-04', tags: '倒霉', sentimentScore: -0.4 },
])

const filteredMemories = computed(() => {
  if (sentimentFilter.value === 'all') return memories.value
  return memories.value.filter(item => {
    if (sentimentFilter.value === 'positive') return item.sentimentScore > 0.3
    if (sentimentFilter.value === 'negative') return item.sentimentScore < -0.3
    return item.sentimentScore >= -0.3 && item.sentimentScore <= 0.3
  })
})

const formatTags = (tags) => tags ? tags.split(',') : []

const getSentimentClass = (score) => {
  if (score > 0.3) return 'positive-card'
  if (score < -0.3) return 'negative-card'
  return 'neutral-card'
}

const getSentimentIcon = (score) => {
  if (score > 0.3) return 'Sunny'
  if (score < -0.3) return 'Cloudy'
  return 'PartlyCloudy'
}

const getSentimentType = (score) => {
  if (score > 0.3) return 'success'
  if (score < -0.3) return 'danger'
  return 'info'
}

const getSentimentLabel = (score) => {
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
</script>

<style lang="scss" scoped>
.gallery-container {
  padding: 24px;
  min-height: calc(100vh - 60px);

  .gallery-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-bottom: 32px;
    
    .title-section {
      h2 { margin: 0 0 8px 0; color: #fff; font-size: 26px; text-shadow: 0 0 10px rgba(127, 90, 240, 0.5); }
      p { margin: 0; color: #94a1b2; }
    }
  }

  .waterfall-layout {
    column-count: 3;
    column-gap: 24px;
    
    @media (max-width: 1200px) { column-count: 2; }
    @media (max-width: 768px) { column-count: 1; }

    .memory-card-wrapper {
      break-inside: avoid;
      margin-bottom: 24px;
      cursor: pointer;
    }

    .memory-card {
      border: 1px solid rgba(255, 255, 255, 0.1);
      transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
      
      &:hover {
        transform: translateY(-8px) scale(1.02);
        box-shadow: 0 10px 30px rgba(127, 90, 240, 0.3);
        border-color: rgba(127, 90, 240, 0.5);
      }

      .card-content {
        padding: 24px;
      }

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 16px;
        .date { font-size: 13px; color: #94a1b2; }
        .sentiment-icon { font-size: 20px; }
      }

      .title {
        margin: 0 0 12px 0;
        font-size: 19px;
        color: #fff;
        line-height: 1.4;
      }

      .summary {
        margin: 0 0 20px 0;
        font-size: 15px;
        color: #94a1b2;
        line-height: 1.6;
        display: -webkit-box;
        -webkit-line-clamp: 4;
        -webkit-box-orient: vertical;
        overflow: hidden;
      }

      &.positive-card { border-top: 4px solid #2cb67d; .sentiment-icon { color: #2cb67d; } }
      &.negative-card { border-top: 4px solid #ef4565; .sentiment-icon { color: #ef4565; } }
      &.neutral-card { border-top: 4px solid #72757e; .sentiment-icon { color: #72757e; } }
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
    }
  }
}

:deep(.el-radio-button__inner) {
  background: rgba(255, 255, 255, 0.05);
  color: #94a1b2;
  border-color: rgba(255, 255, 255, 0.1);
}
:deep(.el-radio-button__original-radio:checked + .el-radio-button__inner) {
  background-color: #7f5af0;
  border-color: #7f5af0;
}
</style>
