<template>
  <div class="import-container museum-fade-in">
    <div class="museum-header">
      <h1 class="museum-title">Memory Laboratory</h1>
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
              <button class="ritual-button" @click="startExtraction" :disabled="!canExtract">
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
            </div>
            
            <div class="crystal-list">
              <el-table :data="previewData" class="museum-table">
                <el-table-column prop="eventDate" label="时间轴" width="150" />
                <el-table-column prop="title" label="记忆之名" width="220" />
                <el-table-column prop="content" label="叙事残片" show-overflow-tooltip />
                <el-table-column label="操作" width="100">
                  <template #default="{$index}">
                    <el-button link class="delete-link" @click="previewData.splice($index, 1)">遗忘</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>

            <div class="final-actions">
              <el-button plain class="back-ritual" @click="activeStep = 0">重塑仪式</el-button>
              <button class="ritual-button primary" @click="handleConfirmImport" :loading="confirming">
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
    previewData.value = res.data
    setTimeout(() => { activeStep.value = 2 }, 1500)
  } catch (error) {
    ElMessage.error('萃取失败，请检查星象连接')
    activeStep.value = 0
  }
}

const handleConfirmImport = async () => {
  confirming.value = true
  try {
    await axios.post('/api/import/confirm', previewData.value)
    ElMessage.success('记忆已永恒留存')
    router.push('/memories')
  } catch (error) {
    ElMessage.error('存入失败')
  } finally {
    confirming.value = false
  }
}
</script>

<style lang="scss" scoped>
.import-container {
  padding: 60px 40px;
  max-width: 1200px;
  margin: 0 auto;
}

.museum-header {
  text-align: center;
  margin-bottom: 60px;
  .museum-title {
    font-size: 42px;
    margin: 0;
    background: linear-gradient(to bottom, #fff, #94a1b2);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
  }
  .museum-subtitle {
    color: var(--accent-primary);
    letter-spacing: 4px;
    font-size: 14px;
    margin-top: 10px;
  }
}

.artistic-steps {
  display: flex;
  justify-content: center;
  gap: 100px;
  margin-bottom: 50px;
  .step-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 12px;
    opacity: 0.5; /* 提高基础不透明度 */
    transition: all 0.5s cubic-bezier(0.4, 0, 0.2, 1);
    cursor: default;
    
    &.active { 
      opacity: 1; 
      .step-dot { transform: scale(1.8); background: #fff; box-shadow: 0 0 20px var(--accent-primary), 0 0 40px rgba(127, 90, 240, 0.3); } 
      .step-label { color: #fff; font-weight: 600; text-shadow: 0 0 10px rgba(127, 90, 240, 0.5); }
    }
    
    &.completed { 
      opacity: 0.85; 
      .step-dot { background: var(--accent-secondary); box-shadow: 0 0 15px rgba(44, 182, 125, 0.3); } 
      .step-label { color: var(--accent-secondary); }
    }
    
    .step-dot {
      width: 10px;
      height: 10px;
      background: rgba(255, 255, 255, 0.3);
      border-radius: 50%;
      transition: all 0.4s;
    }
    
    .step-label { 
      font-size: 14px; /* 稍微增大字号 */
      letter-spacing: 2px; 
      font-family: var(--font-title);
      color: #94a1b2;
    }
  }
}

.glass-chamber {
  min-height: 500px;
  padding: 40px;
  background: rgba(255, 255, 255, 0.02);
  backdrop-filter: blur(30px);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 40px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.3);
}

.ritual-pane {
  .textarea-wrapper {
    position: relative;
    padding: 2px;
    border-radius: 20px;
    background: rgba(255, 255, 255, 0.05);
    .glow-border {
      position: absolute;
      top: 0; left: 0; width: 100%; height: 100%;
      border-radius: 20px;
      border: 1px solid transparent;
      background: linear-gradient(135deg, var(--accent-primary), transparent, var(--accent-secondary)) border-box;
      -webkit-mask: linear-gradient(#fff 0 0) padding-box, linear-gradient(#fff 0 0);
      mask-composite: exclude;
      opacity: 0.3;
    }
  }
}

.ritual-tabs {
  :deep(.el-tabs__header) {
    display: flex;
    justify-content: center;
    border: none;
    margin-bottom: 30px;
  }
  :deep(.el-tabs__item) {
    color: #94a1b2;
    font-family: var(--font-title);
    font-size: 16px;
    &.is-active { color: #fff; }
  }
  :deep(.el-tabs__active-bar) { background: var(--accent-primary); height: 1px; }
}

.shrine-upload {
  :deep(.el-upload-dragger) {
    background: transparent;
    border: 1px dashed rgba(255, 255, 255, 0.1);
    height: 300px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    border-radius: 20px;
    &:hover { border-color: var(--accent-primary); }
  }
  .upload-icon-aura {
    font-size: 60px;
    color: var(--accent-primary);
    filter: drop-shadow(0 0 10px rgba(127, 90, 240, 0.5));
    margin-bottom: 20px;
  }
}

.alchemy-trigger {
  margin-top: 40px;
  text-align: center;
}

.ritual-button {
  position: relative;
  background: transparent;
  border: 1px solid var(--accent-primary);
  color: #fff;
  padding: 15px 40px;
  border-radius: 50px;
  font-family: var(--font-title);
  font-size: 16px;
  letter-spacing: 3px;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.3s;
  
  &:disabled { opacity: 0.3; cursor: not-allowed; }
  &:hover:not(:disabled) {
    transform: scale(1.05);
    box-shadow: 0 0 20px rgba(127, 90, 240, 0.4);
  }

  &.primary {
    background: var(--accent-primary);
    border: none;
  }
}

/* 炼金术动效 */
.alchemy-pane {
  text-align: center;
  padding-top: 60px;
  .alchemy-circle {
    position: relative;
    width: 200px;
    height: 200px;
    margin: 0 auto 40px;
    .orbit {
      position: absolute;
      top: 0; left: 0; width: 100%; height: 100%;
      border: 1px solid rgba(127, 90, 240, 0.2);
      border-radius: 50%;
      animation: rotate 10s linear infinite;
      &:nth-child(2) { border-style: dashed; animation-duration: 15s; transform: rotateX(60deg); }
    }
    .core-eye {
      position: absolute;
      top: 50%; left: 50%; transform: translate(-50%, -50%);
      font-size: 40px;
      color: var(--accent-primary);
    }
  }
}

@keyframes rotate { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }

.chamber-fade-enter-active, .chamber-fade-leave-active { transition: all 0.6s cubic-bezier(0.4, 0, 0.2, 1); }
.chamber-fade-enter-from { opacity: 0; transform: translateY(20px); }
.chamber-fade-leave-to { opacity: 0; transform: translateY(-20px); }
</style>
