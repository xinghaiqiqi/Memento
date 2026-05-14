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
    }
  ]
})

router.beforeEach((to, from, next) => {
  document.title = `${to.meta.title} - Memento AI`
  next()
})

export default router
