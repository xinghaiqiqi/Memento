<template>
  <div class="import-container museum-fade-in">
    <div class="museum-header">
      <h1 class="museum-title">记忆实验室</h1>
      <p class="museum-subtitle">萃取时间残片，凝练永恒记忆</p>
    </div>

    <div class="laboratory-core">
      <!-- 艺术化进度指示器 -->
      <div class="artistic-steps">
        <div v-for="(step, index) in steps" :key="index" 
             :class="['step-item', { active: activeStep === index, completed: activeStep > index }]">
          <div class="step-dot"></div>
          <span class="step-label">{{ step }}</span>
        </div>
      </div>

      <div class="glass-chamber">
        <transition name="chamber-fade" mode="out-in">
          <!-- Step 0: Ritual of Input -->
          <div v-if="activeStep === 0" class="ritual-pane">
            <div class="input-sphere">
              <el-tabs v-model="inputMode" class="ritual-tabs">
                <el-tab-pane label="灵感转录" name="text">
                  <div class="textarea-wrapper">
                    <el-input
                      v-model="rawText"
                      type="textarea"
                      :rows="12"
                      placeholder="在此倾倒您的思绪碎片、聊天记录或昨日随笔..."
                    />
                    <div class="glow-border"></div>
                  </div>
                </el-tab-pane>
                <el-tab-pane label="卷轴上传" name="file">
                  <div class="shrine-upload">
                    <el-upload
                      drag
                      action="#"
                      :auto-upload="false"
                      :on-change="handleFileChange"
                    >
                      <div class="upload-icon-aura">
                        <el-icon><MagicStick /></el-icon>
                      </div>
                      <div class="upload-text">将记忆卷轴放入祭坛</div>
                    </el-upload>
                  </div>
                </el-tab-pane>
              </el-tabs>
            </div>
            
            <div class="alchemy-trigger">
              <button class="ritual-button primary" @click="startExtraction" :disabled="!canExtract">
                <span>启动记忆萃取仪式</span>
                <div class="button-glow"></div>
              </button>
            </div>
          </div>

          <!-- Step 1: The Alchemy Process -->
          <div v-else-if="activeStep === 1" class="alchemy-pane">
            <div class="alchemy-circle">
              <div class="orbit"></div>
              <div class="orbit"></div>
              <div class="core-eye">
                <el-icon class="is-loading"><Compass /></el-icon>
              </div>
            </div>
            <h3 class="alchemy-title">AI 正在编织时间经纬...</h3>
            <p class="alchemy-subtitle">正在从混沌中提取闪光的瞬间</p>
          </div>

          <!-- Step 2: Revelation -->
          <div v-else-if="activeStep === 2" class="revelation-pane">
            <div class="revelation-header">
              <span class="count-badge">已萃取 {{ previewData.length }} 枚记忆结晶</span>
              <p class="revelation-hint">点击结晶可以进行微调，或点击“遗忘”剔除不准确的部分</p>
            </div>
            
            <div class="crystal-grid">
              <div v-for="(item, index) in previewData" :key="index" class="crystal-card">
                <div class="card-aura" :style="{ '--aura-color': getSentimentColor(item.sentimentScore || 0) }"></div>
                <div class="card-content">
                  <div class="card-header">
                    <span class="card-date">{{ item.eventDate }}</span>
                    <el-button link class="forget-btn" @click="previewData.splice(index, 1)">遗忘</el-button>
                  </div>
                  <h3 class="card-title">{{ item.title }}</h3>
                  <p class="card-text">{{ item.content }}</p>
                  
                  <div class="card-photo-upload">
                    <el-upload
                      class="mini-uploader"
                      action="/api/file/upload"
                      :show-file-list="false"
                      :on-success="(res) => item.photoUrl = res.data"
                    >
                      <div v-if="item.photoUrl" class="mini-photo-preview" :style="{ backgroundImage: `url(${item.photoUrl})` }">
                        <div class="change-hint">点击更换</div>
                      </div>
                      <div v-else class="mini-upload-placeholder">
                        <el-icon><Picture /></el-icon>
                        <span>添加瞬间影像</span>
                      </div>
                    </el-upload>
                  </div>

                  <div class="card-footer">
                    <div class="sentiment-indicator" :style="{ color: getSentimentColor(item.sentimentScore || 0) }">
                      <div class="indicator-dot"></div>
                      <span>{{ getSentimentLabel(item.sentimentScore || 0) }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <div class="final-actions">
              <button class="ritual-button secondary" @click="activeStep = 0">
                <el-icon><ArrowLeft /></el-icon>
                <span>重塑仪式</span>
              </button>
              <button class="ritual-button primary" @click="handleConfirmImport" :loading="confirming">
                <el-icon><Finished /></el-icon>
                <span>存入永恒陈列馆</span>
                <div class="button-glow"></div>
              </button>
            </div>
          </div>
        </transition>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Compass, Plus, Picture, ArrowLeft, Finished } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const steps = ['输入素材', '萃取仪式', '预览确认']
const activeStep = ref(0)
const inputMode = ref('text')
const rawText = ref('')
const fileList = ref([])
const previewData = ref([])
const confirming = ref(false)

const canExtract = computed(() => {
  return inputMode.value === 'text' ? rawText.value.length > 10 : fileList.value.length > 0
})

const handleFileChange = (file) => {
  fileList.value = [file]
}

const startExtraction = async () => {
  activeStep.value = 1
  try {
    let res;
    if (inputMode.value === 'text') {
      res = await axios.post('/api/import/text', { text: rawText.value })
    } else {
      const formData = new FormData()
      formData.append('file', fileList.value[0].raw)
      res = await axios.post('/api/import/upload', formData)
    }
    if (res.data.code === 200) {
      previewData.value = res.data.data || []
      setTimeout(() => { activeStep.value = 2 }, 1500)
    } else {
      throw new Error(res.data.message)
    }
  } catch (error) {
    ElMessage.error(error.message || '萃取失败，请检查星象连接')
    activeStep.value = 0
  }
}

const handleConfirmImport = async () => {
  confirming.value = true
  try {
    const res = await axios.post('/api/import/confirm', previewData.value)
    if (res.data.code === 200 && res.data.data) {
      ElMessage.success('记忆已永恒留存')
      router.push('/memories')
    } else {
      throw new Error(res.data.message || '存入失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '存入失败')
  } finally {
    confirming.value = false
  }
}

const getSentimentColor = (score) => {
  if (score > 0.3) return '#2cb67d'
  if (score < -0.3) return '#ef4565'
  return '#7f5af0'
}

const getSentimentLabel = (score) => {
  if (score > 0.3) return '悦'
  if (score < -0.3) return '忧'
  return '平'
}
</script>

<style lang="scss" scoped>
.import-container {
  padding: 40px;
  max-width: 1200px;
  margin: 0 auto;
  position: relative;
  z-index: 1;
}

.museum-header {
  text-align: center;
  margin-bottom: 60px;
  .museum-subtitle {
    color: var(--accent-quaternary);
    font-size: 16px;
    letter-spacing: 4px;
    margin-top: 15px;
    opacity: 0.8;
    text-transform: uppercase;
  }
}

/* 进度条艺术化 */
.artistic-steps {
  display: flex;
  justify-content: center;
  gap: 80px;
  margin-bottom: 60px;
  
  .step-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 15px;
    opacity: 0.25;
    transition: all 0.8s cubic-bezier(0.34, 1.56, 0.64, 1);
    
    .step-dot {
      width: 12px; height: 12px;
      border-radius: 50%;
      background: #fff;
      box-shadow: 0 0 15px rgba(255, 255, 255, 0.3);
      position: relative;
      &::after {
        content: '';
        position: absolute;
        inset: -6px;
        border: 1px solid rgba(255, 255, 255, 0.1);
        border-radius: 50%;
      }
    }
    .step-label { font-family: var(--font-title); font-size: 14px; letter-spacing: 3px; color: #a0aec0; }
    
    &.active {
      opacity: 1;
      transform: scale(1.15);
      .step-dot { 
        background: var(--accent-mystic); 
        box-shadow: 0 0 25px var(--accent-mystic); 
        &::after { border-color: var(--accent-mystic); animation: pulse 2s infinite; }
      }
      .step-label { color: #fff; font-weight: bold; text-shadow: 0 0 10px rgba(255,255,255,0.3); }
    }
    
    &.completed {
      opacity: 0.7;
      .step-dot { background: var(--accent-secondary); box-shadow: 0 0 15px var(--accent-secondary); }
      .step-label { color: var(--accent-secondary); }
    }
  }
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 1; }
  100% { transform: scale(2); opacity: 0; }
}

.glass-chamber {
  background: rgba(15, 18, 24, 0.4);
  backdrop-filter: blur(40px) saturate(150%);
  border-radius: 40px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  padding: 60px;
  min-height: 550px;
  box-shadow: 0 40px 100px rgba(0, 0, 0, 0.5), inset 0 0 40px rgba(255,255,255,0.02);
  position: relative;
  overflow: hidden;
}

/* Revelation Pane Styles */
.revelation-header {
  text-align: center;
  margin-bottom: 40px;
  .count-badge {
    display: inline-block;
    padding: 8px 24px;
    background: var(--grad-mystic);
    border-radius: 20px;
    font-family: var(--font-title);
    font-size: 18px;
    letter-spacing: 2px;
    box-shadow: 0 10px 30px rgba(157, 80, 187, 0.3);
    margin-bottom: 15px;
  }
  .revelation-hint {
    color: #718096;
    font-size: 14px;
  }
}

.crystal-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 25px;
  margin-bottom: 50px;
  max-height: 500px;
  overflow-y: auto;
  padding: 10px;
  
  &::-webkit-scrollbar { width: 6px; }
  &::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.1); border-radius: 3px; }
}

.crystal-card {
  position: relative;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  padding: 25px;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  
  &:hover {
    transform: translateY(-8px);
    background: rgba(255, 255, 255, 0.06);
    border-color: rgba(255, 255, 255, 0.2);
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
    .card-aura { opacity: 0.4; }
  }

  .card-aura {
    position: absolute;
    top: -50%; left: -50%; width: 200%; height: 200%;
    background: radial-gradient(circle at center, var(--aura-color), transparent 60%);
    opacity: 0.15;
    transition: opacity 0.4s;
    pointer-events: none;
    z-index: 0;
  }

  .card-content { position: relative; z-index: 1; }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 15px;
    .card-date {
      font-family: var(--font-title);
      font-size: 13px;
      color: var(--accent-quaternary);
      letter-spacing: 1px;
    }
    .forget-btn {
      color: #718096;
      font-size: 12px;
      &:hover { color: var(--accent-tertiary); }
    }
  }

  .card-title {
    font-family: var(--font-title);
    font-size: 18px;
    margin: 0 0 12px 0;
    color: #fff;
    letter-spacing: 1px;
  }

  .card-text {
    font-size: 14px;
    color: #a0aec0;
    line-height: 1.6;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
    margin-bottom: 20px;
  }

  .card-photo-upload {
    margin-bottom: 20px;
    .mini-uploader {
      :deep(.el-upload) {
        width: 100%;
        height: 120px;
        border: 1px dashed rgba(255, 255, 255, 0.1);
        border-radius: 12px;
        background: rgba(0, 0, 0, 0.2);
        overflow: hidden;
        transition: all 0.3s;
        &:hover { border-color: var(--accent-mystic); background: rgba(157, 80, 187, 0.05); }
      }
    }
    .mini-photo-preview {
      width: 100%; height: 120px;
      background-size: cover;
      background-position: center;
      display: flex;
      align-items: center;
      justify-content: center;
      .change-hint {
        background: rgba(0, 0, 0, 0.5);
        color: #fff;
        padding: 4px 12px;
        border-radius: 20px;
        font-size: 12px;
        opacity: 0;
        transition: opacity 0.3s;
      }
      &:hover .change-hint { opacity: 1; }
    }
    .mini-upload-placeholder {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      color: #718096;
      font-size: 12px;
      .el-icon { font-size: 24px; }
    }
  }

  .card-footer {
    display: flex;
    justify-content: flex-end;
    .sentiment-indicator {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 12px;
      font-weight: bold;
      .indicator-dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; box-shadow: 0 0 8px currentColor; }
    }
  }
}

.final-actions {
  display: flex;
  justify-content: center;
  gap: 30px;
  padding-top: 30px;
  border-top: 1px solid rgba(255, 255, 255, 0.05);
}

/* Ritual Button 深度定制 */
.ritual-button {
  position: relative;
  padding: 16px 40px;
  border-radius: 20px;
  border: none;
  background: transparent;
  color: #fff;
  font-family: var(--font-title);
  font-size: 18px;
  letter-spacing: 3px;
  cursor: pointer;
  transition: all 0.4s;
  display: flex;
  align-items: center;
  gap: 12px;
  overflow: hidden;

  &.primary {
    background: var(--grad-mystic);
    box-shadow: 0 10px 30px rgba(157, 80, 187, 0.4);
    &:hover {
      transform: translateY(-3px) scale(1.02);
      box-shadow: 0 15px 40px rgba(157, 80, 187, 0.6);
    }
  }

  &.secondary {
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    &:hover {
      background: rgba(255, 255, 255, 0.1);
      border-color: rgba(255, 255, 255, 0.3);
    }
  }

  .button-glow {
    position: absolute;
    top: -50%; left: -50%; width: 200%; height: 200%;
    background: radial-gradient(circle at center, rgba(255,255,255,0.2), transparent 70%);
    opacity: 0;
    transition: opacity 0.4s;
  }

  &:hover .button-glow { opacity: 1; }
  &:disabled { opacity: 0.3; cursor: not-allowed; }
}

/* Step 0 Styles */
.ritual-tabs {
  :deep(.el-tabs__nav-wrap::after) { display: none; }
  :deep(.el-tabs__active-bar) { background: var(--grad-mystic); height: 3px; border-radius: 2px; }
  :deep(.el-tabs__item) {
    color: #718096;
    font-family: var(--font-title);
    font-size: 18px;
    letter-spacing: 2px;
    height: 50px;
    &.is-active { color: #fff; }
  }
}

.textarea-wrapper {
  position: relative;
  margin-top: 30px;
  :deep(.el-textarea__inner) {
    background: rgba(0, 0, 0, 0.2) !important;
    border: 1px solid rgba(255, 255, 255, 0.05) !important;
    border-radius: 20px;
    padding: 25px;
    color: #e2e8f0;
    font-size: 16px;
    line-height: 1.8;
    resize: none;
    transition: all 0.3s;
    &:focus { border-color: var(--accent-mystic) !important; }
  }
  
  .glow-border {
    position: absolute;
    top: 0; left: 0; width: 100%; height: 100%;
    border-radius: 20px;
    border: 1px solid transparent;
    background: linear-gradient(135deg, var(--accent-mystic), transparent, var(--accent-quaternary)) border-box;
    -webkit-mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
    mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
    mask-composite: exclude;
    opacity: 0.2;
    pointer-events: none;
  }
}

.alchemy-trigger {
  margin-top: 40px;
  display: flex; 
  justify-content: center;
}

/* Step 1: Alchemy Animation */
.alchemy-pane {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 400px;
}

.alchemy-circle {
  position: relative;
  width: 120px;
  height: 120px;
  margin-bottom: 40px;
  
  .orbit {
    position: absolute;
    top: 0; left: 0; right: 0; bottom: 0;
    border: 2px solid var(--accent-mystic);
    border-radius: 50%;
    border-top-color: transparent;
    animation: spin 2s linear infinite;
    &:nth-child(2) {
      margin: 15px;
      border-color: var(--accent-quaternary);
      border-bottom-color: transparent;
      animation-duration: 1.5s;
      animation-direction: reverse;
    }
  }
  
  .core-eye {
    position: absolute;
    top: 50%; left: 50%;
    transform: translate(-50%, -50%);
    font-size: 32px;
    color: #fff;
    filter: drop-shadow(0 0 10px var(--accent-mystic));
  }
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.alchemy-title { font-family: var(--font-title); font-size: 24px; letter-spacing: 4px; margin-bottom: 15px; }
.alchemy-subtitle { color: #718096; font-size: 14px; letter-spacing: 2px; }

.chamber-fade-enter-active, .chamber-fade-leave-active { transition: all 0.5s ease; }
.chamber-fade-enter-from { opacity: 0; transform: translateY(30px); }
.chamber-fade-leave-to { opacity: 0; transform: translateY(-30px); }
</style>