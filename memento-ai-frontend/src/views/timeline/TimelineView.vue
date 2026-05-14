<template>
  <div class="timeline-container">
    <div class="timeline-header">
      <div class="title-section">
        <h2>时光轴</h2>
        <p>追溯过往，见证成长</p>
      </div>
      <div class="timeline-actions">
        <el-button-group>
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
      <el-timeline v-if="viewType === 'vertical'">
        <el-timeline-item
          v-for="item in sortedTimeline"
          :key="item.id"
          :timestamp="item.eventDate"
          :type="getTimelineItemType(item)"
          :hollow="!item.isMilestone"
          :size="item.isMilestone ? 'large' : 'normal'"
        >
          <el-card class="timeline-card" shadow="hover" @click="handleView(item)">
            <div class="card-header">
              <span class="title">{{ item.title }}</span>
              <el-tag v-if="item.isMilestone" size="small" type="warning" effect="dark">
                {{ item.milestoneType || '里程碑' }}
              </el-tag>
            </div>
            <p class="summary">{{ item.content }}</p>
            <div class="card-footer">
              <el-tag 
                v-for="tag in formatTags(item.tags)" 
                :key="tag" 
                size="small" 
                effect="plain"
                class="tag"
              >
                {{ tag }}
              </el-tag>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>

      <div v-else class="horizontal-timeline">
        <el-empty description="横向视图开发中..." />
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
          <el-tag v-if="selectedMemory.isMilestone" type="warning">
            {{ selectedMemory.milestoneType }}
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

const viewType = ref('vertical')
const drawerVisible = ref(false)
const selectedMemory = ref(null)

const timelineData = ref([
  { id: 1, title: '开启新征程', content: '入职新公司第一天，感觉充满了挑战。', eventDate: '2024-05-13', tags: '工作,入职', sentimentScore: 0.9, isMilestone: true, milestoneType: '入职' },
  { id: 2, title: '西湖游玩', content: '和朋友在西湖边漫步，风景很好。', eventDate: '2024-05-12', tags: '旅行,休闲', sentimentScore: 0.8, isMilestone: false },
  { id: 3, title: '项目上线', content: '忙碌了三个月的项目终于成功上线了！', eventDate: '2024-05-10', tags: '工作,成就', sentimentScore: 0.95, isMilestone: true, milestoneType: '项目里程碑' },
  { id: 4, title: '感冒了', content: '身体不太舒服，在家休息。', eventDate: '2024-05-08', tags: '健康', sentimentScore: -0.4, isMilestone: false },
  { id: 5, title: '毕业五周年', content: '时间过得真快，转眼已经毕业五年了。', eventDate: '2024-05-01', tags: '纪念,感慨', sentimentScore: 0.6, isMilestone: true, milestoneType: '纪念日' },
])

const sortedTimeline = computed(() => {
  return [...timelineData.value].sort((a, b) => new Date(b.eventDate) - new Date(a.eventDate))
})

const getTimelineItemType = (item) => {
  if (item.isMilestone) return 'warning'
  if (item.sentimentScore > 0.3) return 'success'
  if (item.sentimentScore < -0.3) return 'danger'
  return 'primary'
}

const formatTags = (tags) => tags ? tags.split(',') : []

const handleView = (item) => {
  selectedMemory.value = item
  drawerVisible.value = true
}
</script>

<style lang="scss" scoped>
.timeline-container {
  padding: 24px;
  min-height: calc(100vh - 60px);

  .timeline-header {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-bottom: 32px;
    
    .title-section {
      h2 { margin: 0 0 8px 0; color: #fff; font-size: 26px; text-shadow: 0 0 10px rgba(127, 90, 240, 0.5); }
      p { margin: 0; color: #94a1b2; }
    }
  }

  .timeline-content {
    max-width: 800px;
    margin: 0 auto;

    :deep(.el-timeline-item__timestamp) {
      color: #7f5af0;
      font-weight: bold;
    }

    .timeline-card {
      cursor: pointer;
      border: 1px solid rgba(255, 255, 255, 0.1);
      transition: all 0.3s;
      
      &:hover {
        transform: translateX(10px);
        border-color: #7f5af0;
        background: rgba(127, 90, 240, 0.05) !important;
      }

      .card-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 12px;
        .title { font-weight: bold; color: #fff; font-size: 17px; }
      }

      .summary {
        font-size: 15px;
        color: #94a1b2;
        line-height: 1.6;
        margin-bottom: 16px;
      }

      .card-footer {
        display: flex;
        gap: 8px;
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
    }
  }
}
</style>
