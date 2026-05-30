import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'dashboard',
      component: () => import('@/views/dashboard/DashboardView.vue'),
      meta: { title: '仪表盘' }
    },
    {
      path: '/memories',
      name: 'memories',
      component: () => import('@/views/memories/MemoriesView.vue'),
      meta: { title: '记忆管理' }
    },
    {
      path: '/import',
      name: 'import',
      component: () => import('@/views/memories/ImportView.vue'),
      meta: { title: '文件导入' }
    },
    {
      path: '/gallery',
      name: 'gallery',
      component: () => import('@/views/gallery/GalleryView.vue'),
      meta: { title: '时光画廊' }
    },
    {
      path: '/timeline',
      name: 'timeline',
      component: () => import('@/views/timeline/TimelineView.vue'),
      meta: { title: '时间轴' }
    },
    {
      path: '/clusters',
      name: 'clusters',
      component: () => import('@/views/clusters/ClustersView.vue'),
      meta: { title: '主题聚类' }
    },
    {
      path: '/narrative',
      name: 'narrative',
      component: () => import('@/views/narrative/NarrativeView.vue'),
      meta: { title: '叙事生成' }
    },
    {
      path: '/sentiment',
      name: 'sentiment',
      component: () => import('@/views/sentiment/SentimentView.vue'),
      meta: { title: '情感光谱' }
    },
    {
      path: '/export',
      name: 'export',
      component: () => import('@/views/export/ExportView.vue'),
      meta: { title: '导出报告' }
    },
    {
      path: '/ai/badges',
      name: 'badges',
      component: () => import('@/views/ai-subsystem/BadgeView.vue'),
      meta: { title: '时光勋章' }
    },
    {
      path: '/ai/index',
      name: 'ai-index',
      component: () => import('@/views/ai-subsystem/IndexView.vue'),
      meta: { title: '心灵回声系统' }
    },
    {
      path: '/ai/keywords',
      name: 'keywords',
      component: () => import('@/views/ai-subsystem/KeywordsView.vue'),
      meta: { title: '灵魂关键词' }
    },
    {
      path: '/ai/palette',
      name: 'palette',
      component: () => import('@/views/ai-subsystem/EmotionPaletteView.vue'),
      meta: { title: '情绪调色板' }
    },
    {
      path: '/ai/echo',
      name: 'echo',
      component: () => import('@/views/ai-subsystem/TimeEchoView.vue'),
      meta: { title: '时光回声' }
    },
    {
      path: '/ai/dialogue',
      name: 'dialogue',
      component: () => import('@/views/ai-subsystem/DialogueView.vue'),
      meta: { title: '心灵对话亭' }
    },
    {
      path: '/ai/future-mail',
      name: 'future-mail',
      component: () => import('@/views/ai-subsystem/FutureMailView.vue'),
      meta: { title: '未来邮局' }
    }
  ]
})

router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title} - Memento AI`
  next()
})

export default router
