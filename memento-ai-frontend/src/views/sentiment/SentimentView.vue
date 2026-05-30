<template>
  <div class="sentiment-container museum-fade-in">
    <div class="museum-header">
      <div class="header-inner">
        <h1 class="museum-title">情感光谱</h1>
        <div class="header-divider"></div>
        <p class="museum-subtitle">捕捉情感的波长，洞察灵魂的起伏</p>
      </div>
    </div>

    <el-row :gutter="30">
      <el-col :span="16">
        <div class="museum-exhibit-card chart-card">
          <div class="card-header">
            <span class="header-title">情感波动经纬</span>
            <div class="header-dot"></div>
          </div>
          <div ref="lineChartRef" class="chart-box"></div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="museum-exhibit-card stat-card">
          <div class="card-header">
            <span class="header-title">心境占比分布</span>
            <div class="header-dot"></div>
          </div>
          <div ref="pieChartRef" class="chart-box"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="30" class="mt-30">
      <el-col :span="24">
        <div class="museum-exhibit-card heatmap-card">
          <div class="card-header">
            <span class="header-title">时光情感年鉴</span>
            <div class="header-dot"></div>
          </div>
          <div ref="heatmapRef" class="chart-box-large"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
// ... (keep script as is, but ensure imports and init logic are correct)
import { ref, onMounted, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const lineChartRef = ref(null)
const pieChartRef = ref(null)
const heatmapRef = ref(null)

let lineChart = null
let pieChart = null
let heatmap = null

const fetchData = async () => {
  try {
    const [statsRes, timeseriesRes] = await Promise.all([
      axios.get('/api/sentiment/statistics'),
      axios.get('/api/sentiment/timeseries')
    ])
    
    if (statsRes.data.code === 200) {
      updatePieChart(statsRes.data.data)
    }
    
    if (timeseriesRes.data.code === 200) {
      updateLineChart(timeseriesRes.data.data)
    }
  } catch (error) {
    console.error('获取情感数据失败:', error)
    ElMessage.error('无法加载情感数据')
  }
}

onMounted(() => {
  initCharts()
  fetchData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  lineChart?.dispose()
  pieChart?.dispose()
  heatmap?.dispose()
})

const handleResize = () => {
  lineChart?.resize()
  pieChart?.resize()
  heatmap?.resize()
}

const initCharts = () => {
  lineChart = echarts.init(lineChartRef.value, 'dark')
  pieChart = echarts.init(pieChartRef.value, 'dark')
  heatmap = echarts.init(heatmapRef.value, 'dark')
  
  // 初始化热力图（使用 mock 数据作为背景）
  const heatmapData = []
  for (let i = 0; i < 30; i++) {
    heatmapData.push(['2024-05-' + (i + 1 < 10 ? '0' + (i + 1) : i + 1), Math.random() * 2 - 1])
  }
  
  heatmap.setOption({
    backgroundColor: 'transparent',
    visualMap: {
      min: -1, max: 1, calculable: true, orient: 'horizontal', left: 'center', top: 0,
      inRange: { color: ['#ef4565', 'rgba(255,255,255,0.05)', '#2cb67d'] },
      textStyle: { color: '#94a1b2' }
    },
    calendar: {
      top: 80, left: 30, right: 30, cellSize: ['auto', 25], range: '2024-05',
      itemStyle: { borderWidth: 2, borderColor: '#0b0e14', color: 'rgba(255,255,255,0.02)' },
      yearLabel: { show: false },
      dayLabel: { color: '#94a1b2', nameMap: 'cn' },
      monthLabel: { color: '#94a1b2', nameMap: 'cn' }
    },
    series: { type: 'heatmap', coordinateSystem: 'calendar', data: heatmapData }
  })
}

const updateLineChart = (data) => {
  const dates = data.map(item => item.date.split('T')[0]).reverse()
  const scores = data.map(item => item.score).reverse()
  
  lineChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(15, 18, 24, 0.9)', borderColor: 'rgba(127, 90, 240, 0.3)', textStyle: { color: '#fff' } },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
      axisLabel: { color: '#94a1b2' }
    },
    yAxis: {
      type: 'value', min: -1, max: 1,
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } },
      axisLabel: { color: '#94a1b2' }
    },
    series: [{
      data: scores,
      type: 'line', smooth: true, symbol: 'circle', symbolSize: 8,
      itemStyle: { color: '#7f5af0' },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{
          offset: 0, color: 'rgba(127, 90, 240, 0.3)'
        }, {
          offset: 1, color: 'transparent'
        }])
      }
    }]
  })
}

const updatePieChart = (stats) => {
  pieChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', backgroundColor: 'rgba(15, 18, 24, 0.9)', borderColor: 'rgba(127, 90, 240, 0.3)', textStyle: { color: '#fff' } },
    series: [{
      type: 'pie', radius: ['55%', '75%'], avoidLabelOverlap: false,
      itemStyle: { borderRadius: 12, borderColor: '#0b0e14', borderWidth: 3 },
      label: { show: false },
      data: [
        { value: stats.positiveCount, name: '积极', itemStyle: { color: '#2cb67d' } },
        { value: stats.neutralCount, name: '中性', itemStyle: { color: '#72757e' } },
        { value: stats.negativeCount, name: '消极', itemStyle: { color: '#ef4565' } }
      ]
    }]
  })
}
</script>

<style lang="scss" scoped>
.sentiment-container {
  padding: 60px 40px;
  max-width: 1400px;
  margin: 0 auto;
}

.museum-header {
  text-align: center;
  margin-bottom: 60px;
  
  .museum-title {
    font-family: var(--font-title);
    font-size: 38px;
    letter-spacing: 12px;
    margin: 0 0 15px 0;
    background: var(--grad-mystic);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    filter: drop-shadow(0 0 15px rgba(127, 90, 240, 0.4));
  }

  .header-divider {
    width: 220px;
    height: 1px;
    background: linear-gradient(to right, transparent, var(--accent-mystic), transparent);
    margin: 0 auto 12px;
  }

  .museum-subtitle {
    color: #94a1b2;
    font-size: 15px;
    letter-spacing: 3px;
    font-weight: 300;
  }
}

.museum-exhibit-card {
  background: rgba(255, 255, 255, 0.02);
  border: 1px solid rgba(255, 255, 255, 0.05);
  border-radius: 24px;
  padding: 30px;
  transition: all 0.4s;
  
  &:hover {
    background: rgba(255, 255, 255, 0.04);
    border-color: var(--accent-primary);
    box-shadow: 0 15px 40px rgba(0, 0, 0, 0.3);
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 30px;
    padding-bottom: 15px;
    border-bottom: 1px solid rgba(255, 255, 255, 0.05);
    
    .header-title {
      font-family: var(--font-title);
      color: #fff;
      font-size: 18px;
      letter-spacing: 2px;
    }
    .header-dot {
      width: 6px; height: 6px;
      background: var(--accent-primary);
      border-radius: 50%;
      box-shadow: 0 0 10px var(--accent-primary);
    }
  }
}

.chart-box {
  height: 380px;
}
.chart-box-large {
  height: 320px;
}
.mt-30 { margin-top: 30px; }
</style>
