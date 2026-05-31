<template>
  <div class="export-container">
    <div class="museum-header">
      <h1 class="museum-title">永恒归档</h1>
      <p class="museum-subtitle">将你的记忆珍藏成册，支持 PDF 格式</p>
    </div>

    <div class="export-body">
      <el-row :gutter="20">
        <el-col :span="14">
          <el-card shadow="never" class="config-card">
            <template #header>导出配置</template>
            <el-form :model="exportConfig" label-width="100px">
              <el-form-item label="导出范围">
                <el-radio-group v-model="exportConfig.range">
                  <el-radio value="all">全部记忆</el-radio>
                  <el-radio value="date">指定日期</el-radio>
                  <el-radio value="cluster">指定主题</el-radio>
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
                  <el-checkbox value="memories">原始记忆</el-checkbox>
                  <el-checkbox value="narratives">AI 叙事</el-checkbox>
                  <el-checkbox value="charts">统计图表</el-checkbox>
                </el-checkbox-group>
              </el-form-item>

              <el-form-item label="排版风格">
                <el-radio-group v-model="exportConfig.layout">
                  <el-radio value="classic">经典书信</el-radio>
                  <el-radio value="modern">现代简约</el-radio>
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
import { ElMessage, ElMessageBox } from 'element-plus'

import axios from 'axios'

const userStore = useUserStore()
const exporting = ref(false)

const exportConfig = reactive({
  range: 'all',
  dateRange: [],
  selectedClusters: [],
  includes: ['memories', 'narratives'],
  layout: 'classic'
})

const readBlobErrorMessage = async (error) => {
  const data = error.response?.data
  if (!data) return error.message || '导出失败'

  try {
    const text = data instanceof Blob ? await data.text() : String(data)
    const parsed = JSON.parse(text)
    return parsed.message || text || '导出失败'
  } catch {
    return error.message || '导出失败'
  }
}

const showExportError = (message) => {
  ElMessageBox.alert(message, '无法导出', {
    confirmButtonText: '知道了',
    type: 'error'
  })
}

const handleExport = async () => {
  exporting.value = true
  try {
    const res = await axios.post('/api/export/create', exportConfig, {
      responseType: 'blob'
    })
    
    const contentType = res.headers?.['content-type'] || ''
    if (!contentType.includes('application/pdf')) {
      const text = res.data instanceof Blob ? await res.data.text() : String(res.data)
      let message = '导出失败'
      try {
        message = JSON.parse(text).message || message
      } catch {
        message = text || message
      }
      showExportError(message)
      return
    }

    // 创建下载链接
    const blob = new Blob([res.data], { type: 'application/pdf' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    
    // 生成文件名
    const timestamp = new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-')
    link.download = `memento_${timestamp}.pdf`
    
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    
    ElMessage.success('PDF导出成功！')
  } catch (error) {
    const message = await readBlobErrorMessage(error)
    showExportError(message)
  } finally {
    exporting.value = false
  }
}
</script>

<style lang="scss" scoped>
.export-container {
  padding: 40px;
  min-height: calc(100vh - 60px);

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
