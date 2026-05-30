<template>
  <div class="ai-index-view museum-fade-in">
    
    <!-- 页面标题区 -->
    <div class="ai-header">
      <div class="header-inner">
        <h1 class="ai-main-title">心灵回声系统</h1>
        <div class="header-divider"></div>
        <p class="ai-subtitle">探索内心深处的秘密花园，让人工智能成为你的心灵向导</p>
      </div>
    </div>

    <!-- 功能模块卡片网格 -->
    <div class="ai-modules-grid">
      
      <!-- 心灵对话亭 -->
      <div class="ai-module-card available" @click="navigateTo('/ai/dialogue')">
        <div class="card-glow"></div>
        <div class="card-icon">
          <span class="icon-emoji">💬</span>
        </div>
        <div class="card-content">
          <h3 class="card-title">心灵对话亭</h3>
          <p class="card-desc">与 AI 进行深度对话，探索内心的声音与情感的流动</p>
          <div class="card-status available">立即体验</div>
        </div>
        <div class="card-arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>

      <!-- 灵魂关键词 -->
      <div class="ai-module-card available" @click="navigateTo('/ai/keywords')">
        <div class="card-glow"></div>
        <div class="card-icon">
          <span class="icon-emoji">🔍</span>
        </div>
        <div class="card-content">
          <h3 class="card-title">灵魂关键词</h3>
          <p class="card-desc">解析记忆深处的密码，发现你内心最真实的关键词</p>
          <div class="card-status available">立即体验</div>
        </div>
        <div class="card-arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>

      <!-- 时光回声 -->
      <div class="ai-module-card available" @click="navigateTo('/ai/echo')">
        <div class="card-glow"></div>
        <div class="card-icon">
          <span class="icon-emoji">🌊</span>
        </div>
        <div class="card-content">
          <h3 class="card-title">时光回声</h3>
          <p class="card-desc">聆听过往岁月的回响，感受时间长河中的情感涟漪</p>
          <div class="card-status available">立即体验</div>
        </div>
        <div class="card-arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>

      <!-- 时光勋章 -->
      <div class="ai-module-card available" @click="navigateTo('/ai/badges')">
        <div class="card-glow"></div>
        <div class="card-icon">
          <span class="icon-emoji">🏅</span>
        </div>
        <div class="card-content">
          <h3 class="card-title">时光勋章</h3>
          <p class="card-desc">收集人生路上的成就徽章，见证每一个重要的里程碑</p>
          <div class="card-status available">立即体验</div>
        </div>
        <div class="card-arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>

      <!-- 情绪调色板 -->
      <div class="ai-module-card available" @click="navigateTo('/ai/palette')">
        <div class="card-glow"></div>
        <div class="card-icon">
          <span class="icon-emoji">🎨</span>
        </div>
        <div class="card-content">
          <h3 class="card-title">情绪调色板</h3>
          <p class="card-desc">用色彩描绘情感的变化，创造属于你的心情色谱图</p>
          <div class="card-status available">立即体验</div>
        </div>
        <div class="card-arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>

      <!-- 未来邮局 -->
      <div class="ai-module-card available" @click="navigateTo('/ai/future-mail')">
        <div class="card-glow"></div>
        <div class="card-icon">
          <span class="icon-emoji">✉️</span>
        </div>
        <div class="card-content">
          <h3 class="card-title">未来邮局</h3>
          <p class="card-desc">写一封信寄给未来的自己，让时光在约定的时刻为你开启</p>
          <div class="card-status available">立即体验</div>
        </div>
        <div class="card-arrow">
          <el-icon><ArrowRight /></el-icon>
        </div>
      </div>

    </div>

    <!-- 底部语录 -->
    <div class="ai-footer">
      <div class="footer-quote">
        <p class="quote-text">"你不必控制自己的想法，你只需要停止让它们控制你。"</p>
        <span class="quote-author">— Dan Millman</span>
      </div>
    </div>

  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const navigateTo = (path) => {
  router.push(path)
}
</script>

<style lang="scss" scoped>
/* ── 页面容器 ── */
.ai-index-view {
  padding: 40px;
  max-width: 1200px;
  margin: 0 auto;
}

/* ── 淡入动画 ── */
.museum-fade-in {
  animation: fadeInUp 0.5s ease both;
}
@keyframes fadeInUp {
  from { opacity: 0; transform: translateY(20px); }
  to   { opacity: 1; transform: translateY(0); }
}

/* ── 页面标题区 ── */
.ai-header {
  background: rgba(15, 18, 24, 0.4);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 8px;
  padding: 60px 50px;
  text-align: center;
  position: relative;
  margin-bottom: 50px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5),
              inset 0 0 40px rgba(157, 80, 187, 0.05);

  &::before, &::after {
    content: '🤖';
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    font-size: 24px;
    filter: drop-shadow(0 0 15px rgba(157, 80, 187, 0.6));
  }
  &::before { left: 40px; }
  &::after  { right: 40px; }
}

.ai-main-title {
  font-family: var(--font-title);
  font-size: 52px;
  letter-spacing: 16px;
  margin: 0 0 25px 0;
  background: var(--grad-mystic);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
  filter: drop-shadow(0 0 15px rgba(157, 80, 187, 0.4));
}

.header-divider {
  width: 300px;
  height: 1px;
  background: linear-gradient(to right, transparent, var(--accent-mystic), transparent);
  margin: 0 auto 25px;
  opacity: 0.6;
}

.ai-subtitle {
  font-family: var(--font-title);
  font-size: 15px;
  letter-spacing: 3px;
  color: #a0aec0;
  margin: 0;
  line-height: 1.6;
}

/* ── 模块卡片网格 ── */
.ai-modules-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 30px;
  margin-bottom: 60px;
}

/* ── 模块卡片 ── */
.ai-module-card {
  position: relative;
  background: rgba(15, 18, 24, 0.4);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 24px;
  padding: 32px 28px;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  display: flex;
  flex-direction: column;
  gap: 20px;
  min-height: 200px;

  &:hover {
    transform: translateY(-8px);
    border-color: rgba(157, 80, 187, 0.4);
    box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5),
                0 0 30px rgba(157, 80, 187, 0.15);
    
    .card-glow { opacity: 0.6; }
    .card-arrow { transform: translateX(8px); }
    .icon-emoji { transform: scale(1.1) rotate(5deg); }
  }

  &.available {
    border-color: rgba(157, 80, 187, 0.2);
    
    &:hover {
      border-color: var(--accent-mystic);
      box-shadow: 0 20px 50px rgba(0, 0, 0, 0.5),
                  0 0 40px rgba(157, 80, 187, 0.3);
    }
  }
}

/* 卡片光晕 */
.card-glow {
  position: absolute;
  bottom: -60px;
  right: -60px;
  width: 180px;
  height: 180px;
  background: radial-gradient(circle, var(--accent-mystic), transparent 70%);
  opacity: 0.2;
  filter: blur(40px);
  transition: opacity 0.5s;
  pointer-events: none;
}

/* 卡片图标 */
.card-icon {
  display: flex;
  justify-content: center;
  margin-bottom: 8px;
}

.icon-emoji {
  font-size: 48px;
  line-height: 1;
  display: block;
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  filter: drop-shadow(0 0 10px rgba(157, 80, 187, 0.3));
}

/* 卡片内容 */
.card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
  text-align: center;
}

.card-title {
  font-family: var(--font-title);
  font-size: 20px;
  letter-spacing: 3px;
  margin: 0;
  background: linear-gradient(to right, #fff, var(--accent-mystic));
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.card-desc {
  font-size: 14px;
  color: #718096;
  line-height: 1.7;
  margin: 0;
  flex: 1;
}

/* 卡片状态 */
.card-status {
  display: inline-block;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 12px;
  font-family: var(--font-title);
  letter-spacing: 1px;
  font-weight: 600;
  margin-top: 8px;

  &.available {
    background: rgba(44, 182, 125, 0.15);
    color: #2cb67d;
    border: 1px solid rgba(44, 182, 125, 0.3);
  }

  &.coming-soon {
    background: rgba(114, 117, 126, 0.15);
    color: #72757e;
    border: 1px solid rgba(114, 117, 126, 0.3);
  }
}

/* 卡片箭头 */
.card-arrow {
  position: absolute;
  top: 24px;
  right: 24px;
  color: rgba(157, 80, 187, 0.6);
  font-size: 18px;
  transition: all 0.3s;
}

/* ── 底部语录 ── */
.ai-footer {
  background: rgba(15, 18, 24, 0.3);
  backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 20px;
  padding: 40px;
}

.footer-quote {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  text-align: center;
}

.quote-text {
  font-family: var(--font-title);
  font-size: 18px;
  letter-spacing: 3px;
  line-height: 1.8;
  color: #e2e8f0;
  margin: 0;
  max-width: 600px;
  font-style: italic;
}

.quote-author {
  font-size: 13px;
  letter-spacing: 2px;
  color: var(--accent-mystic);
  font-family: var(--font-title);
}

/* ── 响应式 ── */
@media (max-width: 768px) {
  .ai-modules-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .ai-main-title {
    font-size: 36px;
    letter-spacing: 8px;
  }

  .quote-text {
    font-size: 15px;
    letter-spacing: 2px;
  }
}
</style>