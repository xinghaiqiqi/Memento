<template>
  <div class="sentiment-container museum-fade-in">
    <div class="museum-header">
      <h1 class="museum-title">情感光谱</h1>
      <p class="museum-subtitle">捕捉情感的波长，洞察灵魂的起伏</p>
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

const lineChartRef = ref(null)
const pieChartRef = ref(null)
const heatmapRef = ref(null)

let lineChart = null
let pieChart = null
let heatmap = null

onMounted(() => {
  initLineChart()
  initPieChart()
  initHeatmap()
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

const initLineChart = () => {
  lineChart = echarts.init(lineChartRef.value, 'dark')
  lineChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', backgroundColor: 'rgba(15, 18, 24, 0.9)', borderColor: 'rgba(127, 90, 240, 0.3)', textStyle: { color: '#fff' } },
    xAxis: {
      type: 'category',
      data: ['5.1', '5.3', '5.5', '5.7', '5.9', '5.11', '5.13'],
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
      axisLabel: { color: '#94a1b2' }
    },
    yAxis: {
      type: 'value',
      min: -1,
      max: 1,
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } },
      axisLabel: { color: '#94a1b2' }
    },
    series: [{
      data: [0.6, 0.2, -0.4, 0.5, 0.8, 0.1, 0.9],
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 8,
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

const initPieChart = () => {
  pieChart = echarts.init(pieChartRef.value, 'dark')
  pieChart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item', backgroundColor: 'rgba(15, 18, 24, 0.9)', borderColor: 'rgba(127, 90, 240, 0.3)', textStyle: { color: '#fff' } },
    series: [{
      type: 'pie',
      radius: ['55%', '75%'],
      avoidLabelOverlap: false,
      itemStyle: { borderRadius: 12, borderColor: '#0b0e14', borderWidth: 3 },
      label: { show: false },
      data: [
        { value: 45, name: '积极', itemStyle: { color: '#2cb67d' } },
        { value: 30, name: '中性', itemStyle: { color: '#72757e' } },
        { value: 25, name: '消极', itemStyle: { color: '#ef4565' } }
      ]
    }]
  })
}

const initHeatmap = () => {
  heatmap = echarts.init(heatmapRef.value, 'dark')
  const data = []
  for (let i = 0; i < 30; i++) {
    data.push(['2024-05-' + (i + 1 < 10 ? '0' + (i + 1) : i + 1), Math.random() * 2 - 1])
  }
  
  heatmap.setOption({
    backgroundColor: 'transparent',
    visualMap: {
      min: -1,
      max: 1,
      calculable: true,
      orient: 'horizontal',
      left: 'center',
      top: 0,
      inRange: { color: ['#ef4565', 'rgba(255,255,255,0.05)', '#2cb67d'] },
      textStyle: { color: '#94a1b2' }
    },
    calendar: {
      top: 80,
      left: 30,
      right: 30,
      cellSize: ['auto', 25],
      range: '2024-05',
      itemStyle: { borderWidth: 2, borderColor: '#0b0e14', color: 'rgba(255,255,255,0.02)' },
      yearLabel: { show: false },
      dayLabel: { color: '#94a1b2', nameMap: 'cn' },
      monthLabel: { color: '#94a1b2', nameMap: 'cn' }
    },
    series: {
      type: 'heatmap',
      coordinateSystem: 'calendar',
      data: data
    }
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
    font-size: 42px;
    background: linear-gradient(to bottom, #fff, #94a1b2);
    -webkit-background-clip: text;
    background-clip: text;
    -webkit-text-fill-color: transparent;
    margin: 0;
  }
  .museum-subtitle {
    color: var(--accent-primary);
    letter-spacing: 4px;
    font-size: 14px;
    margin-top: 15px;
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
