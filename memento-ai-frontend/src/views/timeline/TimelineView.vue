<template>
  <div class="timeline-container">
    <div class="museum-header">
      <h1 class="museum-title">时光长河</h1>
      <p class="museum-subtitle">追溯过往，见证成长的每一个瞬间</p>

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
          >
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

    <!-- ==================== 改动开始：抽屉内容重构为【上半文字 + 下半图片】 ==================== -->
    <el-drawer
        v-model="drawerVisible"
        :title="selectedMemory?.title"
        size="45%"
        class="memory-drawer"
    >
      <div v-if="selectedMemory" class="memory-detail-shell">
        <!-- 上半部分：纯文字卡片区（无图片背景） -->
        <div class="memory-detail-text">
          <div class="memory-detail-text-card">
            <h2 class="memory-detail-title">{{ selectedMemory.title }}</h2>
            <div class="memory-detail-meta-row">
              <span class="meta-chip meta-date">{{ selectedMemory.eventDate }}</span>
              <span v-if="selectedMemory.isMilestone" class="meta-chip meta-milestone">
                {{ selectedMemory.milestone?.nodeType || '里程碑' }}
              </span>
              <span class="meta-chip meta-sentiment" :class="`sentiment-${getSentimentLabel(selectedMemory.sentimentScore)}`">
                {{ getSentimentLabel(selectedMemory.sentimentScore) }}
              </span>
            </div>
            <div class="memory-detail-content">
              {{ selectedMemory.content }}
            </div>
          </div>
        </div>

        <!-- 下半部分：图片背景区 -->
        <div
            class="memory-detail-image"
            :style="getDetailImageStyle(selectedMemory.photoUrl)"
        >
          <div v-if="!selectedMemory.photoUrl" class="image-placeholder">
            <span>📷 暂无图片</span>
          </div>
        </div>
      </div>
    </el-drawer>
    <!-- ==================== 改动结束 ==================== -->
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

const normalizePhotoUrl = (photoUrl) => {
  if (!photoUrl) return ''
  const normalized = String(photoUrl).replace(/\\/g, '/').trim()
  if (/^(https?:)?\/\//.test(normalized) || normalized.startsWith('data:')) return normalized
  if (normalized.startsWith('/uploads/')) return normalized
  if (normalized.startsWith('uploads/')) return `/${normalized}`
  return normalized.startsWith('/') ? normalized : `/${normalized}`
}

// ==================== 改动开始：新增下半部分图片背景样式函数（删除原 getDetailHeroStyle） ====================
const getDetailImageStyle = (photoUrl) => {
  const normalizedUrl = normalizePhotoUrl(photoUrl)
  if (normalizedUrl) {
    return {
      backgroundImage: `url("${normalizedUrl}")`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat'
    }
  }
  return {} // 无图片时使用占位样式
}
// ==================== 改动结束 ====================

const enrichMemoryWithPhoto = async (memory) => {
  if (!memory?.id || memory.photoUrl) return memory

  try {
    const res = await axios.get(`/api/memories/${memory.id}`)
    if (res.data?.code === 200 && res.data.data?.photoUrl) {
      return { ...memory, photoUrl: res.data.data.photoUrl }
    }
  } catch (error) {
    console.error('Fetch memory detail failed:', error)
  }

  return memory
}

const handleView = async (item) => {
  selectedMemory.value = item
  drawerVisible.value = true

  const memoryWithPhoto = await enrichMemoryWithPhoto(item)
  selectedMemory.value = memoryWithPhoto
  timelineData.value = timelineData.value.map(memory =>
      memory.id === memoryWithPhoto.id ? { ...memory, photoUrl: memoryWithPhoto.photoUrl } : memory
  )
}

onMounted(() => {
  fetchTimeline()
})
</script>

<style lang="scss" scoped>
.timeline-container {
  padding: 40px;
  min-height: calc(100vh - 60px);
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
        transition: transform 0.28s ease, background 0.28s ease, border-color 0.28s ease, box-shadow 0.28s ease;
        border: 1px solid rgba(255, 255, 255, 0.05);
        position: relative;
        overflow: hidden;

        &:hover {
          transform: translateX(12px) translateY(-2px);
          border-color: rgba(127, 90, 240, 0.38);
          background: rgba(127, 90, 240, 0.08);
          box-shadow: 0 14px 30px rgba(0, 0, 0, 0.18);
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

/* ==================== 改动开始：全新的抽屉样式（上半文字，下半图片） ==================== */
.memory-detail-shell {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: linear-gradient(180deg, rgba(10, 12, 28, 0.96) 0%, rgba(15, 23, 42, 0.98) 100%);
  border-radius: 28px;
  overflow: hidden;
}

/* 上半部分：文字卡片区域 */
.memory-detail-text {
  padding: 28px 28px 20px;
  flex-shrink: 0;    /* 新增：防止文字区域被压缩 */
  //flex: 1;

  .memory-detail-text-card {
    background: rgba(255, 255, 255, 0.04);
    backdrop-filter: blur(12px);
    border-radius: 24px;
    padding: 24px 28px 32px;
    border: 1px solid rgba(148, 163, 184, 0.2);
    box-shadow: 0 12px 28px rgba(0, 0, 0, 0.2);
  }

  .memory-detail-title {
    margin: 0 0 16px 0;
    font-size: 28px;
    font-weight: 700;
    color: #fff;
    line-height: 1.3;
    letter-spacing: -0.01em;
  }

  .memory-detail-meta-row {
    display: flex;
    flex-wrap: wrap;
    gap: 12px;
    margin-bottom: 28px;
  }

  .meta-chip {
    display: inline-flex;
    align-items: center;
    padding: 6px 14px;
    border-radius: 40px;
    font-size: 12px;
    font-weight: 600;
    background: rgba(255, 255, 255, 0.1);
    backdrop-filter: blur(8px);
    border: 1px solid rgba(255, 255, 255, 0.15);
    color: #e2e8f0;
  }

  .sentiment-积极 {
    background: rgba(34, 197, 94, 0.2);
    border-color: rgba(34, 197, 94, 0.4);
    color: #bbf7d0;
  }
  .sentiment-消极 {
    background: rgba(59, 130, 246, 0.2);
    border-color: rgba(59, 130, 246, 0.4);
    color: #bfdbfe;
  }
  .sentiment-中性 {
    background: rgba(148, 163, 184, 0.2);
    border-color: rgba(148, 163, 184, 0.4);
    color: #e2e8f0;
  }
  .meta-milestone {
    background: rgba(245, 158, 11, 0.2);
    border-color: rgba(245, 158, 11, 0.4);
    color: #fde68a;
  }

  .memory-detail-content {
    font-size: 16px;
    line-height: 1.75;
    color: #cbd5e6;
    white-space: pre-wrap;
    word-break: break-word;
    margin-top: 8px;
  }
}

/* 下半部分：图片背景区域 */
.memory-detail-image {
  flex: 1;           /* 新增：占据剩余所有高度 */
  //flex-shrink: 0;
  //height: 260px;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  border-top: 1px solid rgba(148, 163, 184, 0.2);
  position: relative;

  .image-placeholder {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
    background: linear-gradient(135deg, rgba(30, 41, 59, 0.6), rgba(15, 23, 42, 0.8));
    color: #94a3b8;
    font-size: 14px;
    letter-spacing: 1px;
  }
}
/* ==================== 改动结束 ==================== */

/* 覆盖 Element Plus 抽屉默认样式（保持不变） */
:deep(.memory-drawer) {
  background: #0b1020;
}

:deep(.memory-drawer .el-drawer__header) {
  margin-bottom: 0;
  padding: 18px 24px 0;
  background: transparent;
  border-bottom: none;
}

:deep(.memory-drawer .el-drawer__title) {
  color: #f8fafc;
  font-weight: 700;
  letter-spacing: 0.02em;
}

:deep(.memory-drawer .el-drawer__body) {
  padding: 22px 24px 26px;
  background: linear-gradient(180deg, #0b1020 0%, #0f172a 100%);
}

:deep(.memory-drawer .el-drawer__close-btn) {
  color: #cbd5e1;
}
</style>