import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref({
    nickname: '记忆收藏家',
    avatar: 'https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png',
    memoryCount: 0
  })

  const setUserInfo = (info) => {
    userInfo.value = { ...userInfo.value, ...info }
  }

  return {
    userInfo,
    setUserInfo
  }
})
