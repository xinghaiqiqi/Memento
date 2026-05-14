<template>
  <div class="clusters-container museum-fade-in">
    <div class="clusters-header">
      <div class="title-section">
        <h2 class="museum-title">Memory Nebula</h2>
        <p class="museum-subtitle">漫步在记忆的星云中，探索灵魂的交织</p>
      </div>
      <div class="header-actions">
        <button class="ritual-button small" @click="handleRunCluster" :disabled="clustering">
          <span>{{ clustering ? '正在编织星云...' : '重新聚类' }}</span>
          <div class="button-glow"></div>
        </button>
      </div>
    </div>

    <!-- 3D 容器 -->
    <div class="nebula-canvas-wrapper" ref="canvasContainer">
      <div id="nebula-canvas"></div>
      
      <!-- 悬浮提示 -->
      <div v-if="hoveredNode" class="hover-card" :style="{ left: mouseX + 'px', top: mouseY + 'px' }">
        <h4>{{ hoveredNode.name }}</h4>
        <p>{{ hoveredNode.description }}</p>
        <span class="hint">点击进入星团</span>
      </div>

      <!-- 交互提示 -->
      <div class="interaction-hint">
        <p><el-icon><Pointer /></el-icon> 拖动旋转 / 滚轮缩放 / 点击星团探索</p>
      </div>
    </div>

    <!-- 星团详情抽屉 -->
    <el-drawer
      v-model="drawerVisible"
      :title="selectedCluster?.name"
      size="50%"
      class="museum-drawer"
    >
      <div v-if="selectedCluster" class="cluster-detail">
        <p class="detail-desc">{{ selectedCluster.description }}</p>
        <el-divider />
        <div class="memory-list">
          <div v-for="memory in selectedCluster.memories" :key="memory.id" class="memory-item">
            <div class="item-header">
              <span class="date">{{ memory.eventDate }}</span>
              <span class="title">{{ memory.title }}</span>
            </div>
            <p class="content">{{ memory.content }}</p>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import { Pointer } from '@element-plus/icons-vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls'
import axios from 'axios'

const clustering = ref(false)
const drawerVisible = ref(false)
const selectedCluster = ref(null)
const canvasContainer = ref(null)
const hoveredNode = ref(null)
const mouseX = ref(0)
const mouseY = ref(0)

const clusters = ref([])
const allMemories = ref([])

// 初始化数据
const fetchData = async () => {
  try {
    const [clustersRes, memoriesRes] = await Promise.all([
      axios.get('/api/clusters'),
      axios.get('/api/memories?size=100')
    ])
    
    allMemories.value = memoriesRes.data.records || []
    
    if (clustersRes.data && clustersRes.data.length > 0) {
      clusters.value = clustersRes.data.map((c, index) => {
        const memoryIds = JSON.parse(c.memoryIds || '[]')
        const colors = ['#7f5af0', '#2cb67d', '#ef4565', '#3da9fc', '#ff8906']
        return {
          ...c,
          color: colors[index % colors.length],
          memoryCount: memoryIds.length,
          memories: allMemories.value.filter(m => memoryIds.includes(m.id))
        }
      })
    } else {
      // 降级使用模拟数据
      useMockData()
    }
    
    // 数据加载后重新构建星云
    if (scene) {
      clearScene()
      createNebula()
    }
  } catch (error) {
    console.error('Fetch data failed:', error)
    useMockData()
  }
}

const useMockData = () => {
  clusters.value = [
    { 
      id: 1, name: '旅行足迹', description: '记录了你前往不同城市的精彩瞬间。', 
      memoryCount: 15, color: '#7f5af0',
      memories: [{ id: 1, title: '西湖日落', eventDate: '2024-05-12', content: '美不胜收...' }]
    },
    { 
      id: 2, name: '职场成长', description: '工作中的挑战与突破。', 
      memoryCount: 10, color: '#2cb67d',
      memories: []
    },
    { 
      id: 3, name: '生活感悟', description: '平凡日子里的不平凡思考。', 
      memoryCount: 20, color: '#ef4565',
      memories: []
    }
  ]
}

const clearScene = () => {
  while(scene.children.length > 0){ 
    scene.remove(scene.children[0]) 
  }
  clusterPoints = []
}

// Three.js 相关变量
let scene, camera, renderer, controls, raycaster, mouse
let stars = []
let clusterPoints = []
let animationFrameId

onMounted(() => {
  fetchData() // 加载真实数据
  initThree()
  animate()
  window.addEventListener('resize', handleResize)
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('click', onClick)
})

onUnmounted(() => {
  cancelAnimationFrame(animationFrameId)
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('click', onClick)
  if (renderer) {
    renderer.dispose()
    renderer.domElement.remove()
  }
})

const initThree = () => {
  const width = canvasContainer.value.clientWidth
  const height = canvasContainer.value.clientHeight

  scene = new THREE.Scene()
  
  camera = new THREE.PerspectiveCamera(75, width / height, 0.1, 2000)
  camera.position.z = 400

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(window.devicePixelRatio)
  document.getElementById('nebula-canvas').appendChild(renderer.domElement)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.05
  controls.autoRotate = true
  controls.autoRotateSpeed = 0.5

  raycaster = new THREE.Raycaster()
  mouse = new THREE.Vector2()

  createNebula()
}

const createNebula = () => {
  // 1. 背景底噪星星
  const starGeometry = new THREE.BufferGeometry()
  const starMaterial = new THREE.PointsMaterial({ color: 0xffffff, size: 0.7, transparent: true, opacity: 0.5 })
  const starVertices = []
  for (let i = 0; i < 5000; i++) {
    const x = (Math.random() - 0.5) * 2000
    const y = (Math.random() - 0.5) * 2000
    const z = (Math.random() - 0.5) * 2000
    starVertices.push(x, y, z)
  }
  starGeometry.setAttribute('position', new THREE.Float32BufferAttribute(starVertices, 3))
  const backgroundStars = new THREE.Points(starGeometry, starMaterial)
  scene.add(backgroundStars)

  // 2. 创建星团
  clusters.value.forEach((cluster, index) => {
    const group = new THREE.Group()
    const color = new THREE.Color(cluster.color)
    
    // 中心核心大星
    const coreGeo = new THREE.SphereGeometry(8, 32, 32)
    const coreMat = new THREE.MeshBasicMaterial({ 
      color: color, 
      transparent: true, 
      opacity: 0.8
    })
    const core = new THREE.Mesh(coreGeo, coreMat)
    
    // 发光光晕
    const spriteMaterial = new THREE.SpriteMaterial({
      map: createGlowTexture(cluster.color),
      color: color,
      transparent: true,
      blending: THREE.AdditiveBlending
    })
    const sprite = new THREE.Sprite(spriteMaterial)
    sprite.scale.set(40, 40, 1)
    core.add(sprite)

    // 随机分布记忆点星星
    const pointsGeo = new THREE.BufferGeometry()
    const pointsVertices = []
    for (let i = 0; i < cluster.memoryCount * 5; i++) {
      const r = 20 + Math.random() * 40
      const theta = Math.random() * Math.PI * 2
      const phi = Math.random() * Math.PI
      pointsVertices.push(
        r * Math.sin(phi) * Math.cos(theta),
        r * Math.sin(phi) * Math.sin(theta),
        r * Math.cos(phi)
      )
    }
    pointsGeo.setAttribute('position', new THREE.Float32BufferAttribute(pointsVertices, 3))
    const pointsMat = new THREE.PointsMaterial({ color: color, size: 2, transparent: true, opacity: 0.6 })
    const cloud = new THREE.Points(pointsGeo, pointsMat)
    
    group.add(core)
    group.add(cloud)

    // 确定星团在空间中的位置
    const angle = (index / clusters.value.length) * Math.PI * 2
    const dist = 150
    group.position.set(Math.cos(angle) * dist, Math.sin(angle) * dist, (Math.random() - 0.5) * 100)
    
    group.userData = { clusterData: cluster }
    clusterPoints.push(core)
    scene.add(group)
  })
}

const createGlowTexture = (colorStr) => {
  const canvas = document.createElement('canvas')
  canvas.width = 64
  canvas.height = 64
  const ctx = canvas.getContext('2d')
  const gradient = ctx.createRadialGradient(32, 32, 0, 32, 32, 32)
  gradient.addColorStop(0, 'rgba(255, 255, 255, 1)')
  gradient.addColorStop(0.2, colorStr)
  gradient.addColorStop(0.5, 'rgba(0, 0, 0, 0)')
  ctx.fillStyle = gradient
  ctx.fillRect(0, 0, 64, 64)
  const texture = new THREE.CanvasTexture(canvas)
  return texture
}

const animate = () => {
  animationFrameId = requestAnimationFrame(animate)
  controls.update()
  
  // 碰撞检测
  raycaster.setFromCamera(mouse, camera)
  const intersects = raycaster.intersectObjects(clusterPoints)
  
  if (intersects.length > 0) {
    const obj = intersects[0].object
    hoveredNode.value = obj.parent.userData.clusterData
    document.body.style.cursor = 'pointer'
    obj.scale.set(1.2, 1.2, 1.2)
  } else {
    hoveredNode.value = null
    document.body.style.cursor = 'default'
    clusterPoints.forEach(p => p.scale.set(1, 1, 1))
  }

  renderer.render(scene, camera)
}

const onMouseMove = (event) => {
  const rect = canvasContainer.value.getBoundingClientRect()
  mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1
  mouseX.value = event.clientX + 20
  mouseY.value = event.clientY + 20
}

const onClick = (event) => {
  if (hoveredNode.value) {
    selectedCluster.value = hoveredNode.value
    drawerVisible.value = true
  }
}

const handleResize = () => {
  if (!canvasContainer.value) return
  const width = canvasContainer.value.clientWidth
  const height = canvasContainer.value.clientHeight
  camera.aspect = width / height
  camera.updateProjectionMatrix()
  renderer.setSize(width, height)
}

const handleRunCluster = async () => {
  clustering.value = true
  try {
    await axios.post('/api/clusters/run')
    ElMessage.success('星云重构完成，发现了新的时空节点')
    await fetchData() // 重新加载数据并更新 3D 场景
  } catch (error) {
    console.error('Clustering failed:', error)
    ElMessage.error('星云重构失败，请检查 AI 配置')
  } finally {
    clustering.value = false
  }
}
</script>

<style lang="scss" scoped>
.clusters-container {
  padding: 40px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.clusters-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  z-index: 10;
}

.nebula-canvas-wrapper {
  flex: 1;
  position: relative;
  border-radius: 40px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.2);
  border: 1px solid rgba(255, 255, 255, 0.05);
  box-shadow: inset 0 0 50px rgba(0, 0, 0, 0.5);
  
  #nebula-canvas {
    width: 100%;
    height: 100%;
  }
}

.hover-card {
  position: fixed;
  background: rgba(15, 18, 24, 0.9);
  backdrop-filter: blur(10px);
  border: 1px solid var(--accent-primary);
  border-radius: 12px;
  padding: 15px;
  width: 220px;
  pointer-events: none;
  z-index: 100;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
  
  h4 { color: #fff; margin: 0 0 8px 0; font-family: var(--font-title); }
  p { color: #94a1b2; font-size: 13px; margin: 0 0 10px 0; line-height: 1.5; }
  .hint { color: var(--accent-primary); font-size: 11px; letter-spacing: 1px; }
}

.interaction-hint {
  position: absolute;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.05);
  padding: 8px 25px;
  border-radius: 50px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  p { margin: 0; font-size: 12px; color: #72757e; display: flex; align-items: center; gap: 8px; }
}

.cluster-detail {
  padding: 20px;
  .detail-desc {
    color: #94a1b2;
    line-height: 2;
    font-size: 16px;
    font-family: 'Noto Serif SC', serif;
    font-style: italic;
    margin-bottom: 30px;
  }
  .memory-list {
    .memory-item {
      margin-bottom: 25px;
      padding: 25px;
      background: rgba(255, 255, 255, 0.02);
      border: 1px solid rgba(255, 255, 255, 0.05);
      border-radius: 16px;
      transition: all 0.3s;
      
      &:hover { border-color: var(--accent-primary); transform: translateX(10px); background: rgba(127, 90, 240, 0.05); }

      .item-header {
        margin-bottom: 12px;
        display: flex;
        align-items: center;
        gap: 15px;
        .date { color: var(--accent-primary); font-size: 13px; font-weight: bold; font-family: var(--font-title); }
        .title { font-weight: bold; color: #fff; font-size: 17px; }
      }
      .content { font-size: 15px; color: #94a1b2; line-height: 1.8; }
    }
  }
}

/* 仪式感按钮样式引用 */
.ritual-button {
  position: relative; background: var(--accent-primary); border: none; color: #fff;
  padding: 15px 40px; border-radius: 50px; font-family: var(--font-title);
  font-size: 16px; letter-spacing: 3px; cursor: pointer; transition: all 0.3s;
  
  &:hover:not(:disabled) { transform: scale(1.05); box-shadow: 0 0 20px rgba(127, 90, 240, 0.4); }
  &.small { padding: 10px 25px; font-size: 13px; }
  &:disabled { opacity: 0.5; cursor: not-allowed; }
}
</style>
