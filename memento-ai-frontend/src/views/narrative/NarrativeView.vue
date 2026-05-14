<template>
  <div class="narrative-container museum-fade-in">
    <div class="museum-header">
      <h1 class="museum-title">Narrative Weaving</h1>
      <p class="museum-subtitle">将零散的时光片段，编织成永恒的生命史诗</p>
    </div>

    <div v-if="!isGenerating && !showResult" class="setup-wizard">
      <div class="museum-exhibit-card wizard-card">
        <div class="card-header">
          <span class="header-title">叙事编织仪式</span>
          <div class="header-dot"></div>
        </div>
        
        <el-steps :active="activeStep" finish-status="success" align-center class="museum-steps">
          <el-step title="时间溯源" />
          <el-step title="主题萃取" />
          <el-step title="笔触选择" />
        </el-steps>

        <div class="step-content">
          <!-- Step 1 -->
          <div v-if="activeStep === 0" class="step-pane">
            <h3 class="step-title">您想回顾哪段尘封的岁月？</h3>
            <div class="input-glow-wrapper">
              <el-date-picker
                v-model="config.dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开启"
                end-placeholder="终了"
                value-format="YYYY-MM-DD"
                class="museum-date-picker"
              />
            </div>
          </div>

          <!-- Step 2 -->
          <div v-if="activeStep === 1" class="step-pane">
            <h3 class="step-title">哪些心境将成为叙事的主轴？</h3>
            <div class="checkbox-group-art">
              <el-checkbox-group v-model="config.selectedClusters">
                <el-checkbox label="旅行足迹" border class="museum-checkbox" />
                <el-checkbox label="职场成长" border class="museum-checkbox" />
                <el-checkbox label="生活点滴" border class="museum-checkbox" />
              </el-checkbox-group>
            </div>
          </div>

          <!-- Step 3 -->
          <div v-if="activeStep === 2" class="step-pane">
            <h3 class="step-title">请为这段回忆定下基调</h3>
            <div class="radio-group-art">
              <el-radio-group v-model="config.style">
                <el-radio label="prose" border class="museum-radio">优美散文</el-radio>
                <el-radio label="diary" border class="museum-radio">私密日记</el-radio>
                <el-radio label="novel" border class="museum-radio">小说纪实</el-radio>
              </el-radio-group>
            </div>
          </div>
        </div>

        <div class="step-footer">
          <el-button v-if="activeStep > 0" plain class="museum-btn-secondary" @click="activeStep--">回溯</el-button>
          <el-button v-if="activeStep < 2" type="primary" class="museum-btn-primary" @click="activeStep++">下一步</el-button>
          <button v-if="activeStep === 2" class="ritual-button primary" @click="startGeneration">
            <span>启动编织引擎</span>
            <div class="button-glow"></div>
          </button>
        </div>
      </div>
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
        <div class="narrative-paper">
          <div class="paper-header">
            <h2 class="title">{{ narrativeResult.title }}</h2>
            <div class="meta">
              <span>公元 {{ narrativeResult.date }}</span>
              <span>卷轴长 {{ narrativeResult.wordCount }} 字</span>
            </div>
          </div>
          <div class="paper-body">
            <p v-for="(p, index) in narrativeResult.paragraphs" :key="index" class="paragraph">
              {{ p }}
            </p>
          </div>
          <div class="paper-footer">
            <el-button plain class="museum-btn-secondary" @click="resetWizard">重新编织</el-button>
            <button class="ritual-button primary small" @click="handleExport">
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
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { EditPen } from '@element-plus/icons-vue'

const activeStep = ref(0)
const isGenerating = ref(false)
const showResult = ref(false)

const config = reactive({
  dateRange: [],
  selectedClusters: [],
  style: 'prose'
})

const narrativeResult = reactive({
  title: '',
  date: '',
  wordCount: 0,
  paragraphs: []
})

const startGeneration = () => {
  if (config.selectedClusters.length === 0) {
    ElMessage.warning('请至少萃取一个主题')
    return
  }
  isGenerating.value = true
  
  setTimeout(() => {
    isGenerating.value = false
    showResult.value = true
    narrativeResult.title = '时间的涟漪：我的 2024 初夏回忆'
    narrativeResult.date = new Date().toLocaleDateString()
    narrativeResult.wordCount = 450
    narrativeResult.paragraphs = [
      '初夏的微风总是带着一丝慵懒，西湖边的落日将湖面染成了一片金色的海洋。在那一刻，时间仿佛静止了，所有的忙碌与疲惫都在这温柔的光影中消散。',
      '职场上的挑战依然存在，入职新公司的第一天，那种既期待又忐忑的心情至今记忆犹新。面对全新的环境和未知的项目，我曾感到迷茫，但每一次攻克难关后的成就感，都成为了我前进的动力。',
      '生活并不总是完美的，感冒时的虚弱、面试失败后的失落，这些碎片也是记忆的一部分。它们如同珍珠，串联起了我这段时间的真实体验。感谢这些瞬间，让我学会了坚韧，也学会了珍惜当下的每一份宁静。'
    ]
  }, 2500)
}

const resetWizard = () => {
  showResult.value = false
  activeStep.value = 0
  config.selectedClusters = []
}

const handleExport = () => {
  ElMessage.success('正在为您雕版印刷...')
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
    -webkit-text-fill-color: transparent;
    margin: 0;
  }
  .museum-subtitle {
    color: var(--accent-primary);
    letter-spacing: 4px;
    font-size: 14px;
    margin-top: 15px;
  }
}

.museum-exhibit-card {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 32px;
  padding: 50px;
  backdrop-filter: blur(20px);
  
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 40px;
    padding-bottom: 20px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    .header-title { font-family: var(--font-title); color: #fff; font-size: 18px; letter-spacing: 2px; }
    .header-dot { width: 6px; height: 6px; background: var(--accent-primary); border-radius: 50%; box-shadow: 0 0 10px var(--accent-primary); }
  }
}

.step-title {
  color: #fff;
  font-family: var(--font-title);
  font-size: 24px;
  margin-bottom: 50px;
  letter-spacing: 2px;
}

.museum-steps {
  margin-bottom: 60px;
  :deep(.el-step__title) { font-family: var(--font-title); font-size: 13px; letter-spacing: 1px; }
  :deep(.el-step__icon) { background: transparent; border-color: rgba(255,255,255,0.1); }
  :deep(.el-step__head.is-success) { color: var(--accent-secondary); border-color: var(--accent-secondary); }
}

.ritual-button {
  position: relative;
  background: var(--accent-primary);
  border: none;
  color: #fff;
  padding: 18px 50px;
  border-radius: 50px;
  font-family: var(--font-title);
  font-size: 16px;
  letter-spacing: 4px;
  cursor: pointer;
  transition: all 0.4s;
  box-shadow: 0 10px 30px rgba(127, 90, 240, 0.3);
  
  &:hover { transform: scale(1.05) translateY(-2px); box-shadow: 0 15px 40px rgba(127, 90, 240, 0.5); }
  &.small { padding: 12px 30px; font-size: 14px; }
}

.generating-state {
  text-align: center;
  padding: 100px 0;
  .alchemy-title { color: #fff; font-family: var(--font-title); font-size: 24px; margin-top: 40px; }
  .alchemy-subtitle { color: var(--accent-primary); margin-top: 10px; opacity: 0.8; }
}

.alchemy-circle {
  position: relative; width: 180px; height: 180px; margin: 0 auto;
  .orbit {
    position: absolute; top: 0; left: 0; width: 100%; height: 100%;
    border: 1px solid rgba(127, 90, 240, 0.2); border-radius: 50%;
    animation: rotate 8s linear infinite;
    &:nth-child(2) { border-style: dashed; animation-duration: 12s; transform: rotateX(60deg); }
  }
  .core-eye { position: absolute; top: 50%; left: 50%; transform: translate(-50%, -50%); font-size: 40px; color: var(--accent-primary); }
}

@keyframes rotate { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.narrative-paper {
  background: #fffaf0 !important;
  border: 1px solid #e0d5c1 !important;
  box-shadow: 0 30px 70px rgba(0,0,0,0.6) !important;
  border-radius: 4px !important;
  padding: 80px 60px;
  position: relative;
  
  &::before {
    content: ''; position: absolute; top: 0; left: 0; width: 100%; height: 100%;
    background-image: url("data:image/svg+xml,%3Csvg width='100' height='100' viewBox='0 0 100 100' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M11 18c3.866 0 7-3.134 7-7s-3.134-7-7-7-7 3.134-7 7 3.134 7 7 7zm48 25c3.866 0 7-3.134 7-7s-3.134-7-7-7-7 3.134-7 7 3.134 7 7 7zm-43-7c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zm63 31c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zM34 90c1.657 0 3-1.343 3-3s-1.343-3-3-3-3 1.343-3 3 1.343 3 3 3zm56-76c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM12 86c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zm28-65c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zm23-11c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zm-6 60c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zm29 22c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM10 21c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM40 59c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2zM70 80c1.105 0 2-.895 2-2s-.895-2-2-2-2 .895-2 2 .895 2 2 2z' fill='%23e0d5c1' fill-opacity='0.2' fill-rule='evenodd'/%3E%3C/svg%3E");
    pointer-events: none;
  }

  .paper-header {
    text-align: center; border-bottom: 2px solid #e0d5c1; padding-bottom: 40px; margin-bottom: 50px;
    .title { color: #5d4037; font-family: "Noto Serif SC", serif; font-size: 32px; letter-spacing: 4px; }
    .meta { color: #909399; font-size: 15px; font-family: var(--font-title); letter-spacing: 2px; margin-top: 20px; display: flex; justify-content: center; gap: 40px; }
  }

  .paper-body {
    .paragraph { text-indent: 2.5em; line-height: 2.4; font-size: 19px; color: #3e2723; margin-bottom: 35px; font-family: "Noto Serif SC", serif; }
  }
  
  .paper-footer {
    margin-top: 60px;
    padding-top: 30px;
    border-top: 1px dashed #e0d5c1;
    display: flex;
    justify-content: center;
    gap: 20px;
  }
}

.museum-btn-secondary {
  background: transparent !important;
  border: 1px solid rgba(255, 255, 255, 0.2) !important;
  color: #fff !important;
  border-radius: 50px !important;
  padding: 18px 40px !important;
  font-family: var(--font-title) !important;
  &:hover { border-color: var(--accent-primary) !important; background: rgba(127, 90, 240, 0.1) !important; }
}

.museum-btn-primary {
  border-radius: 50px !important;
  padding: 18px 40px !important;
  font-family: var(--font-title) !important;
  letter-spacing: 2px !important;
}

/* 覆盖 Element Plus 样式确保对比度 */
:deep(.el-checkbox.is-bordered) {
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  background: rgba(255, 255, 255, 0.02);
  color: #94a1b2 !important;
  &.is-checked {
    border-color: var(--accent-primary) !important;
    background: rgba(127, 90, 240, 0.1) !important;
    .el-checkbox__label { color: #fff !important; }
  }
}

:deep(.el-radio.is-bordered) {
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  background: rgba(255, 255, 255, 0.02);
  color: #94a1b2 !important;
  &.is-checked {
    border-color: var(--accent-primary) !important;
    background: rgba(127, 90, 240, 0.1) !important;
    .el-radio__label { color: #fff !important; }
  }
}

:deep(.el-date-editor) {
  --el-input-bg-color: rgba(255, 255, 255, 0.05) !important;
  --el-input-border-color: rgba(255, 255, 255, 0.1) !important;
  --el-input-text-color: #fff !important;
  .el-range-input { background: transparent !important; color: #fff !important; }
  .el-range-separator { color: #94a1b2 !important; }
}
</style>
