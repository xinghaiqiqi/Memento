<template>
  <div class="dialogue-view museum-fade-in">

    <!-- 页面标题区 -->
    <div class="dialogue-header">
      <h1 class="dialogue-main-title">心灵对话亭</h1>
      <div class="header-divider"></div>
      <p class="dialogue-subtitle">在这里，倾诉你的心事，让 AI 成为聆听的回声</p>
    </div>

    <!-- 对话窗口 -->
    <div class="chat-window" ref="chatWindowRef">
      <!-- 空状态引导 -->
      <div v-if="messages.length === 0" class="chat-empty">
        <span class="empty-icon">🕯️</span>
        <p>这是一处只属于你的静谧角落，说出此刻的心绪吧。</p>
      </div>

      <!-- 消息气泡 -->
      <div
        v-for="(msg, index) in messages"
        :key="index"
        :class="['chat-row', msg.role === 'user' ? 'row-user' : 'row-ai']"
      >
        <div class="chat-avatar">{{ msg.role === 'user' ? '🧑' : '🌙' }}</div>
        <div :class="['chat-bubble', msg.role === 'user' ? 'bubble-user' : 'bubble-ai']">
          <p class="bubble-text">{{ msg.content }}</p>
          <!-- AI 回复后的保存记忆按钮 -->
          <button
            v-if="msg.role === 'ai' && msg.canSave"
            class="save-memory-btn"
            :disabled="msg.saving || msg.saved"
            @click="saveAsMemory(msg, index)"
          >
            <el-icon v-if="msg.saving"><Loading /></el-icon>
            <el-icon v-else-if="msg.saved"><CircleCheck /></el-icon>
            <el-icon v-else><Collection /></el-icon>
            {{ msg.saved ? '已封存为记忆' : (msg.saving ? '封存中...' : '保存为记忆') }}
          </button>
        </div>
      </div>

      <!-- AI 正在输入 -->
      <div v-if="sending" class="chat-row row-ai">
        <div class="chat-avatar">🌙</div>
        <div class="chat-bubble bubble-ai">
          <span class="typing-dots"><i></i><i></i><i></i></span>
        </div>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="chat-input-area">
      <el-input
        v-model="inputText"
        type="textarea"
        :rows="2"
        resize="none"
        placeholder="写下此刻的心情，按 Enter 发送..."
        @keydown.enter.exact.prevent="handleSend"
      />
      <div class="input-actions">
        <el-button @click="handleClear" :disabled="messages.length === 0 && !inputText">
          <el-icon><Delete /></el-icon> 清空
        </el-button>
        <el-button type="primary" :loading="sending" @click="handleSend">
          <el-icon><Promotion /></el-icon> 发送
        </el-button>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

// ─── 状态 ───────────────────────────────────────────────
const inputText = ref('')
const messages = ref([])   // { role: 'user' | 'ai', content, canSave?, userText?, sentimentScore?, saving?, saved? }
const sending = ref(false)
const chatWindowRef = ref(null)

// ─── 滚动到底部 ──────────────────────────────────────────
const scrollToBottom = () => {
  nextTick(() => {
    const el = chatWindowRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

// ─── 发送消息 ────────────────────────────────────────────
const handleSend = async () => {
  const text = inputText.value.trim()
  if (!text) {
    ElMessage.warning('请先输入内容')
    return
  }
  if (sending.value) return

  // 追加用户消息
  messages.value.push({ role: 'user', content: text })
  const userText = text
  inputText.value = ''
  scrollToBottom()

  sending.value = true
  try {
    const res = await axios.post('/api/ai-subsystem/dialogue', { message: userText })
    if (res.data && res.data.code === 200 && res.data.data) {
      const reply = res.data.data.reply || '（AI 沉默了片刻，没有给出回应）'
      messages.value.push({
        role: 'ai',
        content: reply,
        canSave: true,
        userText,
        saving: false,
        saved: false
      })
    } else {
      ElMessage.error(res.data?.message || 'AI 回复失败，请稍后重试')
    }
  } catch (e) {
    console.error('[Dialogue] AI 对话失败:', e)
    ElMessage.error('无法连接到 AI 对话服务，请确认后端已启动')
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

// ─── 清空对话 ────────────────────────────────────────────
const handleClear = () => {
  messages.value = []
  inputText.value = ''
}

// ─── 保存为记忆 ──────────────────────────────────────────
const saveAsMemory = async (msg, index) => {
  if (msg.saving || msg.saved) return
  messages.value[index].saving = true

  try {
    const userText = msg.userText || ''

    // 1) 调用我自己的 AI 子系统做结构化抽取（title / sentimentScore / tags）
    let title = userText.length > 18 ? userText.slice(0, 18) + '...' : (userText || '一段心灵对话')
    let sentimentScore = 0
    let tags = ''
    try {
      const extractRes = await axios.post('/api/ai-subsystem/extract', {
        userText,
        aiReply: msg.content
      })
      if (extractRes.data && extractRes.data.code === 200 && extractRes.data.data) {
        const d = extractRes.data.data
        title = d.title || title
        sentimentScore = d.sentimentScore ?? 0
        tags = d.tags || ''
      }
    } catch (err) {
      console.warn('[Dialogue] 结构化抽取失败，使用本地兜底标题', err)
    }

    // 2) 拼接内容并写入记忆
    const content = `【我说】${userText}\n\n【回声】${msg.content}`
    const payload = {
      title,
      content,
      eventDate: formatToday(),
      tags,
      sentimentScore
    }

    const res = await axios.post('/api/memories', payload)
    if (res.data && res.data.code === 200) {
      messages.value[index].saved = true
      ElMessage.success('这段对话已永恒封存为记忆')
    } else {
      ElMessage.error(res.data?.message || '保存失败')
    }
  } catch (e) {
    console.error('[Dialogue] 保存记忆失败:', e)
    ElMessage.error('保存记忆失败，请确认后端已启动')
  } finally {
    messages.value[index].saving = false
  }
}

// ─── 工具函数 ────────────────────────────────────────────
const formatToday = () => {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

onMounted(() => {
  scrollToBottom()
})
</script>

<style lang="scss" scoped>
/* ── 页面容器 ── */
.dialogue-view {
  padding: 40px;
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 70px);
  box-sizing: border-box;
}

/* ── 淡入动画 ── */
.museum-fade-in {
  animation: fadeInUp 0.5s ease both;
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ── 标题区 ── */
.dialogue-header {
  text-align: center;
  margin-bottom: 24px;
  flex-shrink: 0;
}

.dialogue-main-title {
  font-family: var(--font-title);
  font-size: 38px;
  letter-spacing: 12px;
  margin: 0 0 14px 0;
  background: var(--grad-mystic);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 0 12px rgba(157, 80, 187, 0.4));
}

.header-divider {
  width: 220px;
  height: 1px;
  background: linear-gradient(to right, transparent, var(--accent-mystic), transparent);
  margin: 0 auto 14px;
  opacity: 0.6;
}

.dialogue-subtitle {
  font-family: var(--font-title);
  font-size: 13px;
  letter-spacing: 3px;
  color: #a0aec0;
  margin: 0;
}

/* ── 对话窗口 ── */
.chat-window {
  flex: 1;
  overflow-y: auto;
  background: rgba(15, 18, 24, 0.4);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(157, 80, 187, 0.25);
  border-radius: 24px;
  padding: 28px;
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.3),
              inset 0 0 30px rgba(157, 80, 187, 0.03);
  display: flex;
  flex-direction: column;
  gap: 20px;
  margin-bottom: 20px;
}

/* 滚动条美化 */
.chat-window::-webkit-scrollbar { width: 6px; }
.chat-window::-webkit-scrollbar-thumb {
  background: rgba(157, 80, 187, 0.3);
  border-radius: 3px;
}

/* 空状态 */
.chat-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: #718096;
  font-family: var(--font-title);
  letter-spacing: 1px;

  .empty-icon { font-size: 48px; filter: drop-shadow(0 0 12px rgba(157, 80, 187, 0.4)); }
}

/* ── 消息行 ── */
.chat-row {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  max-width: 80%;
}

.row-user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.row-ai {
  align-self: flex-start;
}

.chat-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

/* ── 气泡 ── */
.chat-bubble {
  padding: 14px 18px;
  border-radius: 18px;
  position: relative;
}

.bubble-user {
  background: var(--grad-mystic);
  border-bottom-right-radius: 4px;
  box-shadow: 0 4px 16px rgba(157, 80, 187, 0.3);

  .bubble-text { color: #fff; }
}

.bubble-ai {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(157, 80, 187, 0.3);
  border-bottom-left-radius: 4px;
  box-shadow: 0 0 18px rgba(157, 80, 187, 0.12);

  .bubble-text { color: #e2e8f0; }
}

.bubble-text {
  margin: 0;
  line-height: 1.7;
  font-size: 14px;
  white-space: pre-wrap;
  word-break: break-word;
}

/* ── 保存记忆按钮 ── */
.save-memory-btn {
  margin-top: 12px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  font-size: 12px;
  font-family: var(--font-title);
  letter-spacing: 1px;
  color: var(--accent-mystic);
  background: rgba(157, 80, 187, 0.1);
  border: 1px solid rgba(157, 80, 187, 0.4);
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.3s;

  &:hover:not(:disabled) {
    background: rgba(157, 80, 187, 0.2);
    color: #fff;
    box-shadow: 0 0 12px rgba(157, 80, 187, 0.4);
  }
  &:disabled { cursor: default; opacity: 0.7; }
}

/* ── 正在输入动画 ── */
.typing-dots {
  display: inline-flex;
  gap: 5px;
  align-items: center;
  height: 20px;

  i {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    background: var(--accent-mystic);
    animation: typing 1.2s infinite ease-in-out both;
  }
  i:nth-child(2) { animation-delay: 0.2s; }
  i:nth-child(3) { animation-delay: 0.4s; }
}
@keyframes typing {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* ── 输入区 ── */
.chat-input-area {
  flex-shrink: 0;
  background: rgba(15, 18, 24, 0.4);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 20px;
  padding: 16px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 12px;
}
</style>
