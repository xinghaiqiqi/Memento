<template>
  <div class="mail-view museum-fade-in">

    <!-- 页面标题区 -->
    <div class="mail-header">
      <h1 class="mail-main-title">未来邮局</h1>
      <div class="header-divider"></div>
      <p class="mail-subtitle">写一封信，寄给某个时刻的自己</p>
    </div>

    <!-- 写信区 -->
    <div class="mail-compose">
      <div class="compose-header">
        <el-icon><EditPen /></el-icon>
        <span>写一封未来的信</span>
      </div>

      <el-input
        v-model="form.title"
        placeholder="信件标题（留空将自动生成）"
        maxlength="40"
        class="compose-title"
      />

      <el-input
        v-model="form.content"
        type="textarea"
        :rows="5"
        resize="none"
        placeholder="写下此刻想对未来的自己说的话..."
        class="compose-content"
      />

      <div class="compose-footer">
        <div class="compose-date">
          <span class="date-label">送达时间</span>
          <el-date-picker
            v-model="form.targetDate"
            type="datetime"
            placeholder="选择信件解封的时刻"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
            :disabled-date="disabledPastDate"
          />
        </div>
        <el-button type="primary" :loading="sending" @click="sendLetter">
          <el-icon><Promotion /></el-icon> 寄出信件
        </el-button>
      </div>
    </div>

    <!-- 加载状态 -->
    <div v-if="loading" class="mail-loading">
      <el-icon class="loading-icon"><Loading /></el-icon>
      <span>正在打开邮局...</span>
    </div>

    <!-- 错误状态 -->
    <div v-else-if="error" class="mail-error">
      <el-icon><WarningFilled /></el-icon>
      <span>{{ error }}</span>
      <el-button size="small" @click="fetchLetters">重新加载</el-button>
    </div>

    <template v-else>

      <!-- 统计 -->
      <div class="mail-stats">
        <div class="stat-pill">
          <span class="stat-num">{{ letters.length }}</span>
          <span class="stat-label">寄出的信</span>
        </div>
        <div class="stat-pill">
          <span class="stat-num locked-num">{{ lockedLetters.length }}</span>
          <span class="stat-label">封存中</span>
        </div>
        <div class="stat-pill">
          <span class="stat-num unlocked-num">{{ unlockedLetters.length }}</span>
          <span class="stat-label">已抵达</span>
        </div>
      </div>

      <!-- 已解锁信件 -->
      <div v-if="unlockedLetters.length" class="mail-section">
        <h3 class="section-title">📬 已抵达的信</h3>
        <div class="mail-grid">
          <div
            v-for="letter in unlockedLetters"
            :key="letter.id"
            class="letter-card unlocked"
          >
            <div class="letter-glow"></div>
            <div class="letter-top">
              <span class="letter-stamp arrived">时间已抵达</span>
              <el-icon class="letter-del" @click="removeLetter(letter.id)"><Delete /></el-icon>
            </div>
            <h4 class="letter-title">{{ letter.title }}</h4>
            <p class="letter-content">{{ letter.content }}</p>
            <p v-if="letter.echo" class="letter-echo">{{ letter.echo }}</p>
            <div class="letter-meta">
              <span>寄于 {{ formatDisplay(letter.createdAt) }}</span>
              <span>抵于 {{ formatDisplay(letter.targetDate) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 未解锁信件 -->
      <div v-if="lockedLetters.length" class="mail-section">
        <h3 class="section-title">🔒 封存中的信</h3>
        <div class="mail-grid">
          <div
            v-for="letter in lockedLetters"
            :key="letter.id"
            class="letter-card locked"
          >
            <div class="letter-top">
              <span class="letter-stamp sealed">封存中</span>
              <el-icon class="letter-del" @click="removeLetter(letter.id)"><Delete /></el-icon>
            </div>
            <div class="lock-icon">🔒</div>
            <h4 class="letter-title sealed-title">{{ letter.title }}</h4>
            <p class="letter-locked-hint">这封信将于下方时刻为你开启</p>
            <div class="letter-countdown">{{ countdownText(letter.targetDate) }}</div>
            <div class="letter-meta">
              <span>寄于 {{ formatDisplay(letter.createdAt) }}</span>
              <span>解封 {{ formatDisplay(letter.targetDate) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="letters.length === 0" class="mail-empty">
        <span class="empty-icon">✉️</span>
        <p>还没有寄出任何信件，写下第一封寄给未来的信吧</p>
      </div>

    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

// ─── 状态 ───────────────────────────────────────────────
const letters = ref([])
const loading = ref(false)
const error = ref(null)
const sending = ref(false)
const now = ref(Date.now())
let timer = null

const form = ref({
  title: '',
  content: '',
  targetDate: ''
})

// ─── 获取信件列表 ────────────────────────────────────────
const fetchLetters = async () => {
  loading.value = true
  error.value = null
  try {
    const res = await axios.get('/api/ai-subsystem/letters')
    if (res.data && res.data.code === 200) {
      letters.value = res.data.data || []
    } else {
      error.value = '获取信件失败，请稍后重试'
    }
  } catch (e) {
    console.error('[FutureMail] 获取信件失败:', e)
    error.value = '无法连接到邮局服务，请确认后端已启动'
  } finally {
    loading.value = false
  }
}

// ─── 解锁判断（前端实时判断，与后端 status 字段一致） ────
const isUnlocked = (letter) => {
  return letter.status === 'unlocked'
}

const unlockedLetters = computed(() =>
  letters.value
    .filter(isUnlocked)
    .sort((a, b) => new Date(b.targetDate) - new Date(a.targetDate))
)

const lockedLetters = computed(() =>
  letters.value
    .filter(l => !isUnlocked(l))
    .sort((a, b) => new Date(a.targetDate) - new Date(b.targetDate))
)

// ─── 写信 ────────────────────────────────────────────────
const disabledPastDate = (date) => {
  return date.getTime() < Date.now() - 24 * 60 * 60 * 1000
}

const sendLetter = async () => {
  if (!form.value.content.trim()) {
    ElMessage.warning('信的内容不能为空')
    return
  }
  if (!form.value.targetDate) {
    ElMessage.warning('请选择信件的送达时间')
    return
  }
  const targetTime = new Date(form.value.targetDate).getTime()
  if (targetTime <= Date.now()) {
    ElMessage.warning('送达时间必须晚于现在，未来的信才有意义')
    return
  }

  const title = form.value.title.trim() || autoTitle(form.value.content)
  const echo = buildEcho(form.value.content)

  sending.value = true
  try {
    const res = await axios.post('/api/ai-subsystem/letters', {
      title,
      content: form.value.content.trim(),
      targetDate: form.value.targetDate,
      echo
    })
    if (res.data && res.data.code === 200) {
      ElMessage.success('信件已寄出，静待时光将它送达')
      form.value = { title: '', content: '', targetDate: '' }
      await fetchLetters()
    } else {
      ElMessage.error(res.data?.message || '寄出失败，请稍后重试')
    }
  } catch (e) {
    console.error('[FutureMail] 寄出信件失败:', e)
    ElMessage.error('无法连接到邮局服务，请确认后端已启动')
  } finally {
    sending.value = false
  }
}

// ─── 撤回信件 ────────────────────────────────────────────
const removeLetter = async (id) => {
  try {
    const res = await axios.delete(`/api/ai-subsystem/letters/${id}`)
    if (res.data && res.data.code === 200) {
      ElMessage.success('信件已撤回')
      letters.value = letters.value.filter(l => l.id !== id)
    } else {
      ElMessage.error(res.data?.message || '撤回失败')
    }
  } catch (e) {
    console.error('[FutureMail] 撤回失败:', e)
    ElMessage.error('撤回失败，请稍后重试')
  }
}

// ─── AI 寄语（前端生成，保底可运行） ─────────────────────
const buildEcho = (content) => {
  const len = content.trim().length
  let mood = '平静而真挚'
  if (/开心|快乐|高兴|期待|幸福|爱/.test(content)) mood = '满怀希望与温暖'
  else if (/难过|焦虑|害怕|担心|失落|痛/.test(content)) mood = '带着一丝不安与脆弱'
  return `这封写给未来的信，承载了你当时${mood}的情绪状态，共 ${len} 字的心绪，正穿越时光等待与你重逢。`
}

// ─── 工具函数 ────────────────────────────────────────────
const autoTitle = (content) => {
  const t = content.trim()
  return t.length > 16 ? t.slice(0, 16) + '...' : t
}

const formatDisplay = (raw) => {
  if (!raw) return '未知'
  return String(raw).slice(0, 16)
}

const countdownText = (targetDate) => {
  const diff = new Date(targetDate).getTime() - now.value
  if (diff <= 0) return '即将解封'
  const days = Math.floor(diff / 86400000)
  const hours = Math.floor((diff % 86400000) / 3600000)
  const mins = Math.floor((diff % 3600000) / 60000)
  if (days > 0) return `还有 ${days} 天 ${hours} 小时解封`
  if (hours > 0) return `还有 ${hours} 小时 ${mins} 分钟解封`
  return `还有 ${mins} 分钟解封`
}

// ─── 生命周期 ────────────────────────────────────────────
onMounted(() => {
  fetchLetters()
  // 每 30 秒刷新时间，驱动倒计时更新
  timer = setInterval(() => { now.value = Date.now() }, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss" scoped>
.mail-view {
  padding: 40px;
  max-width: 1200px;
  margin: 0 auto;
}

.museum-fade-in {
  animation: fadeInUp 0.5s ease both;
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ── 标题区 ── */
.mail-header {
  text-align: center;
  margin-bottom: 36px;
}

.mail-main-title {
  font-family: var(--font-title);
  font-size: 46px;
  letter-spacing: 14px;
  margin: 0 0 18px 0;
  background: var(--grad-mystic);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 0 12px rgba(157, 80, 187, 0.4));
}

.header-divider {
  width: 240px;
  height: 1px;
  background: linear-gradient(to right, transparent, var(--accent-mystic), transparent);
  margin: 0 auto 16px;
  opacity: 0.6;
}

.mail-subtitle {
  font-family: var(--font-title);
  font-size: 14px;
  letter-spacing: 3px;
  color: #a0aec0;
  margin: 0;
}

/* ── 写信区 ── */
.mail-compose {
  background: rgba(15, 18, 24, 0.4);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(157, 80, 187, 0.25);
  border-radius: 24px;
  padding: 28px;
  margin-bottom: 36px;
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.3),
              inset 0 0 30px rgba(157, 80, 187, 0.03);
}

.compose-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  color: var(--accent-mystic);
  font-family: var(--font-title);
  font-size: 16px;
  letter-spacing: 2px;
}

.compose-title  { margin-bottom: 16px; }
.compose-content { margin-bottom: 20px; }

.compose-footer {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 20px;
  flex-wrap: wrap;
}

.compose-date {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.date-label {
  font-size: 12px;
  letter-spacing: 2px;
  color: #718096;
  font-family: var(--font-title);
}

/* ── 加载 / 错误 ── */
.mail-loading,
.mail-error {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px 0;
  color: #718096;
  font-family: var(--font-title);
  letter-spacing: 2px;
  font-size: 15px;
}

.loading-icon {
  font-size: 22px;
  animation: spin 1.2s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}

/* ── 统计 ── */
.mail-stats {
  display: flex;
  gap: 20px;
  margin-bottom: 36px;
  flex-wrap: wrap;
}

.stat-pill {
  flex: 1;
  min-width: 120px;
  background: rgba(15, 18, 24, 0.4);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  transition: border-color 0.3s;
  &:hover { border-color: rgba(157, 80, 187, 0.4); }
}

.stat-num {
  font-family: var(--font-title);
  font-size: 32px;
  font-weight: 700;
  background: linear-gradient(to bottom, #fff, #cbd5e0);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  &.locked-num   { color: #72757e; -webkit-text-fill-color: #72757e; }
  &.unlocked-num { color: var(--accent-mystic); -webkit-text-fill-color: var(--accent-mystic); }
}

.stat-label {
  font-size: 12px;
  letter-spacing: 2px;
  color: #718096;
  font-family: var(--font-title);
}

/* ── 分区 ── */
.mail-section { margin-bottom: 40px; }

.section-title {
  font-family: var(--font-title);
  font-size: 20px;
  letter-spacing: 3px;
  color: #e2e8f0;
  margin: 0 0 20px 0;
}

/* ── 信件网格 ── */
.mail-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

/* ── 信件卡片 ── */
.letter-card {
  position: relative;
  border-radius: 20px;
  padding: 24px;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.letter-card.unlocked {
  background: rgba(15, 18, 24, 0.45);
  border: 1px solid rgba(157, 80, 187, 0.4);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.4);
  &:hover {
    transform: translateY(-6px);
    border-color: var(--accent-mystic);
    box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5), 0 0 28px rgba(157, 80, 187, 0.25);
    .letter-glow { opacity: 0.7; }
  }
}

.letter-card.locked {
  background: rgba(255, 255, 255, 0.02);
  border: 1px dashed rgba(255, 255, 255, 0.15);
  text-align: center;
  &:hover { border-color: rgba(157, 80, 187, 0.3); }
}

.letter-glow {
  position: absolute;
  bottom: -50px; right: -50px;
  width: 160px; height: 160px;
  background: radial-gradient(circle, var(--accent-mystic), transparent 70%);
  opacity: 0.25;
  filter: blur(40px);
  transition: opacity 0.5s;
  pointer-events: none;
}

.letter-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.letter-stamp {
  font-size: 11px;
  font-family: var(--font-title);
  letter-spacing: 1px;
  padding: 4px 12px;
  border-radius: 16px;
  &.arrived {
    color: var(--accent-mystic);
    background: rgba(157, 80, 187, 0.15);
    border: 1px solid rgba(157, 80, 187, 0.4);
  }
  &.sealed {
    color: #72757e;
    background: rgba(114, 117, 126, 0.12);
    border: 1px solid rgba(114, 117, 126, 0.3);
  }
}

.letter-del {
  color: #4a5568;
  cursor: pointer;
  transition: color 0.3s;
  &:hover { color: var(--accent-tertiary); }
}

.lock-icon {
  font-size: 40px;
  margin: 8px 0;
  animation: lockPulse 2.4s ease-in-out infinite;
}
@keyframes lockPulse {
  0%, 100% { opacity: 0.6; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.08); }
}

.letter-title {
  font-family: var(--font-title);
  font-size: 18px;
  letter-spacing: 1px;
  margin: 0;
  background: linear-gradient(to right, #fff, var(--accent-mystic));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  &.sealed-title {
    background: none;
    -webkit-text-fill-color: #718096;
    color: #718096;
  }
}

.letter-content {
  font-size: 14px;
  color: #cbd5e0;
  line-height: 1.7;
  margin: 0;
  white-space: pre-wrap;
  word-break: break-word;
}

.letter-echo {
  font-size: 12px;
  color: var(--accent-mystic);
  font-style: italic;
  line-height: 1.6;
  margin: 0;
  padding: 10px 12px;
  background: rgba(157, 80, 187, 0.06);
  border-left: 2px solid var(--accent-mystic);
  border-radius: 4px;
}

.letter-locked-hint {
  font-size: 12px;
  color: #718096;
  margin: 0;
}

.letter-countdown {
  font-family: var(--font-title);
  font-size: 14px;
  color: var(--accent-mystic);
  letter-spacing: 1px;
  margin: 4px 0;
}

.letter-meta {
  display: flex;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 6px;
  font-size: 11px;
  color: #718096;
  letter-spacing: 1px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  padding-top: 10px;
  margin-top: auto;
}

/* ── 空状态 ── */
.mail-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  padding: 60px 0;
  color: #718096;
  font-family: var(--font-title);
  letter-spacing: 1px;
  .empty-icon { font-size: 48px; filter: drop-shadow(0 0 12px rgba(157, 80, 187, 0.4)); }
}
</style>
