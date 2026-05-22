<template>
  <el-config-provider :locale="zhCn">
    <div class="app-wrapper">
      <div class="galaxy-background">
        <div id="stars"></div>
        <div id="stars2"></div>
        <div id="stars3"></div>
      </div>
      <el-container v-if="!isFullScreen">
        <el-aside :width="isCollapse ? '80px' : '280px'" class="aside-container">
          <div class="logo">
            <span v-if="!isCollapse">MEMENTO</span>
            <span v-else>M</span>
          </div>
          <el-menu
            :default-active="activeMenu"
            class="museum-menu"
            :collapse="isCollapse"
            router
          >
            <el-menu-item index="/">
              <el-icon :size="20"><Monitor /></el-icon>
              <template #title><span class="menu-text">博物馆概览</span></template>
            </el-menu-item>
            <el-menu-item index="/memories">
              <el-icon :size="20"><Collection /></el-icon>
              <template #title><span class="menu-text">记忆陈列室</span></template>
            </el-menu-item>
            <el-menu-item index="/import">
              <el-icon :size="20"><MagicStick /></el-icon>
              <template #title><span class="menu-text">记忆实验室</span></template>
            </el-menu-item>
            <el-menu-item index="/gallery">
              <el-icon :size="20"><Picture /></el-icon>
              <template #title><span class="menu-text">时光画廊</span></template>
            </el-menu-item>
            <el-menu-item index="/timeline">
              <el-icon :size="20"><Calendar /></el-icon>
              <template #title><span class="menu-text">时间长廊</span></template>
            </el-menu-item>
            <el-menu-item index="/clusters">
              <el-icon :size="20"><Connection /></el-icon>
              <template #title><span class="menu-text">记忆星团</span></template>
            </el-menu-item>
            <el-menu-item index="/narrative">
              <el-icon :size="20"><Notebook /></el-icon>
              <template #title><span class="menu-text">叙事编织</span></template>
            </el-menu-item>
            <el-menu-item index="/sentiment">
              <el-icon :size="20"><Histogram /></el-icon>
              <template #title><span class="menu-text">情感光谱</span></template>
            </el-menu-item>
            <el-menu-item index="/export">
              <el-icon :size="20"><Download /></el-icon>
              <template #title><span class="menu-text">永恒归档</span></template>
            </el-menu-item>
          </el-menu>
        </el-aside>
        <el-container class="main-container">
          <el-header class="header-container">
            <div class="header-left">
              <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
                <Expand v-if="isCollapse" />
                <Fold v-else />
              </el-icon>
              <h2 class="current-page-title">{{ currentPageTitle }}</h2>
            </div>
            <div class="header-right">
              <el-avatar size="small" :src="userStore.userInfo.avatar" />
              <span class="nickname">{{ userStore.userInfo.nickname }}</span>
            </div>
          </el-header>
          <el-main>
            <router-view v-slot="{ Component }">
              <transition name="fade-transform" mode="out-in">
                <component :is="Component" />
              </transition>
            </router-view>
          </el-main>
        </el-container>
      </el-container>
      <router-view v-else />
    </div>
  </el-config-provider>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()

const isCollapse = ref(false)
const activeMenu = computed(() => route.path)
const isFullScreen = computed(() => route.meta.fullScreen)

const currentPageTitle = computed(() => {
  const menuMap = {
    '/': '博物馆概览',
    '/memories': '记忆陈列室',
    '/import': '记忆实验室',
    '/gallery': '时光画廊',
    '/timeline': '时间长廊',
    '/clusters': '记忆星团',
    '/narrative': '叙事编织',
    '/sentiment': '情感光谱',
    '/export': '永恒归档'
  }
  return menuMap[route.path] || ''
})
</script>

<style lang="scss">
@use "sass:math";
@use "sass:string";

@function multiple-box-shadow ($n) {
  $value: '#{math.random(2000)}px #{math.random(2000)}px #FFF';
  @for $i from 2 through $n {
    $value: '#{$value} , #{math.random(2000)}px #{math.random(2000)}px #FFF';
  }
  @return string.unquote($value);
}

$shadows-small:  multiple-box-shadow(700);
$shadows-medium: multiple-box-shadow(200);
$shadows-big:    multiple-box-shadow(100);

.galaxy-background {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: radial-gradient(ellipse at bottom, #1B2735 0%, #090A0F 100%);
  overflow: hidden;
  z-index: -1;
}

#stars {
  width: 1px;
  height: 1px;
  background: transparent;
  box-shadow: $shadows-small;
  animation: animStar 50s linear infinite;
  &:after {
    content: " ";
    position: absolute;
    top: 2000px;
    width: 1px;
    height: 1px;
    background: transparent;
    box-shadow: $shadows-small;
  }
}

#stars2 {
  width: 2px;
  height: 2px;
  background: transparent;
  box-shadow: $shadows-medium;
  animation: animStar 100s linear infinite;
  &:after {
    content: " ";
    position: absolute;
    top: 2000px;
    width: 2px;
    height: 2px;
    background: transparent;
    box-shadow: $shadows-medium;
  }
}

#stars3 {
  width: 3px;
  height: 3px;
  background: transparent;
  box-shadow: $shadows-big;
  animation: animStar 150s linear infinite;
  &:after {
    content: " ";
    position: absolute;
    top: 2000px;
    width: 3px;
    height: 3px;
    background: transparent;
    box-shadow: $shadows-big;
  }
}

@keyframes animStar {
  from { transform: translateY(0px); }
  to { transform: translateY(-2000px); }
}

.app-wrapper {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
}

.aside-container {
  background: #05070a !important; /* 改为纯粹的极暗色背景，彻底隔绝背景星星的干扰 */
  border-right: 1px solid rgba(255, 255, 255, 0.1) !important;
  transition: width 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  height: 100vh;
  z-index: 100;
  box-shadow: 10px 0 30px rgba(0, 0, 0, 0.5); /* 增加向右的投影，使侧边栏像悬浮在星空之上 */
  
  .logo {
    height: 120px;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    font-size: 28px;
    font-family: var(--font-title);
    letter-spacing: 10px;
    /* 还原为紫色梦幻渐变 */
    background: linear-gradient(to bottom, #ffffff, var(--accent-primary));
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    filter: drop-shadow(0 0 15px rgba(127, 90, 240, 0.4));
    font-weight: 700;
  }

  .museum-menu {
    border-right: none;
    background-color: transparent;
    padding: 0 15px;
    
    .el-menu-item {
      height: 56px !important;
      line-height: 56px !important;
      margin-bottom: 12px !important;
      border-radius: 8px !important;
      position: relative;
      transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1) !important;
      background: rgba(255, 255, 255, 0.05) !important;
      border: 1px solid rgba(255, 255, 255, 0.1) !important;
      
      /* 默认态：月光金/银白色，确保极高能见度 */
      .el-icon { 
        color: #e2e8f0 !important; 
        font-size: 18px !important;
        margin-right: 14px !important; 
        filter: drop-shadow(0 0 5px rgba(255, 255, 255, 0.1));
      }
      
      .menu-text {
        font-family: var(--font-title) !important;
        font-size: 14px !important;
        letter-spacing: 2px !important;
        font-weight: 600 !important;
        color: #e2e8f0 !important; /* 银白色，非常清晰 */
        transition: all 0.3s;
      }
      
      /* 悬停与选中：淡淡的金色光泽，与紫色 Logo 呼应 */
      &:hover, &.is-active {
        background: rgba(255, 255, 255, 0.1) !important;
        border-color: var(--accent-primary) !important; /* 边框用紫色呼应 Logo */
        box-shadow: 0 0 20px rgba(0,0,0,0.4) !important;
        
        .el-icon {
          color: #fff !important;
          filter: drop-shadow(0 0 8px var(--accent-primary));
          transform: scale(1.1);
        }
        
        .menu-text {
          color: #fff !important;
          font-weight: 700 !important;
          text-shadow: 0 0 8px rgba(255, 255, 255, 0.5) !important;
        }
      }

      /* 选中态额外装饰：左侧紫金丝 */
      &.is-active {
        &::before {
          content: '';
          position: absolute;
          left: 0;
          top: 15%;
          height: 70%;
          width: 3px;
          background: linear-gradient(to bottom, #fff, var(--accent-primary));
          box-shadow: 0 0 10px var(--accent-primary);
          border-radius: 0 4px 4px 0;
        }
      }
    }
  }
}

.main-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: transparent;
}

.header-container {
  background: rgba(0, 0, 0, 0.2) !important;
  backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 40px;
  height: 70px !important;

  .header-left {
    display: flex;
    align-items: center;
    gap: 30px;
    
    .collapse-btn {
      font-size: 22px;
      cursor: pointer;
      color: #94a1b2;
      &:hover { color: #fff; }
    }

    .current-page-title {
      font-family: var(--font-title);
      font-size: 22px;
      color: #ffffff;
      margin: 0;
      letter-spacing: 4px;
      text-shadow: 0 0 15px rgba(127, 90, 240, 0.4);
      background: linear-gradient(to right, #fff, #94a1b2);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
  }

  .header-right {
    display: flex;
    align-items: center;
    gap: 15px;
    .nickname {
      font-size: 14px;
      color: #fff;
      font-family: var(--font-title);
      letter-spacing: 1px;
    }
    :deep(.el-avatar) {
      border: 1px solid var(--accent-primary);
      box-shadow: 0 0 10px rgba(127, 90, 240, 0.3);
    }
  }
}

.el-main {
  padding: 0;
  overflow-y: auto;
  overflow-x: hidden;
}

/* transition */
.fade-transform-leave-active,
.fade-transform-enter-active {
  transition: all 0.3s;
}

.fade-transform-enter-from {
  opacity: 0;
  transform: translateX(-30px);
}

.fade-transform-leave-to {
  opacity: 0;
  transform: translateX(30px);
}
</style>
