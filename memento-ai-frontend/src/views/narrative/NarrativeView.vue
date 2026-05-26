<template>
  <div class="narrative-container museum-fade-in">
    <div class="museum-header">
      <h1 class="museum-title">叙事编织</h1>
      <p class="museum-subtitle">将零散的时光片段，编织成永恒的生命史诗</p>
    </div>

    <!-- 主交互区域 -->
    <div v-if="!isGenerating && !showResult" class="main-interaction-area">
      <!-- 切换箭头 -->
      <div class="navigation-arrows">
        <div 
          class="arrow-btn right" 
          :class="{ active: currentView === 'saved' }"
          @click="toggleView"
        >
          <div class="arrow-content">
            <span>{{ currentView === 'wizard' ? '查看已存卷轴' : '返回编织向导' }}</span>
            <el-icon><ArrowRightBold v-if="currentView === 'wizard'" /><ArrowLeftBold v-else /></el-icon>
          </div>
        </div>
      </div>

      <transition name="view-fade" mode="out-in">
        <!-- 叙事编织向导 -->
        <div v-if="currentView === 'wizard'" key="wizard" class="setup-wizard">
          <div class="museum-exhibit-card wizard-card">
            <div class="card-header">
              <span class="header-title">叙事编织仪式</span>
              <div class="header-dot"></div>
            </div>
            
            <el-steps :active="activeStep" finish-status="success" align-center class="museum-steps">
              <el-step title="时间溯源" />
              <el-step title="笔触选择" />
            </el-steps>

            <div class="step-content">
              <!-- Step 1: Time Range -->
              <div v-if="activeStep === 0" class="step-pane">
                <h3 class="step-title">您想回顾哪段尘封的岁月？</h3>
                <div class="input-glow-wrapper">
                  <el-date-picker
                    v-model="config.dateRange"
                    type="daterange"
                    range-separator="◈"
                    start-placeholder="开启"
                    end-placeholder="终了"
                    value-format="YYYY-MM-DD"
                    class="museum-date-picker"
                  />
                </div>
                <p class="step-hint">AI 将调阅此时间段内的所有记忆结晶进行编织</p>
              </div>

              <!-- Step 2: Style Selection -->
              <div v-if="activeStep === 1" class="step-pane">
                <h3 class="step-title">请为这段回忆定下基调</h3>
                <div class="radio-group-art">
                  <el-radio-group v-model="config.style">
                    <el-radio value="prose" border class="museum-radio">优美散文</el-radio>
                    <el-radio value="diary" border class="museum-radio">私密日记</el-radio>
                    <el-radio value="novel" border class="museum-radio">小说纪实</el-radio>
                  </el-radio-group>
                </div>
                <p class="step-hint">不同的笔触将赋予记忆截然不同的生命力</p>
              </div>
            </div>

            <div class="step-footer">
              <button v-if="activeStep > 0" class="museum-ritual-btn secondary" @click="activeStep--">
                <el-icon><ArrowLeft /></el-icon>
                <span>回溯</span>
              </button>
              <button v-if="activeStep < 1" class="museum-ritual-btn primary" @click="activeStep++">
                <span>下一步</span>
                <el-icon><ArrowRight /></el-icon>
              </button>
              <button v-if="activeStep === 1" class="ritual-button primary" @click="startGeneration">
                <span>启动编织引擎</span>
                <div class="button-glow"></div>
              </button>
            </div>
          </div>
        </div>

        <!-- 已存叙事卷轴 -->
        <div v-else key="saved" class="saved-narratives-section">
          <div class="section-header">
            <h3 class="section-title">已存叙事卷轴</h3>
            <div class="search-bar">
              <el-input
                v-model="searchKeyword"
                placeholder="搜索叙事题目或内容..."
                class="museum-search-input"
                clearable
                @input="fetchSavedNarratives"
              >
                <template #prefix>
                  <el-icon><Search /></el-icon>
                </template>
              </el-input>
            </div>
          </div>

          <div v-loading="loadingList" class="narratives-grid">
            <div v-for="item in savedNarratives" :key="item.id" class="narrative-card" @click="viewNarrative(item)">
              <div class="card-glow"></div>
              <div class="card-content">
                <h4 class="card-title">{{ item.title }}</h4>
                <div class="card-meta">
                  <span>{{ new Date(item.createdAt).toLocaleDateString() }}</span>
                  <span>{{ item.wordCount }} 字</span>
                </div>
                <p class="card-excerpt">{{ item.content.substring(0, 60) }}...</p>
                <div class="card-actions">
                  <el-button link class="museum-btn-link" @click.stop="handleEditSaved(item)">
                    <el-icon><EditPen /></el-icon> 编辑
                  </el-button>
                  <el-button link type="danger" @click.stop="handleDelete(item.id)">
                    <el-icon><Delete /></el-icon> 销毁
                  </el-button>
                </div>
              </div>
            </div>
            <div v-if="savedNarratives.length === 0" class="empty-narratives">
              <el-empty description="尚无存世卷轴" />
            </div>
          </div>
        </div>
      </transition>
    </div>

    <!-- Generating State -->
    <div v-if="isGenerating" class="generating-state">
      <div class="alchemy-circle">
        <div class="orbit"></div>
        <div class="orbit"></div>
        <div class="core-eye">
          <el-icon class="is-loading"><EditPen /></el-icon>
        </div>
      </div>
      <h3 class="alchemy-title">AI 正在调阅您的灵魂档案...</h3>
      <p class="alchemy-subtitle">正在为您笔耕不辍，请稍候</p>
    </div>

    <!-- Result State -->
    <div v-if="showResult" class="result-state">
      <div class="narrative-paper-wrapper">
        <div class="narrative-paper" ref="narrativePaperRef">
          <div class="paper-header">
            <h2 class="title">{{ narrativeResult.title }}</h2>
            <div class="meta">
              <span>公元 {{ narrativeResult.date }}</span>
              <span>卷轴长 {{ narrativeResult.wordCount }} 字</span>
            </div>
          </div>
          <div class="paper-body">
            <div v-if="isEditing" class="edit-area">
              <el-input
                v-model="narrativeResult.rawContent"
                type="textarea"
                :rows="15"
                class="museum-textarea"
                @input="updateParagraphs"
              />
            </div>
            <div v-else class="view-area">
              <p v-for="(p, index) in narrativeResult.paragraphs" :key="index" class="paragraph">
                {{ p }}
              </p>
            </div>
          </div>
          <div class="paper-footer">
            <el-button plain class="museum-btn-secondary" @click="resetWizard">重新编织</el-button>
            <el-button plain class="museum-btn-secondary" @click="isEditing = !isEditing">
              <el-icon><EditPen /></el-icon>
              {{ isEditing ? '完成编辑' : '手动润色' }}
            </el-button>
            <button class="ritual-button primary small" @click="handleSave" :disabled="isSaving">
              <el-icon v-if="!isSaving"><CircleCheck /></el-icon>
              <el-icon v-else class="is-loading"><Loading /></el-icon>
              <span>{{ isSaving ? '正在存入...' : '存入数据库' }}</span>
              <div class="button-glow"></div>
            </button>
            <button class="ritual-button primary small" @click="handleExport">
              <el-icon><Download /></el-icon>
              <span>烙印成册 (PDF)</span>
              <div class="button-glow"></div>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { EditPen, Download, Search, Delete, CircleCheck, Loading, ArrowRightBold, ArrowLeftBold, ArrowLeft, ArrowRight } from '@element-plus/icons-vue'
import axios from 'axios'
import html2canvas from 'html2canvas'
import jsPDF from 'jspdf'

const activeStep = ref(0)
const currentView = ref('wizard') // 'wizard' or 'saved'
const isGenerating = ref(false)
const showResult = ref(false)
const isSaving = ref(false)
const isEditing = ref(false)
const loadingList = ref(false)
const narrativePaperRef = ref(null)

const toggleView = () => {
  currentView.value = currentView.value === 'wizard' ? 'saved' : 'wizard'
}

const config = reactive({
  dateRange: [],
  style: 'prose'
})

const narrativeResult = reactive({
  id: null,
  title: '',
  date: '',
  wordCount: 0,
  paragraphs: [],
  rawContent: ''
})

const savedNarratives = ref([])
const searchKeyword = ref('')

const fetchSavedNarratives = async () => {
  loadingList.value = true
  try {
    const res = await axios.get('/api/narratives/list', {
      params: { keyword: searchKeyword.value }
    })
    if (res.data.code === 200) {
      savedNarratives.value = res.data.data
    } else {
      ElMessage.error(res.data.message || '获取列表失败')
    }
  } catch (error) {
    console.error('Failed to fetch narratives:', error)
  } finally {
    loadingList.value = false
  }
}

onMounted(() => {
  fetchSavedNarratives()
})

const startGeneration = async () => {
  if (!config.dateRange || config.dateRange.length === 0) {
    ElMessage.warning('请选择时间溯源范围')
    return
  }
  
  isGenerating.value = true
  try {
    const res = await axios.post('/api/narratives/generate', {
      dateRange: config.dateRange,
      style: config.style
    }, {
      timeout: 60000 // 增加超时时间至 60 秒
    })
    
    if (res.data.code === 200) {
      const data = res.data.data
      narrativeResult.title = data.title
      narrativeResult.date = new Date().toLocaleDateString()
      narrativeResult.wordCount = data.wordCount
      narrativeResult.rawContent = data.content
      narrativeResult.paragraphs = data.content.split('\n').filter(p => p.trim() !== '')
      showResult.value = true
    } else {
      ElMessage.error(res.data.message || '编织失败')
    }
  } catch (error) {
    console.error('Narrative generation failed:', error)
    const errorMsg = error.response?.data?.message || '编织仪式中断，请检查星象连接'
    ElMessage.error(errorMsg)
  } finally {
    isGenerating.value = false
  }
}

const handleSave = async () => {
  isSaving.value = true
  try {
    const res = await axios.post('/api/narratives/save', {
      id: narrativeResult.id || null,
      title: narrativeResult.title,
      content: narrativeResult.rawContent,
      wordCount: narrativeResult.wordCount
    })
    if (res.data.code === 200) {
      ElMessage.success(narrativeResult.id ? '叙事卷轴已更新' : '叙事卷轴已存入永恒数据库')
      fetchSavedNarratives()
      resetWizard()
    } else {
      ElMessage.error(res.data.message || '保存失败')
    }
  } catch (error) {
    console.error('Save failed:', error)
    ElMessage.error('保存失败，请检查网络连接')
  } finally {
    isSaving.value = false
  }
}

const handleEditSaved = (item) => {
  narrativeResult.id = item.id
  narrativeResult.title = item.title
  narrativeResult.rawContent = item.content
  narrativeResult.wordCount = item.wordCount
  narrativeResult.paragraphs = item.content.split('\n').filter(p => p.trim() !== '')
  narrativeResult.date = new Date(item.createdAt).toLocaleDateString()
  showResult.value = true
  isEditing.value = true
}

const updateParagraphs = () => {
  narrativeResult.paragraphs = narrativeResult.rawContent.split('\n').filter(p => p.trim() !== '')
  narrativeResult.wordCount = narrativeResult.rawContent.length
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要销毁这卷叙事吗？一旦执行，记忆编织的痕迹将不复存在。', '销毁确认', {
    confirmButtonText: '确定销毁',
    cancelButtonText: '保留卷轴',
    type: 'warning',
    customClass: 'museum-message-box'
  }).then(async () => {
    try {
      const res = await axios.delete(`/api/narratives/${id}`)
      if (res.data.code === 200) {
        ElMessage.success('叙事已归于虚无')
        fetchSavedNarratives()
      } else {
        ElMessage.error(res.data.message || '销毁失败')
      }
    } catch (error) {
      ElMessage.error('销毁失败')
    }
  })
}

const viewNarrative = (item) => {
  narrativeResult.id = item.id
  narrativeResult.title = item.title
  narrativeResult.rawContent = item.content
  narrativeResult.wordCount = item.wordCount
  narrativeResult.paragraphs = item.content.split('\n').filter(p => p.trim() !== '')
  narrativeResult.date = new Date(item.createdAt).toLocaleDateString()
  showResult.value = true
  isEditing.value = false
}

const resetWizard = () => {
  showResult.value = false
  isEditing.value = false
  activeStep.value = 0
  config.dateRange = []
  narrativeResult.id = null
}

const handleExport = async () => {
  if (!narrativePaperRef.value) return
  
  const loading = ElMessage({
    message: '正在为您雕版印刷，PDF 卷轴即将生成...',
    duration: 0,
    icon: Loading
  })

  try {
    const element = narrativePaperRef.value
    const canvas = await html2canvas(element, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#f4f1ea' // 仿古纸张背景色
    })
    
    const imgData = canvas.toDataURL('image/jpeg', 1.0)
    const pdf = new jsPDF('p', 'mm', 'a4')
    const imgProps = pdf.getImageProperties(imgData)
    const pdfWidth = pdf.internal.pageSize.getWidth()
    const pdfHeight = (imgProps.height * pdfWidth) / imgProps.width
    
    pdf.addImage(imgData, 'JPEG', 0, 0, pdfWidth, pdfHeight)
    pdf.save(`${narrativeResult.title}.pdf`)
    
    loading.close()
    ElMessage.success('卷轴已成功烙印并下载')
  } catch (error) {
    console.error('PDF Export failed:', error)
    loading.close()
    ElMessage.error('导出失败，请重试')
  }
}
</script>

<style lang="scss" scoped>
.narrative-container {
  padding: 60px 40px;
  max-width: 1200px;
  margin: 0 auto;
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
    letter-spacing: 4px;
    font-size: 14px;
    margin-top: 15px;
    text-transform: uppercase;
  }
}

.main-interaction-area {
  position: relative;
  max-width: 1000px;
  margin: 0 auto;
  padding: 0 60px;
}

.navigation-arrows {
  position: absolute;
  right: -20px;
  top: 50%;
  transform: translateY(-50%);
  z-index: 100;

  .arrow-btn {
    background: rgba(157, 80, 187, 0.15);
    backdrop-filter: blur(10px);
    border: 1px solid rgba(157, 80, 187, 0.3);
    border-radius: 12px 0 0 12px;
    padding: 20px 10px;
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    display: flex;
    align-items: center;
    writing-mode: vertical-lr;
    color: var(--accent-mystic);
    box-shadow: -5px 0 20px rgba(0, 0, 0, 0.3);

    .arrow-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 15px;
      span { font-family: var(--font-title); font-size: 13px; letter-spacing: 2px; }
      .el-icon { font-size: 18px; }
    }

    &:hover {
      background: rgba(157, 80, 187, 0.25);
      transform: translateY(-50%) translateX(-5px);
      border-color: var(--accent-mystic);
    }
    
    &.active {
      background: var(--accent-mystic);
      color: #fff;
    }
  }
}

.view-fade-enter-active, .view-fade-leave-active {
  transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
}
.view-fade-enter-from { opacity: 0; transform: translateX(30px); }
.view-fade-leave-to { opacity: 0; transform: translateX(-30px); }

.museum-ritual-btn {
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #a0aec0;
  padding: 12px 25px;
  border-radius: 12px;
  font-family: var(--font-title);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s;
  display: flex;
  align-items: center;
  gap: 8px;

  &:hover {
    background: rgba(255, 255, 255, 0.1);
    color: #fff;
    border-color: rgba(255, 255, 255, 0.2);
  }

  &.primary {
    background: rgba(127, 90, 240, 0.1);
    border-color: rgba(127, 90, 240, 0.3);
    color: var(--accent-primary);
    &:hover { background: rgba(127, 90, 240, 0.2); border-color: var(--accent-primary); color: #fff; }
  }
}

.wizard-card {
  background: rgba(15, 18, 24, 0.4);
  backdrop-filter: blur(40px) saturate(150%);
  border-radius: 30px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 50px;
  box-shadow: 0 40px 100px rgba(0, 0, 0, 0.5);
}

/* 原有样式保留并微调 */
.step-pane {
  text-align: center;
}

.step-title {
  color: #fff;
  font-family: var(--font-title);
  font-size: 26px;
  margin-bottom: 40px;
  letter-spacing: 3px;
}

.step-hint {
  color: #718096;
  font-size: 14px;
  margin-top: 30px;
  letter-spacing: 1px;
}

.input-glow-wrapper {
  display: inline-block;
  padding: 5px;
  background: rgba(157, 80, 187, 0.1);
  border-radius: 16px;
  box-shadow: 0 0 30px rgba(157, 80, 187, 0.05);
}

.radio-group-art {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.museum-steps {
  margin-bottom: 40px;
}

.step-footer {
  margin-top: 50px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 20px;
}

.ritual-button {
  position: relative;
  background: var(--grad-mystic);
  border: none;
  color: #fff;
  padding: 18px 50px;
  border-radius: 50px;
  font-family: var(--font-title);
  font-size: 16px;
  letter-spacing: 4px;
  cursor: pointer;
  transition: all 0.4s;
  box-shadow: 0 10px 30px rgba(157, 80, 187, 0.3);
  display: flex;
  align-items: center;
  gap: 12px;
  
  &:hover { transform: scale(1.05) translateY(-2px); box-shadow: 0 15px 40px rgba(157, 80, 187, 0.5); }
  &.small { padding: 12px 30px; font-size: 14px; }
}

.generating-state {
  text-align: center;
  padding: 100px 0;
  .alchemy-title { color: #fff; font-family: var(--font-title); font-size: 28px; margin-top: 50px; letter-spacing: 4px; }
  .alchemy-subtitle { color: var(--accent-mystic); margin-top: 15px; opacity: 0.8; letter-spacing: 2px; }
}

.alchemy-circle {
  position: relative; width: 220px; height: 220px; margin: 0 auto;
  .orbit {
    position: absolute; top: 0; left: 0; width: 100%; height: 100%;
    border: 1px solid rgba(157, 80, 187, 0.3); border-radius: 50%;
    animation: rotate 8s linear infinite;
    &:nth-child(2) { border-style: dashed; animation-duration: 12s; transform: rotateX(60deg); }
  }
  .core-eye { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); font-size: 50px; color: var(--accent-mystic); text-shadow: 0 0 20px var(--accent-mystic); }
}

@keyframes rotate { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.narrative-paper-wrapper {
  perspective: 2000px;
  padding-bottom: 100px;
}

.narrative-paper {
  background: #fdfaf3 !important;
  background-image: 
    linear-gradient(rgba(224, 213, 193, 0.3) 1px, transparent 1px),
    url("data:image/svg+xml,%3Csvg width='100' height='100' viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M11 18c3.866 0 7-3.134 7-7s-3.134-7-7-7-7 3.134-7 7 3.134 7 7 7z' fill='%23e0d5c1' fill-opacity='0.1'/%3E%3C/svg%3E") !important;
  background-size: 100% 2.4em, auto;
  border: 1px solid #d4c4a8 !important;
  box-shadow: 0 30px 80px rgba(0,0,0,0.5), inset 0 0 100px rgba(224, 213, 193, 0.2) !important;
  border-radius: 8px !important;
  padding: 60px 60px;
  position: relative;
  max-width: 850px;
  margin: 0 auto;
  transform: rotateX(5deg);
  transition: transform 0.6s cubic-bezier(0.34, 1.56, 0.64, 1);
  
  .paper-header {
    margin-bottom: 40px;
    text-align: center;
    border-bottom: 2px double #d4c4a8;
    padding-bottom: 20px;
    .title { font-family: var(--font-title); font-size: 32px; color: #4a3728; margin: 0 0 15px 0; letter-spacing: 6px; }
    .meta { display: flex; justify-content: center; gap: 40px; color: #8c7e6d; font-size: 14px; letter-spacing: 2px; }
  }

  .paper-body {
    min-height: 400px;
    
    .edit-area {
      .museum-textarea {
        :deep(.el-textarea__inner) {
          background: transparent !important;
          border: 1px dashed #d4c4a8 !important;
          color: #4a3728 !important;
          font-family: var(--font-title);
          font-size: 16px;
          line-height: 2.4em;
          padding: 20px;
          box-shadow: none !important;
          &::selection { background: rgba(157, 80, 187, 0.1); }
        }
      }
    }
    
    .view-area {
      .paragraph {
        font-family: var(--font-title);
        font-size: 18px;
        line-height: 2.4em;
        color: #4a3728;
        margin-bottom: 1.5em;
        text-indent: 2em;
        text-align: justify;
        letter-spacing: 1px;
      }
    }
  }

  .paper-footer {
    margin-top: 50px;
    padding-top: 30px;
    border-top: 1px solid #d4c4a8;
    display: flex;
    justify-content: center;
    gap: 20px;
  }
}

.museum-btn-secondary {
  background: transparent !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  color: #a0aec0 !important;
  border-radius: 50px !important;
  padding: 15px 35px !important;
  font-family: var(--font-title) !important;
  &:hover { border-color: var(--accent-mystic) !important; color: #fff !important; background: rgba(157, 80, 187, 0.1) !important; }
}

.museum-btn-primary {
  background: var(--grad-mystic) !important;
  border: none !important;
  color: #fff !important;
  border-radius: 50px !important;
  padding: 15px 35px !important;
  font-family: var(--font-title) !important;
  letter-spacing: 2px !important;
  box-shadow: 0 8px 20px rgba(157, 80, 187, 0.3) !important;
  &:hover { transform: translateY(-2px); box-shadow: 0 12px 25px rgba(157, 80, 187, 0.4) !important; }
}

/* Element Plus 覆盖 */
:deep(.el-radio.is-bordered) {
  border: 1px solid rgba(255, 255, 255, 0.08) !important;
  background: rgba(255, 255, 255, 0.02);
  height: 54px;
  padding: 0 30px;
  border-radius: 16px;
  &.is-checked {
    border-color: var(--accent-mystic) !important;
    background: rgba(157, 80, 187, 0.1) !important;
    .el-radio__label { color: #fff !important; }
  }
}

:deep(.el-date-editor.el-range-editor) {
  background: rgba(0, 0, 0, 0.3) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  border-radius: 14px;
  height: 54px;
  width: 400px;
  .el-range-input { color: #fff !important; }
  .el-range-separator { color: var(--accent-mystic) !important; }
}

.saved-narratives-section {
  background: rgba(15, 18, 24, 0.3);
  backdrop-filter: blur(40px);
  border-radius: 30px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  padding: 40px;
  min-height: 500px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 40px;
  .section-title {
    font-family: var(--font-title);
    color: #fff;
    font-size: 24px;
    letter-spacing: 4px;
    margin: 0;
  }
  .search-bar {
    width: 400px;
  }
}

.museum-search-input {
  :deep(.el-input__wrapper) {
    background: rgba(255, 255, 255, 0.05) !important;
    border: 1px solid rgba(255, 255, 255, 0.1) !important;
    border-radius: 12px;
    box-shadow: none !important;
    padding: 8px 15px;
  }
  :deep(.el-input__inner) {
    color: #fff !important;
    font-family: var(--font-title);
    letter-spacing: 1px;
    &::placeholder { color: #4a5568; }
  }
}

.narratives-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 30px;
}

.narrative-card {
  position: relative;
  background: rgba(15, 18, 24, 0.4);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  padding: 30px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;

  &:hover {
    transform: translateY(-10px);
    border-color: var(--accent-mystic);
    background: rgba(157, 80, 187, 0.05);
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
    .card-glow { opacity: 0.6; }
  }

  .card-glow {
    position: absolute;
    bottom: -50px; right: -50px;
    width: 150px; height: 150px;
    background: radial-gradient(circle, var(--accent-mystic), transparent 70%);
    opacity: 0.1;
    filter: blur(40px);
    transition: opacity 0.5s;
    pointer-events: none;
  }

  .card-title {
    color: #fff;
    font-family: var(--font-title);
    font-size: 18px;
    margin: 0 0 15px 0;
    letter-spacing: 2px;
  }

  .card-meta {
    display: flex;
    gap: 20px;
    color: #718096;
    font-size: 13px;
    margin-bottom: 20px;
    font-family: var(--font-title);
  }

  .card-excerpt {
    color: #a0aec0;
    font-size: 14px;
    line-height: 1.8;
    margin-bottom: 25px;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .card-actions {
    display: flex;
    justify-content: flex-end;
    border-top: 1px solid rgba(255, 255, 255, 0.05);
    padding-top: 15px;
  }
}

.empty-narratives {
  grid-column: 1 / -1;
  padding: 100px 0;
  text-align: center;
}

.dialog-narrative-content {
  color: #fff;
  .meta-info {
    display: flex;
    gap: 30px;
    color: #718096;
    font-family: var(--font-title);
    margin-bottom: 30px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    padding-bottom: 20px;
  }
  .content-body {
    .p-text {
      text-indent: 2em;
      line-height: 2;
      font-size: 16px;
      color: #e2e8f0;
      margin-bottom: 20px;
      text-align: justify;
    }
  }
}

:deep(.museum-dialog) {
  background: #0f1218 !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  border-radius: 24px !important;
  .el-dialog__title { color: #fff; font-family: var(--font-title); letter-spacing: 2px; }
  .el-dialog__body { padding: 40px; }
}
</style>