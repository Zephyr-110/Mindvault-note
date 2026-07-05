C:\Users\30149\IdeaProjects\MindVault-note-project\frontend\src\components\MindMapView.vue
<template>
  <div class="mindmap-container" ref="containerRef">
    <div class="hint-overlay" v-if="isEmpty">
      <el-empty description="暂无目录数据" />
    </div>
    <div class="hint-tip" v-if="!isEmpty">
      <span>🖱️ 拖拽平移 &nbsp;|&nbsp; 滚轮缩放 &nbsp;|&nbsp; 点击文档节点跳转</span>
    </div>
    <svg
      v-if="!isEmpty"
      ref="svgRef"
      class="mindmap-svg"
      xmlns="http://www.w3.org/2000/svg"
    >
      <g ref="mainGroup">
        <line
          v-for="(link, idx) in linkLines"
          :key="'link-' + idx"
          :x1="link.x1" :y1="link.y1"
          :x2="link.x2" :y2="link.y2"
          stroke="var(--text-muted)"
          stroke-width="2"
          stroke-linecap="round"
        />
        <g v-for="node in layoutNodes" :key="node.id" class="node-group">
          <rect
            :x="node.x - node.width / 2"
            :y="node.y - node.height / 2"
            :width="node.width"
            :height="node.height"
            :rx="node.type === 'category' ? 10 : 4"
            :fill="node.type === 'category' ? '#4a90e2' : '#ff9500'"
            class="mindmap-node"
            :class="{ 'doc-node': node.type === 'document' }"
            @click="node.type === 'document' ? $emit('document-click', node.documentId) : $emit('category-click', node.id)"
          />
          <text
            :x="node.x"
            :y="node.y + 5"
            text-anchor="middle"
            fill="#fff"
            font-size="13"
            font-family="Microsoft YaHei, PingFang SC, sans-serif"
            style="pointer-events: none; user-select: none;"
          >{{ truncate(node.name, 8) }}</text>
        </g>
      </g>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'

const emit = defineEmits(['document-click', 'category-click'])

const props = defineProps({
  treeData: {
    type: Array,
    default: () => []
  }
})

const containerRef = ref(null)
const svgRef = ref(null)
const mainGroup = ref(null)
const svgWidth = ref(800)
const svgHeight = ref(600)

const isEmpty = computed(() => !props.treeData || props.treeData.length === 0)

function flattenTree(treeNodes, parentId, depth) {
  const result = []
  treeNodes.forEach(node => {
    result.push({ id: node.id, name: node.name, type: 'category', parentId, depth })
    if (node.documents) {
      node.documents.forEach(doc => {
        result.push({ id: 'doc-' + doc.documentId, documentId: doc.documentId, name: doc.title, type: 'document', parentId: node.id, depth: depth + 1 })
      })
    }
    if (node.children && node.children.length > 0) {
      result.push(...flattenTree(node.children, node.id, depth + 1))
    }
  })
  return result
}

const NODE_W = 120
const NODE_H = 36
const DOC_W = 100
const DOC_H = 28
const H_GAP = 80
const V_GAP = 50

const layoutNodes = computed(() => {
  const allNodes = flattenTree(props.treeData, null, 0)
  if (allNodes.length === 0) return []

  const childrenMap = {}
  allNodes.forEach(n => {
    const pid = n.parentId || '__root__'
    if (!childrenMap[pid]) childrenMap[pid] = []
    childrenMap[pid].push(n)
  })

  const positions = []

  function measure(nodeId) {
    const kids = childrenMap[nodeId] || []
    if (kids.length === 0) return 1
    let total = 0
    kids.forEach(k => { total += measure(k.id) })
    return total
  }

  function place(parentId, baseX, baseY) {
    const kids = childrenMap[parentId] || []
    let y = baseY
    kids.forEach(child => {
      const isDoc = child.type === 'document'
      const w = isDoc ? DOC_W : NODE_W
      const h = isDoc ? DOC_H : NODE_H
      const grandKids = childrenMap[child.id] || []
      const span = grandKids.length > 0 ? measure(child.id) * V_GAP : V_GAP
      const cx = baseX + H_GAP + w / 2
      const cy = y + span / 2
      positions.push({
        id: child.id,
        documentId: child.documentId,
        name: child.name,
        type: child.type,
        x: cx,
        y: cy,
        width: w,
        height: h
      })
      if (grandKids.length > 0) {
        place(child.id, baseX + H_GAP + w, y)
      }
      y += span
    })
  }

  place('__root__', 30, 30)

  const maxX = positions.reduce((m, p) => Math.max(m, p.x + p.width / 2), 0) + 40
  const maxY = positions.reduce((m, p) => Math.max(m, p.y + p.height / 2), 0) + 40
  svgWidth.value = Math.max(maxX, svgWidth.value)
  svgHeight.value = Math.max(maxY, svgHeight.value)

  return positions
})

const linkLines = computed(() => {
  const lines = []
  const allNodes = flattenTree(props.treeData, null, 0)
  layoutNodes.value.forEach(node => {
    const src = allNodes.find(a => a.id === node.id)
    if (!src || !src.parentId) return
    const parent = layoutNodes.value.find(n => n.id === src.parentId)
    if (!parent) return
    lines.push({
      x1: parent.x + parent.width / 2,
      y1: parent.y,
      x2: node.x - node.width / 2,
      y2: node.y
    })
  })
  return lines
})

const truncate = (text, len) => {
  if (!text) return ''
  return text.length > len ? text.substring(0, len) + '..' : text
}

let isDragging = false
let startX = 0
let startY = 0
let translateX = 0
let translateY = 0
let scale = 1

function onMouseDown(e) {
  if (e.target.closest('.node-group')) return
  isDragging = true
  startX = e.clientX - translateX
  startY = e.clientY - translateY
}

function onMouseMove(e) {
  if (!isDragging) return
  translateX = e.clientX - startX
  translateY = e.clientY - startY
  updateTransform()
}

function onMouseUp() {
  isDragging = false
}

function onWheel(e) {
  e.preventDefault()
  const delta = e.deltaY > 0 ? 0.9 : 1.1
  scale = Math.min(2, Math.max(0.3, scale * delta))
  updateTransform()
}

function updateTransform() {
  if (!mainGroup.value) return
  mainGroup.value.setAttribute('transform', `translate(${translateX},${translateY}) scale(${scale})`)
}

let resizeObserver = null

onMounted(() => {
  nextTick(() => {
    if (containerRef.value && svgRef.value) {
      resizeObserver = new ResizeObserver(() => {
        const w = containerRef.value.clientWidth
        const h = containerRef.value.clientHeight
        svgRef.value.setAttribute('width', Math.max(w, svgWidth.value))
        svgRef.value.setAttribute('height', Math.max(h, svgHeight.value))
      })
      resizeObserver.observe(containerRef.value)

      svgRef.value.addEventListener('mousedown', onMouseDown)
      svgRef.value.addEventListener('mousemove', onMouseMove)
      svgRef.value.addEventListener('mouseup', onMouseUp)
      svgRef.value.addEventListener('mouseleave', onMouseUp)
      svgRef.value.addEventListener('wheel', onWheel, { passive: false })
    }
  })
})

onUnmounted(() => {
  if (resizeObserver) resizeObserver.disconnect()
  if (svgRef.value) {
    svgRef.value.removeEventListener('mousedown', onMouseDown)
    svgRef.value.removeEventListener('mousemove', onMouseMove)
    svgRef.value.removeEventListener('mouseup', onMouseUp)
    svgRef.value.removeEventListener('mouseleave', onMouseUp)
    svgRef.value.removeEventListener('wheel', onWheel)
  }
})

watch(() => props.treeData, () => {
  translateX = 0
  translateY = 0
  scale = 1
  nextTick(() => updateTransform())
}, { deep: true })
</script>

<style scoped>
.mindmap-container {
  width: 100%;
  height: 100%;
  position: relative;
  background: var(--bg-content);
  border-radius: 8px;
  overflow: hidden;
  min-height: 400px;
}

.mindmap-svg {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  cursor: grab;
}

.mindmap-link {
  stroke: var(--text-muted);
  transition: stroke 0.3s;
}

.mindmap-svg:active {
  cursor: grabbing;
}

.node-group {
  cursor: default;
}

.mindmap-node {
  cursor: pointer;
  transition: opacity 0.2s;
}

.mindmap-node:hover {
  opacity: 0.8;
}

.doc-node:hover {
  opacity: 0.75;
}

.hint-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  z-index: 10;
}

.hint-tip {
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  background: var(--bg-card);
  color: var(--text-primary);
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 12px;
  z-index: 10;
  opacity: 0.9;
  pointer-events: none;
  box-shadow: 0 1px 4px var(--accent-shadow);
}
</style>