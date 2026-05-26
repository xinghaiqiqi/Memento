<template>
  <div class="clusters-container museum-fade-in">
    <div class="museum-header">
      <h1 class="museum-title">记忆影像墙</h1>
      <p class="museum-subtitle">漫步在时光长廊，重温那些被定格的瞬间</p>
      
      <div class="header-actions">
        <button class="ritual-button small sensor-toggle" :class="{ active: sensorEnabled }" @click="toggleSensor">
          <span>{{ sensorEnabled ? '关闭手势传感器' : '开启手势控制' }}</span>
          <div class="button-glow"></div>
        </button>
        <button v-if="focusedCluster" class="ritual-button small secondary" @click="focusedCluster = null">
          <span>返回银河</span>
        </button>
        <button class="ritual-button small" @click="handleRunCluster" :disabled="clustering">
          <span>{{ clustering ? '重构影像墙...' : '重新聚类' }}</span>
          <div class="button-glow"></div>
        </button>
      </div>
    </div>

    <!-- 3D 容器 -->
    <div class="nebula-canvas-wrapper" ref="canvasContainer">
      <div id="nebula-canvas"></div>
      
      <!-- 手势控制监视器 -->
      <div v-show="sensorEnabled" class="sensor-monitor">
        <video ref="videoElement" class="input_video" autoplay muted playsinline style="display:none"></video>
        <canvas ref="canvasElement" class="output_canvas" width="240" height="180"></canvas>
        <div class="sensor-status">手势传感器已启动</div>
      </div>
      
      <!-- 悬浮提示 -->
      <div v-if="hoveredMemory" class="hover-card" :style="{ left: mouseX + 'px', top: mouseY + 'px' }">
        <div class="cluster-tag" :style="{ color: hoveredMemory.clusterColor }">◈ {{ hoveredMemory.clusterName }}</div>
        <h4>{{ hoveredMemory.title }}</h4>
        <p class="date">{{ hoveredMemory.eventDate }}</p>
        <p class="excerpt">{{ hoveredMemory.content.substring(0, 50) }}...</p>
        <span class="hint">点击查阅详细记忆</span>
      </div>

      <!-- 交互提示 -->
      <div class="interaction-hint">
        <p><el-icon><Pointer /></el-icon> 拖动探索 / 滚轮缩放 / 点击照片进入深度回忆</p>
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
            <div v-if="memory.photoUrl" class="item-photo" :style="{ backgroundImage: `url(${memory.photoUrl})` }"></div>
            <div class="item-body">
              <div class="item-header">
                <span class="date">{{ memory.eventDate }}</span>
                <span class="title">{{ memory.title }}</span>
              </div>
              <p class="content">{{ memory.content }}</p>
            </div>
          </div>
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Pointer } from '@element-plus/icons-vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls'
import axios from 'axios'
import { Hands, HAND_CONNECTIONS } from '@mediapipe/hands'
import { Camera } from '@mediapipe/camera_utils'
import { drawConnectors, drawLandmarks } from '@mediapipe/drawing_utils'

// 1. 响应式状态定义
const clustering = ref(false)
const drawerVisible = ref(false)
const selectedCluster = ref(null)
const canvasContainer = ref(null)
const hoveredMemory = ref(null)
const mouseX = ref(0)
const mouseY = ref(0)

const sensorEnabled = ref(false)
const videoElement = ref(null)
const canvasElement = ref(null)

const clusters = ref([])
const allMemories = ref([])

// 2. Three.js & MediaPipe 资源管理
let scene, camera, renderer, controls, raycaster, mouse
let galaxyGroup, starSystem
let clusterGroups = []
let photoMeshes = [] 
let animationFrameId
const textureLoader = new THREE.TextureLoader()

let hands, mpCamera
let lastHandPos = null
let lastPinchDist = null

// 聚焦状态
const focusedCluster = ref(null)
const isFocusing = ref(false)

// --- 手势识别核心逻辑 ---
const initHands = () => {
  hands = new Hands({
    locateFile: (file) => {
      return `https://cdn.jsdelivr.net/npm/@mediapipe/hands/${file}`
    }
  })

  hands.setOptions({
    maxNumHands: 1,
    modelComplexity: 1,
    minDetectionConfidence: 0.5,
    minTrackingConfidence: 0.5,
    selfieMode: true
  })

  hands.onResults(onResults)

  if (videoElement.value) {
    mpCamera = new Camera(videoElement.value, {
      onFrame: async () => {
        if (sensorEnabled.value) {
          await hands.send({ image: videoElement.value })
        }
      },
      width: 640,
      height: 480
    })
  }
}

const onResults = (results) => {
  if (!canvasElement.value || !sensorEnabled.value) return
  
  const canvasCtx = canvasElement.value.getContext('2d')
  if (!canvasCtx) return

  canvasCtx.save()
  
  // 清除画布
  canvasCtx.clearRect(0, 0, canvasElement.value.width, canvasElement.value.height)
  
  // 1. 绘制摄像头画面
  // 优先使用 results.image，如果不存在则尝试直接使用 videoElement
  if (results.image) {
    canvasCtx.drawImage(results.image, 0, 0, canvasElement.value.width, canvasElement.value.height)
  } else if (videoElement.value) {
    canvasCtx.drawImage(videoElement.value, 0, 0, canvasElement.value.width, canvasElement.value.height)
  }
  
  if (results.multiHandLandmarks && results.multiHandLandmarks.length > 0) {
    const landmarks = results.multiHandLandmarks[0]
    
    // 2. 绘制手势辅助线
    drawConnectors(canvasCtx, landmarks, HAND_CONNECTIONS, { color: '#7f5af0', lineWidth: 2 })
    drawLandmarks(canvasCtx, landmarks, { color: '#ffffff', lineWidth: 1, radius: 2 })

    // 1. 旋转交互 (手掌中心移动)
    const palmBase = landmarks[0]
    if (lastHandPos) {
      const dx = palmBase.x - lastHandPos.x
      const dy = palmBase.y - lastHandPos.y
      
      if (controls && !focusedCluster.value) {
        // 水平旋转
        galaxyGroup.rotation.y += dx * 5
        // 俯仰视角
        camera.position.y += dy * 1000
        camera.lookAt(0, 0, 0)
      }
    }
    lastHandPos = { x: palmBase.x, y: palmBase.y }

    // 2. 缩放交互 (拇指与食指捏合)
    const thumbTip = landmarks[4]
    const indexTip = landmarks[8]
    const dist = Math.sqrt(
      Math.pow(thumbTip.x - indexTip.x, 2) + 
      Math.pow(thumbTip.y - indexTip.y, 2)
    )
    
    if (lastPinchDist !== null) {
      const delta = dist - lastPinchDist
      if (Math.abs(delta) > 0.005) {
        camera.position.z -= delta * 5000
        camera.position.z = Math.max(200, Math.min(3000, camera.position.z))
      }
    }
    lastPinchDist = dist

    // 3. 指针交互 (食指尖端)
    const pointerX = (1 - indexTip.x) * window.innerWidth
    const pointerY = indexTip.y * window.innerHeight
    
    // 模拟鼠标坐标给 Raycaster
    mouse.x = (indexTip.x * -2) + 1 // 镜像处理
    mouse.y = (indexTip.y * -2) + 1
    
    mouseX.value = pointerX
    mouseY.value = pointerY
    
    // 自动点击判定 (快速捏合)
    if (dist < 0.03 && !isFocusing.value) {
      isFocusing.value = true
      onClick()
      setTimeout(() => { isFocusing.value = false }, 1000)
    }
  } else {
    lastHandPos = null
    lastPinchDist = null
  }
  canvasCtx.restore()
}

const toggleSensor = async () => {
  if (!sensorEnabled.value) {
    try {
      sensorEnabled.value = true // 先设为 true，确保 onFrame 中的 hands.send 能执行
      if (!hands) {
        initHands()
        console.log('>>> [Sensor] Hands initialized')
      }
      await mpCamera.start()
      console.log('>>> [Sensor] Camera started')
      ElMessage.success('手势传感器已启动')
    } catch (err) {
      sensorEnabled.value = false
      console.error('>>> [Sensor] Failed to start:', err)
      ElMessage.error('无法启动摄像头: ' + err.message)
    }
  } else {
    sensorEnabled.value = false
    if (mpCamera) await mpCamera.stop()
    ElMessage.info('手势传感器已关闭')
  }
}

// 3. 数据加载逻辑
const fetchData = async () => {
  try {
    const [clustersRes, memoriesRes] = await Promise.all([
      axios.get('/api/clusters'),
      axios.get('/api/memories?size=200')
    ])
    
    const clusterData = clustersRes.data.data || []
    const memoryData = memoriesRes.data.records || memoriesRes.data.data?.records || []
    
    allMemories.value = memoryData
    
    if (clusterData.length > 0) {
      clusters.value = clusterData.map((c, index) => {
        const memoryIds = c.memoryIds || []
        const filteredMemories = allMemories.value.filter(m => memoryIds.includes(m.id) && m.photoUrl)
        
        const colors = ['#7f5af0', '#2cb67d', '#ef4565', '#3da9fc', '#ff8906']
        return {
          ...c,
          color: colors[index % colors.length],
          memoryCount: filteredMemories.length,
          memories: filteredMemories
        }
      }).filter(c => c.memoryCount > 0)
    } else {
      useMockData()
    }
    
    if (scene) {
      clearScene()
      createPhotoWall()
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
      memoryCount: 1, color: '#7f5af0',
      memories: [{ id: 1, title: '西湖日落', eventDate: '2024-05-12', content: '美不胜收...', photoUrl: 'https://images.unsplash.com/photo-1506744038136-46273834b3fb' }]
    }
  ]
}

const clearScene = () => {
  if (!scene) return
  if (galaxyGroup) scene.remove(galaxyGroup)
  if (starSystem) scene.remove(starSystem)
  clusterGroups = []
  photoMeshes = []
  focusedCluster.value = null
}

// 4. Three.js 核心逻辑
const initThree = () => {
  if (!canvasContainer.value) return
  
  const width = canvasContainer.value.clientWidth
  const height = canvasContainer.value.clientHeight

  scene = new THREE.Scene()
  camera = new THREE.PerspectiveCamera(75, width / height, 0.1, 10000)
  camera.position.set(0, 500, 1200)

  renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(window.devicePixelRatio)
  
  const canvasEl = document.getElementById('nebula-canvas')
  if (canvasEl) canvasEl.appendChild(renderer.domElement)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.05
  controls.maxDistance = 3000
  controls.minDistance = 200

  raycaster = new THREE.Raycaster()
  mouse = new THREE.Vector2()

  createPhotoWall()
}

const createClusterLabel = (name, color) => {
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  canvas.width = 512
  canvas.height = 128
  
  ctx.fillStyle = 'rgba(0,0,0,0)'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  
  ctx.font = 'bold 60px "Noto Serif SC"'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  
  // 绘制发光文字
  ctx.shadowColor = color
  ctx.shadowBlur = 20
  ctx.fillStyle = '#ffffff'
  ctx.fillText(name, 256, 64)
  
  const texture = new THREE.CanvasTexture(canvas)
  const spriteMaterial = new THREE.SpriteMaterial({ map: texture, transparent: true })
  const sprite = new THREE.Sprite(spriteMaterial)
  sprite.scale.set(300, 75, 1)
  return sprite
}

const createPhotoWall = () => {
  // 1. 背景星尘
  const starGeometry = new THREE.BufferGeometry()
  const starMaterial = new THREE.PointsMaterial({ color: 0x7f5af0, size: 2, transparent: true, opacity: 0.5 })
  const starVertices = []
  for (let i = 0; i < 4000; i++) {
    starVertices.push((Math.random() - 0.5) * 6000, (Math.random() - 0.5) * 6000, (Math.random() - 0.5) * 6000)
  }
  starGeometry.setAttribute('position', new THREE.Float32BufferAttribute(starVertices, 3))
  starSystem = new THREE.Points(starGeometry, starMaterial)
  scene.add(starSystem)

  // 2. 银河容器
  galaxyGroup = new THREE.Group()
  scene.add(galaxyGroup)

  clusterGroups = []
  photoMeshes = []
  
  const clusterRadius = 800 // 星团分布的大圆半径
  
  clusters.value.forEach((cluster, cIndex) => {
    const clusterGroup = new THREE.Group()
    
    // 计算星团在银河中的位置 - 3D 错落分布
    const angle = (cIndex / clusters.value.length) * Math.PI * 2
    const x = Math.cos(angle) * clusterRadius
    const z = Math.sin(angle) * clusterRadius
    const y = (Math.random() - 0.5) * 600 // Y 轴大幅度错落
    clusterGroup.position.set(x, y, z)
    
    // 给星团一个随机的初始倾斜角，更有动感
    clusterGroup.rotation.x = (Math.random() - 0.5) * 0.3
    clusterGroup.rotation.z = (Math.random() - 0.5) * 0.3
    
    // 星团中心标签
    const label = createClusterLabel(cluster.name, cluster.color)
    label.position.set(0, 0, 0)
    clusterGroup.add(label)
    
    // 核心光晕
    const coreGeo = new THREE.SphereGeometry(20, 32, 32)
    const coreMat = new THREE.MeshBasicMaterial({ color: cluster.color, transparent: true, opacity: 0.3 })
    const core = new THREE.Mesh(coreGeo, coreMat)
    clusterGroup.add(core)

    // 排列该星团内的照片
    const photoMemories = cluster.memories
    const photoCount = photoMemories.length
    const innerRadius = 250 + (photoCount * 10) // 小圆半径随数量增加
    
    photoMemories.forEach((memory, pIndex) => {
      const w = 100, h = 75
      const photoGeo = new THREE.PlaneGeometry(w, h)
      const texture = textureLoader.load(memory.photoUrl)
      const photoMat = new THREE.MeshBasicMaterial({ map: texture, side: THREE.DoubleSide, transparent: true, opacity: 0, depthWrite: true })
      const mesh = new THREE.Mesh(photoGeo, photoMat)
      
      // 在星团内部做圆形分布
      const pAngle = (pIndex / photoCount) * Math.PI * 2
      const px = Math.cos(pAngle) * innerRadius
      const pz = Math.sin(pAngle) * innerRadius
      const py = (Math.random() - 0.5) * 200
      
      mesh.position.set(px, py, pz)
      mesh.lookAt(0, py, 0) // 看向星团中心
      
      mesh.userData = {
        memory: { ...memory, clusterName: cluster.name, clusterColor: cluster.color },
        targetPos: mesh.position.clone(),
        targetRot: mesh.rotation.clone(),
        floatOffset: Math.random() * Math.PI * 2,
        floatSpeed: 0.2 + Math.random() * 0.3,
        entryProgress: 0,
        clusterId: cluster.id,
        parentGroup: clusterGroup
      }
      
      clusterGroup.add(mesh)
      photoMeshes.push(mesh)
    })
    
    clusterGroup.userData = {
      id: cluster.id,
      originalPos: clusterGroup.position.clone(),
      rotationSpeed: 0.005 + Math.random() * 0.005,
      isFocused: false
    }
    
    galaxyGroup.add(clusterGroup)
    clusterGroups.push(clusterGroup)
  })
}

const animate = () => {
  animationFrameId = requestAnimationFrame(animate)
  if (controls) controls.update()

  const time = performance.now() * 0.001

  // 1. 银河整体慢转
  if (galaxyGroup && !focusedCluster.value) {
    galaxyGroup.rotation.y += 0.0005
  }

  // 2. 星团逻辑
  clusterGroups.forEach(group => {
    const isThisFocused = focusedCluster.value === group.userData.id
    
    // 如果不是聚焦状态，或者不是被聚焦的星团，则自转
    if (!isThisFocused) {
      group.rotation.y += group.userData.rotationSpeed
    }
    
    // 聚焦过渡逻辑
    if (focusedCluster.value) {
      if (isThisFocused) {
        // 移向中心并放大
        group.position.lerp(new THREE.Vector3(0, 0, 0), 0.1)
        group.scale.lerp(new THREE.Vector3(1.5, 1.5, 1.5), 0.1)
        // 聚焦时摆正姿态（消除 X 和 Z 轴的随机倾斜）
        group.rotation.x = THREE.MathUtils.lerp(group.rotation.x, 0, 0.1)
        group.rotation.z = THREE.MathUtils.lerp(group.rotation.z, 0, 0.1)
        // 缓慢转动展示
        group.rotation.y += 0.002
      } else {
        // 其他星团淡出并缩小
        group.scale.lerp(new THREE.Vector3(0.01, 0.01, 0.01), 0.1)
      }
    } else {
      // 恢复原始 3D 错落位置
      group.position.lerp(group.userData.originalPos, 0.05)
      group.scale.lerp(new THREE.Vector3(1, 1, 1), 0.05)
      // 恢复一定的倾斜动感
      group.rotation.x = THREE.MathUtils.lerp(group.rotation.x, (group.userData.id % 2 === 0 ? 0.15 : -0.15), 0.05)
    }
  })

  // 3. 影像个体动画
  photoMeshes.forEach(mesh => {
    const data = mesh.userData
    const isParentFocused = focusedCluster.value === data.clusterId
    
    // 进场与透明度处理
    if (data.entryProgress < 1) {
      data.entryProgress += 0.02
      mesh.material.opacity = data.entryProgress * 0.9
    }

    // 呼吸浮动
    const f = Math.sin(time * data.floatSpeed + data.floatOffset) * 10
    mesh.position.y = data.targetPos.y + f
    
    // 如果星团被聚焦，增加光亮
    if (focusedCluster.value) {
      mesh.material.opacity = THREE.MathUtils.lerp(mesh.material.opacity, isParentFocused ? 1.0 : 0, 0.1)
    } else {
      mesh.material.opacity = THREE.MathUtils.lerp(mesh.material.opacity, 0.9, 0.1)
    }
  })
  
  if (starSystem) {
    starSystem.rotation.y += 0.0001
  }

  if (renderer && scene && camera) renderer.render(scene, camera)
}

// 5. 事件监听
const onMouseMove = (event) => {
  if (!canvasContainer.value || !mouse) return
  const rect = canvasContainer.value.getBoundingClientRect()
  mouse.x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  mouse.y = -((event.clientY - rect.top) / rect.height) * 2 + 1
  mouseX.value = event.clientX + 20
  mouseY.value = event.clientY + 20
}

const onClick = () => {
  if (!raycaster || !mouse || !camera || !galaxyGroup) return
  
  raycaster.setFromCamera(mouse, camera)
  
  // 检查是否点击了照片
  const photoIntersects = raycaster.intersectObjects(photoMeshes)
  
  if (photoIntersects.length > 0) {
    const clickedMesh = photoIntersects[0].object
    const clusterId = clickedMesh.userData.clusterId
    
    if (focusedCluster.value === clusterId) {
      // 已经聚焦，点击照片打开详情
      selectedCluster.value = {
        name: clickedMesh.userData.memory.clusterName,
        description: '这段记忆属于主题：' + clickedMesh.userData.memory.clusterName,
        memories: [clickedMesh.userData.memory]
      }
      drawerVisible.value = true
    } else {
      focusedCluster.value = clusterId
    }
    return
  }
  
  // 检查是否点击了星团中心的标签或核心
  const allClickable = []
  clusterGroups.forEach(g => {
    g.children.forEach(child => {
      if (!(child instanceof THREE.Mesh && photoMeshes.includes(child))) {
        allClickable.push(child)
      }
    })
  })
  
  const labelIntersects = raycaster.intersectObjects(allClickable)
  if (labelIntersects.length > 0) {
    let target = labelIntersects[0].object
    // 向上寻找包含 clusterId 的 parentGroup
    while (target && !target.userData.id) {
      target = target.parent
    }
    if (target && target.userData.id) {
      focusedCluster.value = target.userData.id
      return
    }
  }

  // 点击空白处
  focusedCluster.value = null
}

const handleResize = () => {
  if (!canvasContainer.value || !camera || !renderer) return
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
    ElMessage.success('影像墙重构完成')
    await fetchData()
  } catch (error) {
    console.error('Clustering failed:', error)
    ElMessage.error('重构失败，请检查 AI 配置')
  } finally {
    clustering.value = false
  }
}

// 6. 生命周期
onMounted(() => {
  fetchData()
  initThree()
  animate()
  window.addEventListener('resize', handleResize)
  window.addEventListener('mousemove', onMouseMove)
  window.addEventListener('click', onClick)
})

onUnmounted(() => {
  cancelAnimationFrame(animationFrameId)
  if (mpCamera) mpCamera.stop()
  if (hands) hands.close()
  window.removeEventListener('resize', handleResize)
  window.removeEventListener('mousemove', onMouseMove)
  window.removeEventListener('click', onClick)
  if (renderer) {
    renderer.dispose()
    renderer.domElement.remove()
  }
})
</script>

<style lang="scss" scoped>
.clusters-container {
  padding: 40px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.museum-header {
  text-align: center;
  margin-bottom: 40px;
  z-index: 10;
  
  .museum-title {
    font-size: 42px;
    background: linear-gradient(to bottom, #fff, #94a1b2);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    margin: 0;
  }

  .museum-subtitle {
    color: var(--accent-mystic);
    font-size: 14px;
    letter-spacing: 2px;
    margin-top: 15px;
    opacity: 0.8;
    text-transform: uppercase;
  }

  .header-actions {
    display: flex;
    justify-content: center;
    gap: 15px;
    margin-top: 25px;
    
    .sensor-toggle {
      background: rgba(127, 90, 240, 0.1);
      border-color: rgba(127, 90, 240, 0.3);
      &.active {
        background: rgba(127, 90, 240, 0.3);
        border-color: #7f5af0;
        box-shadow: 0 0 15px rgba(127, 90, 240, 0.4);
      }
    }

    .secondary {
      background: rgba(255, 255, 255, 0.05);
      border-color: rgba(255, 255, 255, 0.2);
      &:hover {
        background: rgba(255, 255, 255, 0.1);
        border-color: var(--accent-primary);
      }
    }
  }
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
  background: rgba(15, 18, 24, 0.95);
  backdrop-filter: blur(15px);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 16px;
  padding: 20px;
  width: 280px;
  pointer-events: none;
  z-index: 100;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.6);
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  
  .cluster-tag {
    font-family: var(--font-title);
    font-size: 11px;
    letter-spacing: 2px;
    margin-bottom: 10px;
    text-transform: uppercase;
  }

  h4 { color: #fff; margin: 0 0 6px 0; font-family: var(--font-title); font-size: 18px; letter-spacing: 1px; }
  
  .date {
    color: var(--accent-mystic);
    font-size: 12px;
    margin-bottom: 12px;
    font-family: var(--font-title);
    opacity: 0.8;
  }

  .excerpt { 
    color: #a0aec0; 
    font-size: 13px; 
    margin-bottom: 15px; 
    line-height: 1.6;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
  }

  .hint { 
    display: block;
    color: var(--accent-primary); 
    font-size: 11px; 
    letter-spacing: 1px;
    border-top: 1px solid rgba(255, 255, 255, 0.05);
    padding-top: 10px;
  }
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
        display: flex;
        gap: 20px;
        
        &:hover { border-color: var(--accent-primary); transform: translateX(10px); background: rgba(127, 90, 240, 0.05); }

        .item-photo {
          width: 100px;
          height: 100px;
          border-radius: 8px;
          background-size: cover;
          background-position: center;
          flex-shrink: 0;
        }

        .item-body {
          flex: 1;
        }

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

.sensor-monitor {
  position: absolute;
  bottom: 20px;
  right: 20px;
  width: 240px;
  height: 180px;
  background: rgba(11, 14, 20, 0.8);
  border: 1px solid rgba(127, 90, 240, 0.3);
  border-radius: 12px;
  overflow: hidden;
  z-index: 100;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.5);
  display: flex;
  flex-direction: column;

  .output_canvas {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transform: scaleX(-1); // 镜像显示
  }

  .sensor-status {
    position: absolute;
    bottom: 0;
    left: 0;
    right: 0;
    padding: 5px;
    background: rgba(127, 90, 240, 0.8);
    color: white;
    font-size: 10px;
    text-align: center;
    text-transform: uppercase;
    letter-spacing: 1px;
  }
}
</style>
