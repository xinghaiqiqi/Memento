<template>
  <div class="memories-container">
    <div class="filter-section">
      <el-input
        v-model="searchQuery"
        placeholder="搜索记忆标题或内容..."
        class="search-input"
        clearable
        @input="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        @change="handleDateChange"
      />
      <el-select v-model="sentimentFilter" placeholder="情感筛选" clearable @change="handleSentimentChange">
        <el-option label="全部情感" value="" />
        <el-option label="积极" value="positive" />
        <el-option label="中性" value="neutral" />
        <el-option label="消极" value="negative" />
      </el-select>
      <el-button type="primary" @click="handleAdd">
        <el-icon><Plus /></el-icon>新增记忆
      </el-button>
      <el-button @click="router.push('/import')">
        <el-icon><Upload /></el-icon>导入
      </el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="pagedData" v-loading="loading" style="width: 100%">
        <el-table-column prop="eventDate" label="发生日期" width="120" sortable />
        <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="tags" label="标签" width="150">
          <template #default="{ row }">
            <el-tag v-for="tag in formatTags(row.tags)" :key="tag" size="small" class="tag-item">
              {{ tag }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sentimentScore" label="情感分析" width="120">
          <template #default="{ row }">
            <el-tooltip :content="'得分: ' + row.sentimentScore" placement="top">
              <el-tag :type="getSentimentType(row.sentimentScore)">
                {{ getSentimentLabel(row.sentimentScore) }}
              </el-tag>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="handleView(row)">详情</el-button>
            <el-popconfirm title="确定要删除这条记忆吗？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          :total="filteredData.length"
        />
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'add' ? '新增记忆' : '编辑记忆'"
      width="600px"
    >
      <el-form :model="form" label-width="80px" :rules="rules" ref="formRef">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="给这段记忆起个标题吧" />
        </el-form-item>
        <el-form-item label="日期" prop="eventDate">
          <el-date-picker v-model="form.eventDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="form.tags" placeholder="多个标签用逗号分隔，如：旅行,开心" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="记录下此时此刻的想法..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">保存</el-button>
      </template>
    </el-dialog>

    <!-- 详情弹窗 -->
    <el-drawer
      v-model="drawerVisible"
      :title="selectedMemory?.title"
      size="45%"
    >
      <div v-if="selectedMemory" class="memory-detail">
        <div class="detail-meta">
          <el-tag type="info">{{ selectedMemory.eventDate }}</el-tag>
          <el-tag :type="getSentimentType(selectedMemory.sentimentScore)">
            情感: {{ getSentimentLabel(selectedMemory.sentimentScore) }} ({{ selectedMemory.sentimentScore }})
          </el-tag>
        </div>
        <div class="detail-tags">
          <el-tag v-for="tag in formatTags(selectedMemory.tags)" :key="tag" effect="plain">
            {{ tag }}
          </el-tag>
        </div>
        <el-divider />
        <div class="detail-content">
          {{ selectedMemory.content }}
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

const router = useRouter()

// 模拟数据
const memories = ref([
  { id: 1, title: '西湖边的落日', content: '今天和朋友一起去了西湖，落日真的很美。', eventDate: '2024-05-12', tags: '旅行,落日', sentimentScore: 0.8 },
  { id: 2, title: '完成项目', content: '终于把这个艰巨的任务完成了。', eventDate: '2024-05-10', tags: '工作,成就感', sentimentScore: 0.5 },
  { id: 3, title: '面试失败', content: '今天面试没过，有点难受。', eventDate: '2024-05-08', tags: '生活,挫折', sentimentScore: -0.6 },
])

const loading = ref(false)
const searchQuery = ref('')
const dateRange = ref([])
const sentimentFilter = ref('')
const currentPage = ref(1)
const pageSize = ref(10)

const filteredData = computed(() => {
  return memories.value.filter(item => {
    const matchSearch = item.title.includes(searchQuery.value) || item.content.includes(searchQuery.value)
    const matchDate = dateRange.value?.length ? (item.eventDate >= dateRange.value[0] && item.eventDate <= dateRange.value[1]) : true
    const matchSentiment = !sentimentFilter.value ? true : 
      sentimentFilter.value === 'positive' ? item.sentimentScore > 0.3 :
      sentimentFilter.value === 'negative' ? item.sentimentScore < -0.3 :
      (item.sentimentScore >= -0.3 && item.sentimentScore <= 0.3)
    return matchSearch && matchDate && matchSentiment
  })
})

const pagedData = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredData.value.slice(start, start + pageSize.value)
})

const formatTags = (tags) => tags ? tags.split(',').filter(t => t) : []

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
  await formRef.value.validate((valid) => {
    if (valid) {
      submitting.value = true
      // 模拟保存
      setTimeout(() => {
        if (dialogType.value === 'add') {
          memories.value.unshift({ ...form, id: Date.now(), sentimentScore: 0 })
        } else {
          const index = memories.value.findIndex(m => m.id === form.id)
          if (index > -1) memories.value[index] = { ...form }
        }
        submitting.value = false
        dialogVisible.value = false
        ElMessage.success('保存成功')
      }, 500)
    }
  })
}

const handleDelete = (row) => {
  memories.value = memories.value.filter(m => m.id !== row.id)
  ElMessage.success('删除成功')
}

// 详情逻辑
const drawerVisible = ref(false)
const selectedMemory = ref(null)
const handleView = (row) => {
  selectedMemory.value = row
  drawerVisible.value = true
}

const handleSearch = () => currentPage.value = 1
const handleDateChange = () => currentPage.value = 1
const handleSentimentChange = () => currentPage.value = 1
</script>

<style lang="scss" scoped>
.memories-container {
  padding: 40px;
  min-height: calc(100vh - 60px);

  .filter-section {
    display: flex;
    gap: 20px;
    margin-bottom: 40px;
    flex-wrap: wrap;
    align-items: center;
    background: rgba(255, 255, 255, 0.02);
    padding: 25px;
    border-radius: 20px;
    border: 1px solid rgba(255, 255, 255, 0.05);

    .search-input {
      width: 300px;
    }
  }

  .table-card {
    background: rgba(255, 255, 255, 0.01) !important;
    border: 1px solid rgba(255, 255, 255, 0.05) !important;
    border-radius: 24px;
    
    .tag-item {
      margin-right: 6px;
      margin-bottom: 6px;
      background: rgba(127, 90, 240, 0.1);
      border: 1px solid rgba(127, 90, 240, 0.2);
      color: #7f5af0;
    }
    
    .pagination-container {
      margin-top: 32px;
      display: flex;
      justify-content: center;
    }
  }

  .memory-detail {
    padding: 0 10px;
    .detail-meta {
      display: flex;
      gap: 15px;
      margin-bottom: 25px;
    }
    .detail-tags {
      display: flex;
      gap: 10px;
      flex-wrap: wrap;
      margin-bottom: 30px;
    }
    .detail-content {
      font-size: 18px;
      line-height: 2;
      color: #fffffe;
      white-space: pre-wrap;
      font-family: 'Noto Serif SC', serif;
      background: rgba(255, 255, 255, 0.02);
      padding: 30px;
      border-radius: 16px;
      border: 1px solid rgba(255, 255, 255, 0.05);
    }
  }
}

:deep(.el-input__wrapper), :deep(.el-select .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.03) !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  box-shadow: none !important;
  border-radius: 12px;
}

:deep(.el-range-editor.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.03) !important;
}

:deep(.el-table) {
  --el-table-header-bg-color: rgba(255, 255, 255, 0.02);
}
</style>
