<template>
  <div class="export-container">
    <div class="export-header">
      <div class="title-section">
        <h2>导出报告</h2>
        <p>将你的记忆珍藏成册，支持 PDF 格式</p>
      </div>
    </div>

    <div class="export-body">
      <el-row :gutter="20">
        <el-col :span="14">
          <el-card shadow="never" class="config-card">
            <template #header>导出配置</template>
            <el-form :model="exportConfig" label-width="100px">
              <el-form-item label="导出范围">
                <el-radio-group v-model="exportConfig.range">
                  <el-radio label="all">全部记忆</el-radio>
                  <el-radio label="date">指定日期</el-radio>
                  <el-radio label="cluster">指定主题</el-radio>
                </el-radio-group>
              </el-form-item>
              
              <el-form-item v-if="exportConfig.range === 'date'" label="日期范围">
                <el-date-picker
                  v-model="exportConfig.dateRange"
                  type="daterange"
                  range-separator="至"
                  start-placeholder="开始日期"
                  end-placeholder="结束日期"
                  value-format="YYYY-MM-DD"
                />
              </el-form-item>

              <el-form-item v-if="exportConfig.range === 'cluster'" label="选择主题">
                <el-select v-model="exportConfig.selectedClusters" multiple placeholder="请选择主题">
                  <el-option label="旅行足迹" value="1" />
                  <el-option label="职场成长" value="2" />
                  <el-option label="生活点滴" value="3" />
                </el-select>
              </el-form-item>

              <el-form-item label="包含内容">
                <el-checkbox-group v-model="exportConfig.includes">
                  <el-checkbox label="memories">原始记忆</el-checkbox>
                  <el-checkbox label="narratives">AI 叙事</el-checkbox>
                  <el-checkbox label="charts">统计图表</el-checkbox>
                </el-checkbox-group>
              </el-form-item>

              <el-form-item label="排版风格">
                <el-radio-group v-model="exportConfig.layout">
                  <el-radio label="classic">经典书信</el-radio>
                  <el-radio label="modern">现代简约</el-radio>
                </el-radio-group>
              </el-form-item>

              <el-form-item>
                <el-button type="primary" size="large" @click="handleExport" :loading="exporting">
                  <el-icon><Download /></el-icon> 生成 PDF
                </el-button>
              </el-form-item>
            </el-form>
          </el-card>
        </el-col>

        <el-col :span="10">
          <el-card shadow="never" class="preview-card">
            <template #header>预览</template>
            <div class="book-preview">
              <div class="book-cover">
                <h3>拾光记</h3>
                <p>MEMENTO AI</p>
                <div class="author">{{ userStore.userInfo.nickname }} 著</div>
              </div>
            </div>
            <p class="preview-tip">预览仅供参考，实际导出将包含更多细节</p>
          </el-card>
        </el-col>
      </el-row>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()
const exporting = ref(false)

const exportConfig = reactive({
  range: 'all',
  dateRange: [],
  selectedClusters: [],
  includes: ['memories', 'narratives'],
  layout: 'classic'
})

const handleExport = () => {
  exporting.value = true
  setTimeout(() => {
    exporting.value = false
    ElMessage.success('导出任务已提交，完成后将自动下载')
  }, 2000)
}
</script>

<style lang="scss" scoped>
.export-container {
  padding: 40px;
  min-height: calc(100vh - 60px);

  .export-header {
    margin-bottom: 50px;
    .title-section {
      h2 { margin: 0 0 10px 0; color: #fff; font-size: 32px; text-shadow: 0 0 15px rgba(127, 90, 240, 0.4); }
      p { margin: 0; color: #94a1b2; font-size: 16px; letter-spacing: 1px; }
    }
  }

  .config-card {
    background: rgba(255, 255, 255, 0.02) !important;
    border: 1px solid rgba(255, 255, 255, 0.05) !important;
    border-radius: 24px !important;
    height: 100%;
    
    :deep(.el-card__header) {
      color: #fff;
      font-family: var(--font-title);
      letter-spacing: 1px;
      border-bottom: 1px solid rgba(255,255,255,0.05);
    }
  }

  .preview-card {
    background: rgba(255, 255, 255, 0.02) !important;
    border: 1px solid rgba(255, 255, 255, 0.05) !important;
    border-radius: 24px !important;
    text-align: center;
    
    :deep(.el-card__header) {
      color: #fff;
      font-family: var(--font-title);
      letter-spacing: 1px;
      border-bottom: 1px solid rgba(255,255,255,0.05);
    }

    .book-preview {
      margin: 40px auto;
      width: 220px;
      height: 310px;
      background: linear-gradient(135deg, #5d4037 0%, #3e2723 100%);
      border-radius: 4px 16px 16px 4px;
      box-shadow: 15px 15px 40px rgba(0,0,0,0.5), inset 2px 0 5px rgba(255,255,255,0.1);
      display: flex;
      flex-direction: column;
      justify-content: center;
      color: #d7ccc8;
      border: 1px solid rgba(255,255,255,0.05);
      
      h3 { font-size: 28px; font-family: var(--font-title); margin-bottom: 10px; letter-spacing: 4px; }
      p { font-size: 13px; letter-spacing: 3px; font-family: var(--font-title); opacity: 0.8; }
      .author { margin-top: 80px; font-size: 15px; font-family: var(--font-title); }
    }
    .preview-tip { color: #72757e; font-size: 13px; font-style: italic; }
  }
}

:deep(.el-form-item__label) {
  color: #94a1b2 !important;
  font-family: var(--font-title);
  letter-spacing: 1px;
}

:deep(.el-radio), :deep(.el-checkbox) {
  color: #94a1b2 !important;
}

:deep(.el-input__wrapper), :deep(.el-select .el-input__wrapper) {
  background: rgba(255, 255, 255, 0.03) !important;
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  box-shadow: none !important;
  border-radius: 12px;
}
</style>
