<template>
  <div class="profile-view museum-fade-in">
    <!-- 页面标题区 -->
    <div class="profile-header">
      <div class="header-inner">
        <h1 class="profile-main-title">个人主页</h1>
        <div class="header-divider"></div>
        <p class="profile-subtitle">管理你的记忆通行证，查看成长的每一个勋章</p>
      </div>
    </div>

    <el-row :gutter="40">
      <!-- 左侧：个人信息管理 -->
      <el-col :span="9">
        <div class="museum-exhibit-card profile-card">
          <div class="card-header">
            <span class="header-title">馆长资料</span>
            <div class="header-dot"></div>
          </div>
          
          <div class="avatar-upload-section">
            <div class="avatar-wrap">
              <el-avatar :size="120" :src="userStore.userInfo.avatar" />
              <div class="avatar-edit-overlay">
                <el-icon><Camera /></el-icon>
              </div>
            </div>
            <p class="upload-hint">点击更换头像</p>
          </div>

          <el-form :model="userStore.userInfo" label-position="top" class="profile-form">
            <el-form-item label="馆长昵称">
              <el-input v-model="userStore.userInfo.nickname" placeholder="输入你的独特称呼" />
            </el-form-item>
            <el-form-item label="个性签名">
              <el-input 
                v-model="userStore.userInfo.signature" 
                type="textarea" 
                :rows="3" 
                placeholder="在这座博物馆里，你最想留下什么？" 
              />
            </el-form-item>
            <div class="form-actions">
              <el-button type="primary" class="save-btn" @click="handleSave">保存修改</el-button>
            </div>
          </el-form>
        </div>
      </el-col>

      <!-- 右侧：时光勋章展示 -->
      <el-col :span="15">
        <div class="museum-exhibit-card badges-card">
          <div class="card-header">
            <span class="header-title">时光勋章墙</span>
            <div class="header-dot"></div>
          </div>
          
          <BadgeContent />
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import BadgeContent from './ai-subsystem/components/BadgeContent.vue'

const userStore = useUserStore()

const handleSave = () => {
  ElMessage.success('个人资料已更新')
}
</script>

<style lang="scss" scoped>
.profile-view {
  padding: 40px;
  max-width: 1400px;
  margin: 0 auto;
}

.profile-header {
  text-align: center;
  margin-bottom: 40px;
}

.profile-main-title {
  font-family: var(--font-title);
  font-size: 38px;
  letter-spacing: 12px;
  margin: 0 0 15px 0;
  background: var(--grad-mystic);
  -webkit-background-clip: text;
  background-clip: text;
  -webkit-text-fill-color: transparent;
}

.header-divider {
  width: 250px;
  height: 1px;
  background: linear-gradient(to right, transparent, var(--accent-mystic), transparent);
  margin: 0 auto 15px;
}

.profile-subtitle {
  color: #94a1b2;
  font-size: 14px;
  letter-spacing: 2px;
}

.profile-card {
  padding: 30px;
  text-align: center;
}

.avatar-upload-section {
  margin-bottom: 30px;
  .avatar-wrap {
    position: relative;
    display: inline-block;
    cursor: pointer;
    border-radius: 50%;
    overflow: hidden;
    border: 2px solid var(--accent-primary);
    box-shadow: 0 0 20px rgba(127, 90, 240, 0.3);
    
    .avatar-edit-overlay {
      position: absolute;
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background: rgba(0, 0, 0, 0.4);
      display: flex;
      align-items: center;
      justify-content: center;
      opacity: 0;
      transition: opacity 0.3s;
      color: #fff;
      font-size: 24px;
    }
    
    &:hover .avatar-edit-overlay {
      opacity: 1;
    }
  }
  .upload-hint {
    margin-top: 10px;
    font-size: 12px;
    color: #72757e;
  }
}

.profile-form {
  text-align: left;
  :deep(.el-form-item__label) {
    color: #94a1b2;
    font-family: var(--font-title);
    font-size: 13px;
    letter-spacing: 1px;
  }
  :deep(.el-input__inner), :deep(.el-textarea__inner) {
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    color: #fff;
    &:focus {
      border-color: var(--accent-primary);
    }
  }
}

.form-actions {
  margin-top: 30px;
  .save-btn {
    width: 100%;
    height: 40px;
    background: var(--grad-mystic);
    border: none;
    font-family: var(--font-title);
    letter-spacing: 2px;
  }
}

.badges-card {
  padding: 30px;
  min-height: 600px;
}
</style>
