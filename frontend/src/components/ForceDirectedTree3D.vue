<template>
  <div class="force-directed-tree" ref="containerRef">
    <div class="hint-overlay" v-if="isEmpty">
      <el-empty description="暂无目录数据" />
    </div>
    <div class="hint-tip" v-if="!isEmpty">
      <span>🖱️ 拖拽旋转 &nbsp;|&nbsp; 滚轮缩放 &nbsp;|&nbsp; 点击橙方块跳转文档</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as THREE from 'three'
import { OrbitControls } from 'three/examples/jsm/controls/OrbitControls.js'

const emit = defineEmits(['document-click', 'category-click'])

const props = defineProps({
  treeData: {
    type: Array,
    default: () => []
  }
})

const containerRef = ref(null)
const isEmpty = ref(true)
let scene, camera, renderer, controls
let nodes = []
let links = []
let animating = false

const CATEGORY_COLOR = 0x4a90e2
const DOCUMENT_COLOR = 0xff9500

let starfieldGroup

// 文字标签精灵
const createLabelSprite = (text, type) => {
  const canvas = document.createElement('canvas')
  canvas.width = 512; canvas.height = 128
  const ctx = canvas.getContext('2d')
  ctx.shadowColor = type === 'category' ? '#4a90e2' : '#ff9500'
  ctx.shadowBlur = 15
  ctx.fillStyle = type === 'category' ? '#a8d8ff' : '#ffcc80'
  ctx.font = 'bold 42px "Microsoft YaHei","PingFang SC",sans-serif'
  ctx.textAlign = 'center'; ctx.textBaseline = 'middle'
  const d = text.length > 10 ? text.substring(0, 10) + '...' : text
  ctx.fillText(d, 256, 64)
  const tex = new THREE.CanvasTexture(canvas)
  tex.minFilter = THREE.LinearFilter
  const s = new THREE.Sprite(new THREE.SpriteMaterial({
    map: tex, transparent: true, depthTest: false, depthWrite: false
  }))
  s.scale.set(28, 7, 1)
  return s
}

// 光晕精灵（星辰发光）
const createGlowSprite = (colorHex) => {
  const canvas = document.createElement('canvas')
  canvas.width = 128; canvas.height = 128
  const ctx = canvas.getContext('2d')
  const g = ctx.createRadialGradient(64, 64, 0, 64, 64, 64)
  g.addColorStop(0, colorHex)
  g.addColorStop(0.15, colorHex + 'cc')
  g.addColorStop(0.5, colorHex + '33')
  g.addColorStop(1, 'transparent')
  ctx.fillStyle = g; ctx.fillRect(0, 0, 128, 128)
  const tex = new THREE.CanvasTexture(canvas)
  const s = new THREE.Sprite(new THREE.SpriteMaterial({
    map: tex, transparent: true, blending: THREE.AdditiveBlending,
    depthTest: false, depthWrite: false
  }))
  s.scale.set(32, 32, 1)
  return s
}

// 陨石几何体（变形二十面体）
const createMeteoriteGeometry = () => {
  const geo = new THREE.IcosahedronGeometry(4, 1)
  const pos = geo.attributes.position
  for (let i = 0; i < pos.count; i++) {
    const x = pos.getX(i), y = pos.getY(i), z = pos.getZ(i)
    const len = Math.sqrt(x*x + y*y + z*z)
    if (len > 0) {
      const scale = 1 + (Math.random() - 0.5) * 0.5
      pos.setXYZ(i, x * scale, y * scale, z * scale)
    }
  }
  geo.computeVertexNormals()
  return geo
}

// 星空背景
const createStarfield = () => {
  starfieldGroup = new THREE.Group()

  const starsGeo = new THREE.BufferGeometry()
  const starsCount = 1500
  const starsPos = new Float32Array(starsCount * 3)
  const starsCol = new Float32Array(starsCount * 3)
  for (let i = 0; i < starsCount * 3; i += 3) {
    const r = 250 + Math.random() * 350
    const theta = Math.random() * Math.PI * 2
    const phi = Math.acos(2 * Math.random() - 1)
    starsPos[i] = Math.cos(theta) * Math.sin(phi) * r
    starsPos[i+1] = Math.sin(theta) * Math.sin(phi) * r
    starsPos[i+2] = Math.cos(phi) * r
    const c = Math.random()
    if (c < 0.1) { starsCol[i]=0.7; starsCol[i+1]=0.8; starsCol[i+2]=1.0 }
    else if (c < 0.2) { starsCol[i]=1.0; starsCol[i+1]=0.85; starsCol[i+2]=0.6 }
    else { starsCol[i]=0.85; starsCol[i+1]=0.9; starsCol[i+2]=1.0 }
  }
  starsGeo.setAttribute('position', new THREE.BufferAttribute(starsPos, 3))
  starsGeo.setAttribute('color', new THREE.BufferAttribute(starsCol, 3))
  const starsMat = new THREE.PointsMaterial({
    size: 0.8, vertexColors: true, transparent: true,
    blending: THREE.AdditiveBlending, depthWrite: false
  })
  starfieldGroup.add(new THREE.Points(starsGeo, starsMat))

  // 亮星
  const brightGeo = new THREE.BufferGeometry()
  const brightCount = 80
  const brightPos = new Float32Array(brightCount * 3)
  for (let i = 0; i < brightCount * 3; i += 3) {
    const r = 200 + Math.random() * 300
    const theta = Math.random() * Math.PI * 2
    const phi = Math.acos(2 * Math.random() - 1)
    brightPos[i] = Math.cos(theta) * Math.sin(phi) * r
    brightPos[i+1] = Math.sin(theta) * Math.sin(phi) * r
    brightPos[i+2] = Math.cos(phi) * r
  }
  brightGeo.setAttribute('position', new THREE.BufferAttribute(brightPos, 3))
  const brightMat = new THREE.PointsMaterial({
    size: 1.8, color: 0xffffff, transparent: true,
    blending: THREE.AdditiveBlending, depthWrite: false
  })
  starfieldGroup.add(new THREE.Points(brightGeo, brightMat))

  scene.add(starfieldGroup)
}

const flattenTree = (treeNodes, parentId = null) => {
  const result = []
  treeNodes.forEach(node => {
    result.push({ id: node.id, name: node.name, type: 'category', parentId })
    if (node.documents) {
      node.documents.forEach(doc => {
        result.push({ id: doc.documentId, name: doc.title, type: 'document', parentId: node.id })
      })
    }
    if (node.children && node.children.length > 0) {
      result.push(...flattenTree(node.children, node.id))
    }
  })
  return result
}

const init = () => {
  if (!containerRef.value) return

  const allNodes = flattenTree(props.treeData)
  isEmpty.value = allNodes.length === 0
  if (isEmpty.value) return

  const width = containerRef.value.clientWidth || 800
  const height = containerRef.value.clientHeight || 600

  if (width === 0 || height === 0) {
    setTimeout(() => init(), 100)
    return
  }

  scene = new THREE.Scene()
  scene.background = new THREE.Color(0x0a0a2e)

  createStarfield()

  camera = new THREE.PerspectiveCamera(75, width / height, 0.1, 1000)
  camera.position.z = 200

  renderer = new THREE.WebGLRenderer({ antialias: true })
  renderer.setSize(width, height)
  renderer.setPixelRatio(window.devicePixelRatio)
  containerRef.value.appendChild(renderer.domElement)

  controls = new OrbitControls(camera, renderer.domElement)
  controls.enableDamping = true
  controls.dampingFactor = 0.05
  controls.rotateSpeed = 0.5
  controls.zoomSpeed = 0.6

  createGraph(allNodes)
  layout(allNodes)
  animating = true
  animate()

  renderer.domElement.addEventListener('click', handleClick)
  window.addEventListener('resize', onResize)
}

const createGraph = (data) => {
  nodes = []
  links = []

  data.forEach(node => {
    let mesh, labelOffset

    if (node.type === 'category') {
      // 发光星辰：球体 + emissive + 光晕
      const geometry = new THREE.SphereGeometry(5, 32, 32)
      const material = new THREE.MeshStandardMaterial({
        color: CATEGORY_COLOR,
        emissive: CATEGORY_COLOR,
        emissiveIntensity: 0.8,
        roughness: 0.3,
        metalness: 0.1
      })
      mesh = new THREE.Mesh(geometry, material)
      mesh.add(createGlowSprite('#4a90e2'))
      labelOffset = 9
    } else {
      // 陨石：变形二十面体
      const geometry = createMeteoriteGeometry()
      const material = new THREE.MeshStandardMaterial({
        color: DOCUMENT_COLOR,
        roughness: 0.75,
        metalness: 0.2
      })
      mesh = new THREE.Mesh(geometry, material)
      labelOffset = 7.5
    }

    mesh.userData = { id: node.id, name: node.name, type: node.type }
    mesh.position.set(
        (Math.random() - 0.5) * 100,
        (Math.random() - 0.5) * 100,
        (Math.random() - 0.5) * 50
    )

    // 文字标签
    const label = createLabelSprite(node.name, node.type)
    label.position.set(0, labelOffset, 0)
    mesh.add(label)

    scene.add(mesh)
    nodes.push(mesh)

    if (node.parentId !== null) {
      const parentNode = nodes.find(n => n.userData.id === node.parentId)
      if (parentNode) {
        links.push({ source: parentNode, target: mesh })
      }
    }
  })

  // 宇宙风格灯光
  const ambientLight = new THREE.AmbientLight(0x333366, 0.4)
  scene.add(ambientLight)
  const dirLight1 = new THREE.DirectionalLight(0xffffff, 0.3)
  dirLight1.position.set(100, 100, 100)
  scene.add(dirLight1)
  const dirLight2 = new THREE.DirectionalLight(0x6688cc, 0.2)
  dirLight2.position.set(-100, -50, -50)
  scene.add(dirLight2)
  const pointLight = new THREE.PointLight(0x4466aa, 0.5, 300)
  pointLight.position.set(0, 0, 0)
  scene.add(pointLight)
}

const layout = (data) => {
  const repulsion = 50
  const attraction = 0.5
  const iterations = 100

  for (let i = 0; i < iterations; i++) {
    nodes.forEach(node => {
      nodes.forEach(other => {
        if (node !== other) {
          const dx = other.position.x - node.position.x
          const dy = other.position.y - node.position.y
          const dz = other.position.z - node.position.z
          const dist = Math.sqrt(dx * dx + dy * dy + dz * dz) || 1
          if (dist < 20) {
            const force = repulsion / dist
            node.position.x -= (dx / dist) * force
            node.position.y -= (dy / dist) * force
            node.position.z -= (dz / dist) * force
          }
        }
      })
    })

    links.forEach(link => {
      const dx = link.target.position.x - link.source.position.x
      const dy = link.target.position.y - link.source.position.y
      const dz = link.target.position.z - link.source.position.z
      const dist = Math.sqrt(dx * dx + dy * dy + dz * dz) || 1
      link.target.position.x -= (dx / dist) * dist * 0.01 * attraction
      link.target.position.y -= (dy / dist) * dist * 0.01 * attraction
      link.target.position.z -= (dz / dist) * dist * 0.01 * attraction
      link.source.position.x += (dx / dist) * dist * 0.01 * attraction
      link.source.position.y += (dy / dist) * dist * 0.01 * attraction
      link.source.position.z += (dz / dist) * dist * 0.01 * attraction
    })
  }

  links.forEach(link => {
    const points = [
      new THREE.Vector3(link.source.position.x, link.source.position.y, link.source.position.z),
      new THREE.Vector3(link.target.position.x, link.target.position.y, link.target.position.z)
    ]
    const geometry = new THREE.BufferGeometry().setFromPoints(points)
    const material = new THREE.LineBasicMaterial({ color: 0x44ccff, transparent: true, opacity: 0.9 })
    const line = new THREE.Line(geometry, material)
    scene.add(line)
  })
}

const handleClick = (event) => {
  if (!renderer) return
  const rect = renderer.domElement.getBoundingClientRect()
  const x = ((event.clientX - rect.left) / rect.width) * 2 - 1
  const y = -((event.clientY - rect.top) / rect.height) * 2 + 1

  const raycaster = new THREE.Raycaster()
  raycaster.setFromCamera(new THREE.Vector2(x, y), camera)
  const intersects = raycaster.intersectObjects(nodes)

  if (intersects.length > 0) {
    const obj = intersects[0].object
    if (obj.userData.type === 'document') {
      emit('document-click', obj.userData.id)
    } else if (obj.userData.type === 'category') {
      emit('category-click', obj.userData.id)
    }
  }
}

const animate = () => {
  if (!animating) return
  requestAnimationFrame(animate)

  if (starfieldGroup) {
    starfieldGroup.rotation.y += 0.0001
    starfieldGroup.rotation.x += 0.00005
  }

  controls.update()
  renderer.render(scene, camera)
}

const onResize = () => {
  if (!containerRef.value || !renderer || !camera) return
  const width = containerRef.value.clientWidth
  const height = containerRef.value.clientHeight
  if (width === 0 || height === 0) return
  camera.aspect = width / height
  camera.updateProjectionMatrix()
  renderer.setSize(width, height)
}

const clear = () => {
  animating = false
  if (renderer && containerRef.value && renderer.domElement) {
    renderer.domElement.removeEventListener('click', handleClick)
    containerRef.value.removeChild(renderer.domElement)
    renderer.dispose()
    renderer = null
  }
  window.removeEventListener('resize', onResize)
}

onMounted(() => {
  nextTick(() => {
    init()
  })
})

onUnmounted(() => {
  clear()
})

watch(() => props.treeData, () => {
  clear()
  nextTick(() => {
    init()
  })
}, { deep: true })
</script>

<style scoped>
.force-directed-tree {
  width: 100%;
  height: 100%;
  position: relative;
  background: #0a0a2e;
  border-radius: 8px;
  overflow: hidden;
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
  background: rgba(0, 0, 0, 0.55);
  color: white;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 12px;
  z-index: 10;
  opacity: 0.85;
  pointer-events: none;
}
</style>