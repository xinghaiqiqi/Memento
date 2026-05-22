<template>
  <div class="memories-showroom museum-fade-in">
    <!-- 展厅头部：艺术化标题 -->
    <div class="showroom-header">
      <div class="header-content">
        <h1 class="showroom-title">Memory Showroom</h1>
        <p class="showroom-subtitle">在此静候，检阅您生命中每一个闪光的瞬间</p>
      </div>
      <div class="header-actions">
        <button class="action-btn-gold" @click="handleAdd">
          <el-icon><Plus /></el-icon> 增添馆藏
        </button>
        <button class="action-btn-glass" @click="router.push('/import')">
          <el-icon><MagicStick /></el-icon> 萃取新记忆
        </button>
      </div>
    </div>

    <!-- 检索区域：玻璃拟态滤镜 -->
    <div class="search-vault">
      <div class="search-grid">
        <div class="search-item">
          <label class="item-label">◈ 寻踪</label>
          <el-input
            v-model="searchQuery"
            placeholder="搜寻记忆残片..."
            class="museum-input"
            clearable
            @input="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </div>
        
        <div class="search-item">
          <label class="item-label">◈ 纪元</label>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="◈"
            start-placeholder="始"
            end-placeholder="终"
            value-format="YYYY-MM-DD"
            class="museum-date-picker"
            @change="handleDateChange"
          />
        </div>

        <div class="search-item">
          <label class="item-label">◈ 情感</label>
          <el-select v-model="sentimentFilter" placeholder="情感底色" clearable class="museum-select" @change="handleSentimentChange">
            <el-option label="全域" value="" />
            <el-option label="悦 (积极)" value="positive" />
            <el-option label="平 (中性)" value="neutral" />
            <el-option label="忧 (消极)" value="negative" />
          </el-select>
        </div>
      </div>
    </div>

    <!-- 主陈列区：艺术化列表 -->
    <div class="exhibit-area" v-loading="loading">
      <div class="exhibit-table-wrapper">
        <el-table :data="pagedData" class="museum-table" row-class-name="museum-row">
          <el-table-column prop="eventDate" label="时间印记" width="140">
            <template #default="{ row }">
              <span class="time-stamp">{{ row.eventDate.replace(/-/g, ' . ') }}</span>
            </template>
          </el-table-column>
          
          <el-table-column prop="title" label="记忆之名" min-width="200">
            <template #default="{ row }">
              <div class="memory-name-cell" @click="handleView(row)">
                <span class="title-text">{{ row.title }}</span>
                <div class="tag-cloud">
                  <span v-for="tag in formatTags(row.tags)" :key="tag" class="mini-tag">#{{ tag }}</span>
                </div>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="情感色彩" width="120">
            <template #default="{ row }">
              <div class="sentiment-badge" :style="{ '--color': getSentimentColor(row.sentimentScore) }">
                <div class="badge-dot"></div>
                <span class="badge-label">{{ getSentimentLabel(row.sentimentScore) }}</span>
              </div>
            </template>
          </el-table-column>

          <el-table-column label="操作" width="160" fixed="right" align="right">
            <template #default="{ row }">
              <div class="row-actions">
                <el-tooltip content="编辑" placement="top">
                  <button class="icon-btn" @click="handleEdit(row)"><el-icon><Edit /></el-icon></button>
                </el-tooltip>
                <el-tooltip content="详情" placement="top">
                  <button class="icon-btn" @click="handleView(row)"><el-icon><View /></el-icon></button>
                </el-tooltip>
                <el-popconfirm title="确定要将这段记忆遗忘吗？" @confirm="handleDelete(row)">
                  <template #reference>
                    <button class="icon-btn delete"><el-icon><Delete /></el-icon></button>
                  </template>
                </el-popconfirm>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <!-- 翻页控制 -->
      <div class="showroom-pagination">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, prev, pager, next"
          :total="total"
          @current-change="handlePageChange"
          @size-change="handleSizeChange"
        />
      </div>
    </div>

    <!-- 弹窗部分保持逻辑，美化样式 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '◈ 增添馆藏' : '◈ 修补记忆'"
      width="600px"
      custom-class="museum-dialog"
    >
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef" class="museum-form">
        <el-form-item label="记忆之名" prop="title">
          <el-input v-model="form.title" placeholder="输入标题..." />
        </el-form-item>
        <el-form-item label="时间印记" prop="eventDate">
          <el-date-picker v-model="form.eventDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="标签云">
          <el-input v-model="form.tags" placeholder="逗号分隔..." />
        </el-form-item>
        <el-form-item label="叙事内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="8" placeholder="在此刻录..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <button class="action-btn-glass" @click="dialogVisible = false">取消</button>
          <button class="action-btn-gold" @click="submitForm" :loading="submitting">保存至永恒</button>
        </div>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      size="50%"
      custom-class="museum-drawer"
    >
      <template #header>
        <div class="drawer-header">
          <h2 class="drawer-title">{{ selectedMemory?.title }}</h2>
          <span class="drawer-date">{{ selectedMemory?.eventDate }}</span>
        </div>
      </template>
      <div v-if="selectedMemory" class="memory-scroll-detail">
        <div class="detail-tags">
          <span v-for="tag in formatTags(selectedMemory.tags)" :key="tag" class="glow-tag"># {{ tag }}</span>
        </div>
        <div class="detail-sentiment">
          <span class="label">情感基调：</span>
          <span class="value" :style="{ color: getSentimentColor(selectedMemory.sentimentScore) }">
            {{ getSentimentLabel(selectedMemory.sentimentScore) }} ({{ selectedMemory.sentimentScore }})
          </span>
        </div>
        <div class="scroll-content">
          {{ selectedMemory.content }}
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()

const memories = ref([])
const loading = ref(false)

const fetchMemories = async () => {
  loading.value = true
  try {
    const res = await axios.get('/api/memories', {
      params: {
        current: currentPage.value,
        size: pageSize.value,
        keyword: searchQuery.value,
        sentimentType: sentimentFilter.value,
        startDate: dateRange.value?.[0],
        endDate: dateRange.value?.[1]
      }
    })
    memories.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    ElMessage.error('无法连接至陈列馆')
  } finally {
    loading.value = false
  }
}

const total = ref(0)
const searchQuery = ref('')
const dateRange = ref([])
const sentimentFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

onMounted(() => {
  fetchMemories()
})

const pagedData = computed(() => memories.value)

const handleSearch = () => {
  currentPage.value = 1
  fetchMemories()
}
const handleDateChange = () => {
  currentPage.value = 1
  fetchMemories()
}
const handleSentimentChange = () => {
  currentPage.value = 1
  fetchMemories()
}

const handlePageChange = (val) => {
  currentPage.value = val
  fetchMemories()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  fetchMemories()
}

const formatTags = (tags) => tags ? tags.split(',').filter(t => t) : []

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

const getSentimentType = (score) => {
  if (score > 0.3) return 'success'
  if (score < -0.3) return 'danger'
  return 'info'
}

// 表单逻辑
const dialogVisible = ref(false)
const dialogType = ref('add')
const submitting = ref(false)
const formRef = ref(null)
const form = reactive({
  id: null,
  title: '',
  eventDate: '',
  tags: '',
  content: ''
})

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  eventDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

const handleAdd = () => {
  dialogType.value = 'add'
  Object.assign(form, { id: null, title: '', eventDate: '', tags: '', content: '' })
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogType.value = 'edit'
  Object.assign(form, row)
  dialogVisible.value = true
}

const submitForm = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (valid) {
      submitting.value = true
      try {
        if (dialogType.value === 'add') {
          await axios.post('/api/memories', form)
        } else {
          await axios.put(`/api/memories/${form.id}`, form)
        }
        ElMessage.success('记忆已永恒留存')
        dialogVisible.value = false
        fetchMemories()
      } catch (error) {
        ElMessage.error('保存失败')
      } finally {
        submitting.value = false
      }
    }
  })
}

const handleDelete = async (row) => {
  try {
    await axios.delete(`/api/memories/${row.id}`)
    ElMessage.success('记忆已进入虚无')
    fetchMemories()
  } catch (error) {
    ElMessage.error('删除失败')
  }
}

// 详情逻辑
const drawerVisible = ref(false)
const selectedMemory = ref(null)
const handleView = (row) => {
  selectedMemory.value = row
  drawerVisible.value = true
}
</script>

<style lang="scss" scoped>
.memories-showroom {
  padding: 40px;
  max-width: 1400px;
  margin: 0 auto;
}

/* 头部样式 */
.showroom-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 50px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  padding-bottom: 30px;

  .header-content {
    .showroom-subtitle {
      color: var(--accent-quaternary);
      font-size: 14px;
      letter-spacing: 2px;
      margin-top: 10px;
      opacity: 0.8;
    }
  }

  .header-actions {
    display: flex;
    gap: 15px;
  }
}

/* 检索区域 - 增强彩色感 */
.search-vault {
  background: rgba(15, 18, 24, 0.4);
  backdrop-filter: blur(40px) saturate(160%);
  padding: 35px 40px;
  border-radius: 32px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  margin-bottom: 50px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.4), inset 0 0 40px rgba(157, 80, 187, 0.03);
  
  .search-grid {
    display: grid;
    grid-template-columns: 2fr 1.5fr 1fr;
    gap: 30px;
    align-items: flex-end;
  }

  .search-item {
    display: flex;
    flex-direction: column;
    gap: 12px;
    .item-label {
      font-family: var(--font-title);
      font-size: 13px;
      color: #718096;
      letter-spacing: 2px;
      padding-left: 5px;
    }
  }
}

/* 彻底修复筛选框白底 */
:deep(.museum-select), :deep(.museum-date-picker), :deep(.museum-input) {
  .el-input__wrapper, .el-select__wrapper {
    background-color: rgba(0, 0, 0, 0.4) !important;
    background: rgba(0, 0, 0, 0.4) !important;
    box-shadow: 0 0 0 1px rgba(255, 255, 255, 0.1) inset !important;
    &:hover { box-shadow: 0 0 0 1px rgba(157, 80, 187, 0.4) inset !important; }
    &.is-focus { box-shadow: 0 0 0 1px var(--accent-mystic) inset !important; }
  }
  .el-input__inner, .el-select__input {
    color: #fff !important;
    &::placeholder { color: #4a5568 !important; }
  }
  .el-select__placeholder {
    color: #4a5568 !important;
  }
}

:deep(.museum-date-picker) {
  .el-range-input { background: transparent !important; color: #fff !important; }
  .el-range-separator { color: var(--accent-mystic) !important; }
}

/* 展陈列表 */
.exhibit-area {
  .exhibit-table-wrapper {
    background: rgba(15, 18, 24, 0.3);
    backdrop-filter: blur(20px);
    border-radius: 32px;
    border: 1px solid rgba(255, 255, 255, 0.06);
    overflow: hidden;
    box-shadow: 0 30px 80px rgba(0, 0, 0, 0.5);
    padding: 10px;
  }
}

/* 强制深度重写 Element Plus 表格样式以消除白底 */
:deep(.museum-table) {
  --el-table-border-color: rgba(255, 255, 255, 0.05);
  --el-table-header-bg-color: transparent;
  --el-table-tr-bg-color: transparent;
  --el-table-row-hover-bg-color: rgba(255, 255, 255, 0.03);
  background: transparent !important;
  
  &::before, &::after { display: none !important; }
  
  .el-table__header {
    th {
      border-bottom: 1px solid rgba(255, 255, 255, 0.08) !important;
      padding: 24px 15px;
      .cell {
        font-family: var(--font-title);
        color: #718096 !important;
        letter-spacing: 2px;
        font-weight: 600;
        text-transform: uppercase;
        font-size: 13px;
      }
    }
  }

  .el-table__row {
    transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
    td {
      padding: 20px 15px;
      border-bottom: 1px solid rgba(255, 255, 255, 0.03);
    }
    &:last-child td { border-bottom: none; }
    
    &.museum-row:hover {
      background: rgba(157, 80, 187, 0.05) !important;
      transform: scale(1.002) translateY(-1px);
      box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
    }
  }

  .el-table__body-wrapper { background: transparent !important; }
  .el-table__inner-wrapper::before { display: none !important; }
}

/* 按钮样式扩展 */
.action-btn-gold {
  background: var(--grad-mystic);
  border: none;
  color: #fff;
  font-weight: 700;
  padding: 12px 28px;
  border-radius: 14px;
  font-family: var(--font-title);
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 10px 25px rgba(157, 80, 187, 0.3);
  &:hover { 
    transform: translateY(-3px) scale(1.02); 
    box-shadow: 0 15px 35px rgba(157, 80, 187, 0.5); 
    color: #fff;
  }
}

.action-btn-glass {
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;
  padding: 12px 28px;
  border-radius: 14px;
  font-family: var(--font-title);
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;
  backdrop-filter: blur(10px);
  &:hover { background: rgba(255, 255, 255, 0.08); border-color: var(--accent-quaternary); color: var(--accent-quaternary); }
}

/* 表格内元素美化 */
.time-stamp {
  font-family: var(--font-title);
  color: var(--accent-quaternary);
  font-size: 13px;
  letter-spacing: 1px;
}

.memory-name-cell {
  cursor: pointer;
  padding: 10px 0;
  .title-text {
    font-size: 17px;
    color: #fff;
    font-weight: 600;
    transition: all 0.3s;
    display: block;
    margin-bottom: 6px;
  }
  &:hover .title-text { color: var(--accent-mystic); text-shadow: 0 0 10px rgba(157, 80, 187, 0.3); }
  .tag-cloud {
    display: flex;
    gap: 10px;
    .mini-tag { 
      font-size: 11px; 
      color: var(--accent-secondary); 
      background: rgba(44, 182, 125, 0.1);
      padding: 2px 8px;
      border-radius: 6px;
      border: 1px solid rgba(44, 182, 125, 0.1);
    }
  }
}

.sentiment-badge {
  display: flex;
  align-items: center;
  gap: 10px;
  background: rgba(255, 255, 255, 0.02);
  padding: 6px 12px;
  border-radius: 10px;
  width: fit-content;
  .badge-dot {
    width: 8px; height: 8px;
    border-radius: 50%;
    background: var(--color);
    box-shadow: 0 0 12px var(--color);
    animation: pulse 2s infinite;
  }
  .badge-label { font-size: 13px; color: #a0aec0; font-family: var(--font-title); font-weight: 500; }
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 0.8; }
  50% { transform: scale(1.3); opacity: 1; }
  100% { transform: scale(1); opacity: 0.8; }
}

.row-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  .icon-btn {
    background: rgba(255, 255, 255, 0.03);
    border: 1px solid rgba(255, 255, 255, 0.08);
    color: #a0aec0;
    width: 36px; height: 36px;
    border-radius: 10px;
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.3s;
    &:hover { border-color: var(--accent-mystic); color: #fff; background: rgba(157, 80, 187, 0.1); }
    &.delete:hover { border-color: var(--accent-tertiary); color: var(--accent-tertiary); background: rgba(239, 69, 101, 0.1); }
  }
}

/* 详情抽屉内部样式 */
.memory-scroll-detail {
  padding: 10px;
  .detail-tags { margin-bottom: 25px; display: flex; flex-wrap: wrap; gap: 12px; }
  .glow-tag { 
    color: var(--accent-quaternary); 
    background: rgba(0, 209, 255, 0.1);
    padding: 4px 14px;
    border-radius: 12px;
    border: 1px solid rgba(0, 209, 255, 0.2);
    font-size: 13px;
  }
  .detail-sentiment { 
    margin-bottom: 35px; 
    font-size: 15px; 
    color: #a0aec0; 
    display: flex;
    align-items: center;
    gap: 10px;
    .value { font-weight: bold; letter-spacing: 1px; }
  }
  .scroll-content {
    font-size: 19px;
    line-height: 2.2;
    color: #e2e8f0;
    white-space: pre-wrap;
    font-family: 'Noto Serif SC', serif;
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.02) 0%, rgba(255, 255, 255, 0) 100%);
    padding: 45px;
    border-radius: 32px;
    border: 1px solid rgba(255, 255, 255, 0.05);
    box-shadow: inset 0 0 30px rgba(0, 0, 0, 0.2);
  }
}

/* 分页器 */
.showroom-pagination {
  margin-top: 50px;
  padding-bottom: 20px;
}

/* 弹窗底部按钮排列优化 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 20px;
  width: 100%;
}

.museum-form {
  :deep(.el-form-item__label) {
    color: #a0aec0;
    font-family: var(--font-title);
    letter-spacing: 1px;
    padding-top: 10px;
  }
  :deep(.el-form-item) {
    margin-bottom: 25px;
  }
}
</style>
