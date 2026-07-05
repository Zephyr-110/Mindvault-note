<template>
  <div class="dashboard" :class="'theme-' + currentTheme" @keydown="handleGlobalKeydown" tabindex="0">
    <!-- 顶部导航 -->
    <el-header class="header">
      <div class="header-left">
        <h2 class="logo">🧠 MindVault</h2>
        <el-dialog v-model="trashVisible" title="🗑️ 回收站" width="650px" class="trash-dialog">
      <div v-if="trashCategories.length === 0 && trashDocuments.length === 0" class="empty-tip">
        <span class="empty-icon">🗑️</span>
        <p>回收站是空的</p>
      </div>
      <template v-else>
        <div class="trash-toolbar">
          <span class="trash-count">共 {{ trashCategories.length + trashDocuments.length }} 项</span>
          <el-button type="danger" size="small" @click="handleClearTrash" :loading="clearingTrash">
            🗑️ 清空回收站
          </el-button>
        </div>
        <div v-if="trashCategories.length > 0" class="trash-section">
          <h4>📁 已删除的目录 ({{ trashCategories.length }})</h4>
          <div v-for="cat in trashCategories" :key="cat.id" class="trash-item">
            <div class="trash-item-left">
              <span class="trash-icon">📁</span>
              <div class="trash-item-info">
                <span class="trash-name">{{ cat.name }}</span>
              </div>
            </div>
            <div class="trash-actions">
              <el-button link size="small" type="primary" @click="handleRestoreCategory(cat.id)">↩ 恢复</el-button>
              <el-button link size="small" type="danger" @click="handlePermanentDeleteCategory(cat.id)">🗑️ 彻底删除</el-button>
            </div>
          </div>
        </div>
        <div v-if="trashDocuments.length > 0" class="trash-section">
          <h4>📄 已删除的文档 ({{ trashDocuments.length }})</h4>
          <div v-for="doc in trashDocuments" :key="doc.id" class="trash-item">
            <div class="trash-item-left">
              <span class="trash-icon">📄</span>
              <div class="trash-item-info">
                <span class="trash-name">{{ doc.title }}</span>
              </div>
            </div>
            <div class="trash-actions">
              <el-button link size="small" type="primary" @click="handleRestoreDocument(doc.id)">↩ 恢复</el-button>
              <el-button link size="small" type="danger" @click="handlePermanentDeleteDocument(doc.id)">🗑️ 彻底删除</el-button>
            </div>
          </div>
        </div>
      </template>
    </el-dialog>
  </div>
      <div class="header-right">
        <div class="user-profile-trigger" @click="openProfile">
          <el-avatar :size="36" :src="profile.avatar || ''" class="header-avatar">
            {{ profile.nickname ? profile.nickname.charAt(0) : currentUser.username.charAt(0) }}
          </el-avatar>
          <span class="header-nickname">{{ profile.nickname || currentUser.username }}</span>
        </div>
        <el-button type="" size="small" circle @click="openTrash">🗑️</el-button>
        <el-button type="danger" size="small" @click="logout">退出</el-button>
      </div>
    </el-header>

    <div class="dashboard-body">
      <el-aside :width="sidebarWidth + 'px'" class="dash-sidebar" :class="{ collapsed: isSidebarCollapsed }">
        <div v-show="!isSidebarCollapsed" class="sidebar-inner">
        <div class="view-mode-bar">
          <el-tooltip content="普通视图" placement="bottom">
            <el-button :type="viewMode === 'normal' ? 'primary' : ''" size="small" circle @click="viewMode = 'normal'">📄</el-button>
          </el-tooltip>
          <el-tooltip content="平面导图" placement="bottom">
            <el-button :type="viewMode === 'mindmap' ? 'primary' : ''" size="small" circle @click="viewMode = 'mindmap'">🗺️</el-button>
          </el-tooltip>
          <el-tooltip content="3D导图" placement="bottom">
            <el-button :type="viewMode === '3d' ? 'primary' : ''" size="small" circle @click="viewMode = '3d'">🌳</el-button>
          </el-tooltip>
        </div>
        <div class="sidebar-header">
          <h3>📁 目录</h3>
          <div class="header-actions">
            <el-dropdown trigger="click">
              <el-button type="primary" size="small">+ 新建</el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item @click="showCreateCategoryDialog()">新建目录</el-dropdown-item>
                  <el-dropdown-item @click="showCreateDocumentDialog()" :disabled="!currentCategoryId">新建文档</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </div>

        <div class="sidebar-search" v-if="currentCategoryId">
          <el-input v-model="searchKeyword" placeholder="搜索子树文档..." size="small" clearable @input="onSearchInput" @clear="onSearchClear">
            <template #prefix>🔍</template>
          </el-input>
        </div>

        <div class="tree-wrapper">
          <CategoryTree
              :tree-data="categoryTreeWithDocs"
              @select="handleCategorySelect"
              @document-select="selectDocumentFromTree"
              @create-sub="showCreateCategoryDialog"
              @edit="showEditCategoryDialog"
              @delete="handleDeleteCategory"
              @delete-document="handleDeleteDocument"
          />
        </div>
        </div>
      </el-aside>
      <div class="resize-handle" @mousedown="startResize" :class="{ dragging: isDragging }">
        <div class="collapse-toggle" @click.stop="toggleSidebar">
          {{ isSidebarCollapsed ? '▶' : '◀' }}
        </div>
      </div>
      <el-main class="content-area">
        <!-- 普通视图 -->
        <template v-if="viewMode === 'normal'">
          <div class="global-search-bar" v-if="!isEditing">
            <el-input v-model="globalSearchKeyword" placeholder="全局搜索正文内容..." size="large" clearable @input="onGlobalSearchInput" @clear="clearGlobalSearch">
              <template #prefix>🌐</template>
            </el-input>
          </div>

          <div v-if="globalSearchResults.length > 0" class="global-search-results">
            <div v-for="doc in globalSearchResults" :key="doc.id" class="search-result-item" @click="openDocumentFromSearch(doc)">
              <h4>{{ doc.title }}</h4>
              <p class="result-preview" v-html="highlightKeyword(doc.preview || '', globalSearchKeyword)"></p>
              <span class="result-meta">{{ formatDate(doc.updateTime) }}</span>
            </div>
          </div>

          <div v-if="currentCategoryId && globalSearchResults.length === 0 && !selectedDocId && !isEditing">
            <div class="top-bar">
              <div class="top-bar-title">{{ currentCategoryName }}</div>
              <div class="top-actions">
                <el-button type="primary" size="small" @click="showCreateDocumentDialog">+ 新建文档</el-button>
                <el-button size="small" @click="showCreateCategoryDialog(currentCategoryId)">+ 新建子目录</el-button>
              </div>
            </div>

            <div v-if="subCategories.length > 0" class="subcategory-section">
              <h3>📁 子目录</h3>
              <div class="subcategory-grid">
                <div v-for="cat in subCategories" :key="cat.id" class="subcategory-card" @click="handleCategorySelect(cat.id, cat.name)">
                  <span class="folder-icon">📁</span>
                  <span class="folder-name">{{ cat.name }}</span>
                </div>
              </div>
            </div>

            <div class="doc-section">
              <h3>📄 文档</h3>
              <div class="doc-list" v-if="pagedDocuments.length > 0">
                <div v-for="doc in pagedDocuments" :key="doc.id" class="doc-item" :class="{ active: selectedDocId === doc.id }" @click="selectDocument(doc)">
                  <div class="doc-item-content">
                    <span class="doc-title">{{ doc.title }}</span>
                    <span class="doc-preview" v-if="doc.preview" v-html="highlightKeyword(doc.preview, searchKeyword)"></span>
                  </div>
                  <div class="doc-actions">
                    <el-button link size="small" @click.stop="editDocument(doc)">✏️</el-button>
                    <el-button link size="small" type="danger" @click.stop="handleDeleteDocument(doc.id)">🗑️</el-button>
                  </div>
                </div>
              </div>
              <div v-else class="empty-tip">暂无文档</div>
              <div v-if="filteredDocuments.length > pageSize" class="pagination-box">
                <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="filteredDocuments.length" layout="prev, pager, next" small />
              </div>
            </div>
          </div>

          <div v-if="selectedDocId && !isEditing && globalSearchResults.length === 0" class="view-mode">
            <div class="doc-header">
              <h2>{{ currentDocument?.title }}</h2>
              <div class="doc-header-actions">
                <el-button type="primary" size="small" @click="startEditing">编辑</el-button>
                <el-button size="small" @click="backToFolder">返回目录</el-button>
              </div>
              <div class="doc-meta">
                <el-tag v-for="tag in currentDocument?.tags" :key="tag.id" size="small" effect="plain">{{ tag.name }}</el-tag>
                <span class="time">{{ formatDate(currentDocument?.createTime) }}</span>
              </div>
            </div>
            <div class="doc-content" v-html="renderMarkdown(currentDocument?.content)" @click="handleContentClick"></div>
          </div>

          <div v-if="isEditing" class="edit-mode">
            <div class="edit-header">
              <el-input v-model="documentForm.title" placeholder="输入标题..." size="large" style="flex: 1; margin-right: 10px;" @keyup.enter="submitDocument" />
              <div class="edit-actions">
                <el-button @click="cancelEdit">取消</el-button>
                <el-button type="primary" @click="submitDocument">保存</el-button>
              </div>
            </div>

            <div class="edit-toolbar">
              <el-tooltip content="图片库" placement="top">
                <el-button size="small" circle @click="imageDrawerVisible = true">🖼️</el-button>
              </el-tooltip>
              <span class="toolbar-hint">粘贴图片 Ctrl+V 或拖拽到编辑区</span>
              <span v-if="activeImageForZoom" class="toolbar-hint zoom-hint">🖱️ 上下滚轮改变图片大小</span>
            </div>

            <div ref="editorRef" class="content-editable" contenteditable="true" @input="onEditorInput" @paste="onPaste" @keydown.tab="handleTab" @click="handleImageClick" @wheel="handleWheelZoomImage" placeholder="开始写作..."></div>
            <div class="tag-selector">
              <el-select v-model="documentForm.tagIds" multiple placeholder="选择标签（可选）" style="width: 100%">
                <el-option v-for="tag in tags" :key="tag.id" :label="tag.name" :value="tag.id" />
              </el-select>
            </div>
          </div>

          <el-drawer v-model="imageDrawerVisible" title="📷 图片库" size="300px">
            <div class="image-drawer-content">
              <div v-if="imageLibrary.length === 0" class="empty-tip">暂无图片</div>
              <div v-for="(url, idx) in imageLibrary" :key="idx" class="image-drawer-item" @click="insertImage(url)">
                <img :src="url" alt="图片" />
              </div>
            </div>
          </el-drawer>

          <div v-if="previewImageUrl" class="image-preview-mask" @click.self="previewImageUrl = null">
            <div class="image-preview-container">
              <img :src="previewImageUrl" class="preview-image" />
              <el-button class="close-btn" circle @click="previewImageUrl = null">✕</el-button>
            </div>
          </div>

          <div v-if="!currentCategoryId && !selectedDocId && globalSearchResults.length === 0 && !isEditing" class="empty-state">
            <div class="empty-icon">📝</div>
            <p>在左侧目录树中选择一个文件夹</p>
            <p class="hint">或使用上方全局搜索框搜索正文</p>
          </div>
        </template>

        <!-- 平面导图视图 -->
        <template v-else-if="viewMode === 'mindmap'">
          <div v-if="focusedCategoryId" class="focus-bar">
            <el-button size="small" @click="focusedCategoryId = null">↩ 返回全部</el-button>
            <span class="focus-label">当前聚焦：{{ focusedCategoryName }}</span>
          </div>
          <MindMapView :tree-data="focusedTreeFor3D" @document-click="handle3DDocumentClick" @category-click="handle3DCategoryClick" />
        </template>

        <!-- 3D 导图视图 -->
        <template v-else>
          <div v-if="focusedCategoryId" class="focus-bar">
            <el-button size="small" @click="focusedCategoryId = null">↩ 返回全部</el-button>
            <span class="focus-label">当前聚焦：{{ focusedCategoryName }}</span>
          </div>
          <ForceDirectedTree3D :tree-data="focusedTreeFor3D" @document-click="handle3DDocumentClick" @category-click="handle3DCategoryClick" />
        </template>
      </el-main>
    </div>

    <div class="shortcut-hint">
      <el-tooltip placement="top" effect="light">
        <template #content>
          <div class="shortcut-list">
            <div><kbd>Ctrl+N</kbd> 新建文档</div>
            <div><kbd>Ctrl+S</kbd> 保存</div>
            <div><kbd>Ctrl+E</kbd> 编辑</div>
            <div><kbd>Delete</kbd> 删除文档</div>
            <div><kbd>Esc</kbd> 取消</div>
            <div><kbd>Enter</kbd> 标题栏保存</div>
            <div><kbd>Ctrl+↑↓</kbd> 切换文档</div>
          </div>
        </template>
        <el-button circle size="small" class="shortcut-btn">⌨️</el-button>
      </el-tooltip>
    </div>

    <el-dialog v-model="profileVisible" title="👤 个人资料" width="520px" class="profile-dialog" :close-on-click-modal="false" @close="handleProfileClose">
      <div class="profile-content">
        <div class="profile-avatar-section">
          <el-avatar :size="80" :src="profileForm.avatar || ''" class="profile-avatar">
            {{ profileForm.nickname ? profileForm.nickname.charAt(0) : currentUser.username.charAt(0) }}
          </el-avatar>
          <div class="profile-avatar-actions" v-if="profileEditing">
            <el-button size="small" @click="viewAvatar">👁️ 查看头像</el-button>
            <el-upload :show-file-list="false" :before-upload="handleAvatarUpload" accept="image/*">
              <el-button size="small">📷 更换头像</el-button>
            </el-upload>
          </div>
          <div class="profile-avatar-actions" v-else>
            <el-button size="small" @click="viewAvatar">👁️ 查看头像</el-button>
          </div>
        </div>
        <el-divider />
        <el-form :model="profileForm" label-width="70px" :disabled="!profileEditing">
          <el-form-item label="昵称">
            <el-input v-model="profileForm.nickname" placeholder="设置昵称" />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="profileForm.phone" placeholder="设置手机号" />
          </el-form-item>
          <el-form-item label="邮箱">
            <el-input v-model="profileForm.email" placeholder="设置邮箱" />
          </el-form-item>
          <el-form-item label="性别">
            <el-select v-model="profileForm.gender" placeholder="选择性别" style="width: 100%">
              <el-option label="保密" :value="0" />
              <el-option label="男" :value="1" />
              <el-option label="女" :value="2" />
            </el-select>
          </el-form-item>
          <el-form-item label="年龄">
            <el-input-number v-model="profileForm.age" :min="1" :max="150" style="width: 100%" />
          </el-form-item>
          <el-form-item label="地区">
            <el-input v-model="profileForm.region" placeholder="设置地区" />
          </el-form-item>
          <el-form-item label="个性签名">
            <el-input v-model="profileForm.bio" type="textarea" :rows="3" placeholder="写一句个性签名..." />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button v-if="!profileEditing" type="primary" size="large" @click="startProfileEdit" style="width: 100%">
          ✏️ 编辑资料
        </el-button>
        <template v-else>
          <el-button @click="cancelProfileEdit">取消</el-button>
          <el-button type="primary" @click="submitProfile" :loading="profileSaving">💾 保存更改</el-button>
        </template>
      </template>
    </el-dialog>

    <el-dialog v-model="avatarPreviewVisible" title="头像预览" width="400px">
      <div style="text-align: center">
        <img :src="previewAvatarUrl" style="max-width: 100%; max-height: 400px; border-radius: 8px" />
      </div>
    </el-dialog>

    <el-dialog v-model="categoryDialogVisible" :title="categoryDialogTitle" width="400px" @keyup.enter="submitCategory">
      <el-form :model="categoryForm">
        <el-form-item label="目录名称">
          <el-input v-model="categoryForm.name" placeholder="输入目录名称" ref="categoryNameInputRef" @keyup.enter="submitCategory" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="categoryDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCategory">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'
import CategoryTree from '../components/CategoryTree.vue'
import ForceDirectedTree3D from '../components/ForceDirectedTree3D.vue'
import MindMapView from '../components/MindMapView.vue'
import MarkdownIt from 'markdown-it'

const router = useRouter()
const route = useRoute()
const md = new MarkdownIt({ html: true, breaks: true })

const currentUser = ref(JSON.parse(localStorage.getItem('userInfo')) || {})
const categoryTree = ref([])
const tags = ref([])
const currentCategoryId = ref(null)
const currentCategoryName = ref('')
const selectedDocId = ref(null)
const isEditing = ref(false)
const editorRef = ref(null)
const categoryNameInputRef = ref(null)
const previewImageUrl = ref(null)
const imageDrawerVisible = ref(false)
const imageLibrary = ref([])
const activeImageForZoom = ref(null)

const allDocuments = ref([])
const sidebarWidth = ref(300)
const savedSidebarWidth = ref(300)
const isSidebarCollapsed = ref(false)
const isDragging = ref(false)
const searchKeyword = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
let searchTimer = null
const globalSearchKeyword = ref('')
const globalSearchResults = ref([])
let globalSearchTimer = null

const categoryDialogVisible = ref(false)
const categoryDialogTitle = ref('新建目录')
const categoryForm = reactive({ name: '', parentId: null })
const documentForm = reactive({ title: '', content: '', tagIds: [], categoryId: null })
const currentDocument = ref(null)

const focusedCategoryId = ref(null)
const focusedCategoryName = ref('')

const viewMode = ref('normal')

const currentTheme = ref('default')
const trashVisible = ref(false)
const trashCategories = ref([])
const trashDocuments = ref([])
const clearingTrash = ref(false)

const profile = ref({})
const profileVisible = ref(false)
const profileEditing = ref(false)
const profileSaving = ref(false)
const profileOriginal = ref('')
const profileForm = reactive({
  nickname: '', avatar: '', phone: '', email: '',
  gender: null, age: null, region: '', bio: ''
})
const avatarPreviewVisible = ref(false)
const previewAvatarUrl = ref('')

const onThemeChange = async (val) => {
  currentTheme.value = val
  document.documentElement.setAttribute('data-theme', val)
  try {
    await api.updateSetting({ settingKey: 'appearance.theme', settingValue: val })
  } catch (e) { /* ignore */ }
}

const applyTheme = async () => {
  try {
    const res = await api.getSettings({ category: 'appearance' })
    if (res.code === 200 && res.data && res.data['appearance.theme']) {
      currentTheme.value = res.data['appearance.theme']
    }
  } catch (e) { /* ignore */ }
  document.documentElement.setAttribute('data-theme', currentTheme.value)
}

const subCategories = computed(() => {
  if (!currentCategoryId.value) return []
  const node = findNodeById(categoryTree.value, currentCategoryId.value)
  return node?.children || []
})

const filteredDocuments = computed(() => allDocuments.value.filter(doc => !searchKeyword.value || doc.title?.toLowerCase().includes(searchKeyword.value.toLowerCase())))

const pagedDocuments = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  return filteredDocuments.value.slice(start, start + pageSize.value)
})

const categoryTreeWithDocs = computed(() => {
  const addDocs = (node) => {
    const children = []
    if (node.children) {
      children.push(...node.children.map(addDocs))
    }
    const docs = allDocuments.value.filter(d => d.categoryId === node.id)
    docs.forEach(doc => {
      children.push({
        id: doc.id,
        name: doc.title,
        documentId: doc.id
      })
    })
    return { ...node, children }
  }
  return categoryTree.value.map(addDocs)
})

const flattenTreeFor3D = computed(() => {
  const process = (node) => {
    const result = { id: node.id, name: node.name, children: [], documents: [] }
    if (node.children) result.children = node.children.map(process)
    const docs = allDocuments.value.filter(d => d.categoryId === node.id)
    result.documents = docs.map(d => ({ documentId: d.id, title: d.title }))
    return result
  }
  return categoryTree.value.map(process)
})

const focusedTreeFor3D = computed(() => {
  if (!focusedCategoryId.value) return flattenTreeFor3D.value
  const findAndExtract = (nodes) => {
    for (const node of nodes) {
      if (node.id === focusedCategoryId.value) return [node]
      if (node.children) {
        const found = findAndExtract(node.children)
        if (found) return found
      }
    }
    return null
  }
  return findAndExtract(flattenTreeFor3D.value) || flattenTreeFor3D.value
})

const handle3DCategoryClick = (categoryId) => {
  const node = findNodeById(categoryTree.value, categoryId)
  focusedCategoryId.value = categoryId
  focusedCategoryName.value = node?.name || ''
}

const handle3DDocumentClick = (documentId) => {
  const doc = allDocuments.value.find(d => d.id === documentId)
  if (doc) {
    viewMode.value = 'normal'
    loadAllDocuments()
    nextTick(() => selectDocument(doc))
  }
}

const selectDocumentFromTree = (documentId) => {
  const doc = allDocuments.value.find(d => d.id === documentId)
  if (doc) selectDocument(doc)
}

const findNodeById = (nodes, id) => {
  for (let node of nodes) {
    if (node.id === id) return node
    if (node.children) { const found = findNodeById(node.children, id); if (found) return found }
  }
  return null
}

const getDescendantIds = (nodeId) => {
  const ids = [nodeId]
  const node = findNodeById(categoryTree.value, nodeId)
  if (node?.children) node.children.forEach(child => ids.push(...getDescendantIds(child.id)))
  return ids
}

const markdownToHtml = (mk) => md.render(mk)

const htmlToMarkdown = (html) => {
  let t = html
  t = t.replace(/<img[^>]+src="([^"]+)"[^>]*>/gi, '\n![图片]($1)\n')
  t = t.replace(/<b>([\s\S]*?)<\/b>/gi, '**$1**')
  t = t.replace(/<strong>([\s\S]*?)<\/strong>/gi, '**$1**')
  t = t.replace(/<i>([\s\S]*?)<\/i>/gi, '*$1*')
  t = t.replace(/<em>([\s\S]*?)<\/em>/gi, '*$1*')
  t = t.replace(/<br\s*\/?>/gi, '\n')
  t = t.replace(/<\/p>/gi, '\n')
  t = t.replace(/<p>/gi, '')
  t = t.replace(/<[^>]+>/g, '')
  return t.trim()
}

const refreshImageLibrary = () => {
  if (!isEditing.value || !editorRef.value) return
  const html = editorRef.value.innerHTML || ''
  const regex = /<img[^>]+src="([^"]+)"/g
  const urls = []
  let m
  while ((m = regex.exec(html)) !== null) urls.push(m[1])
  imageLibrary.value = urls
}

const insertImage = (url) => {
  if (!editorRef.value) return
  editorRef.value.focus()
  const img = document.createElement('img')
  img.src = url
  img.style.maxWidth = '100%'
  img.style.width = '50%'
  img.style.cursor = 'pointer'
  const sel = window.getSelection()
  if (sel.rangeCount) {
    const range = sel.getRangeAt(0)
    range.insertNode(img)
    range.setStartAfter(img)
    range.collapse(true)
    sel.removeAllRanges()
    sel.addRange(range)
  } else {
    editorRef.value.appendChild(img)
  }
  imageDrawerVisible.value = false
  onEditorInput()
}

const onEditorInput = () => {
  if (!editorRef.value) return
  documentForm.content = htmlToMarkdown(editorRef.value.innerHTML)
  nextTick(refreshImageLibrary)
}

const onPaste = async (e) => {
  const cd = e.clipboardData
  if (!cd) return
  const files = cd.files
  if (files && files.length > 0) {
    const file = files[0]
    if (file.type.startsWith('image/')) {
      e.preventDefault()
      try {
        ElMessage.info('正在上传图片...')
        const res = await api.uploadFile(file)
        if (res.code === 200) { insertImage(res.data); ElMessage.success('图片上传成功') }
        else ElMessage.error(res.message || '上传失败')
      } catch (err) { ElMessage.error('图片上传失败') }
      return
    }
  }
  const items = cd.items
  if (!items) return
  let hasImg = false
  for (const it of items) { if (it.type.startsWith('image/')) { hasImg = true; break } }
  if (!hasImg) {
    const html = cd.getData('text/html')
    if (html && /<img[^>]+src="data:image\//.test(html)) {
      e.preventDefault()
      ElMessage.warning('请将图片保存到本地后再粘贴，或使用截图工具截图后直接粘贴')
      return
    }
    return
  }
  e.preventDefault()
  for (const it of items) {
    if (it.type.startsWith('image/')) {
      const file = it.getAsFile()
      if (!file) continue
      try {
        ElMessage.info('正在上传图片...')
        const res = await api.uploadFile(file)
        if (res.code === 200) { insertImage(res.data); ElMessage.success('图片上传成功') }
        else ElMessage.error(res.message || '上传失败')
      } catch (err) { ElMessage.error('图片上传失败') }
      break
    }
  }
}

const handleTab = (e) => {
  e.preventDefault()
  if (!isEditing.value || !editorRef.value) return
  const sel = window.getSelection()
  if (!sel.rangeCount) return
  const range = sel.getRangeAt(0)
  const tabNode = document.createTextNode('\t')
  range.insertNode(tabNode)
  range.setStartAfter(tabNode)
  range.collapse(true)
  sel.removeAllRanges()
  sel.addRange(range)
  onEditorInput()
}

const handleImageClick = (e) => {
  if (!isEditing.value) return
  const img = e.target.closest('img')
  if (!img || !editorRef.value?.contains(img)) return
  e.stopPropagation()
  if (activeImageForZoom.value === img) {
    activeImageForZoom.value = null
    img.style.outline = 'none'
    ElMessage.info('图片缩放模式已关闭')
  } else {
    if (activeImageForZoom.value) {
      activeImageForZoom.value.style.outline = 'none'
    }
    activeImageForZoom.value = img
    img.style.outline = '2px solid #409eff'
    ElMessage.info('上下滑动滚轮改变图片大小')
  }
}

const handleWheelZoomImage = (e) => {
  if (!isEditing.value || !activeImageForZoom.value) return
  const img = activeImageForZoom.value
  if (img !== e.target && !img.contains(e.target)) return
  e.preventDefault()
  let currentWidth = parseFloat(img.style.width) || 50
  const delta = e.deltaY > 0 ? -5 : 5
  currentWidth = Math.min(100, Math.max(10, currentWidth + delta))
  img.style.width = currentWidth + '%'
}

const handleGlobalKeydown = (e) => {
  const isFocused = document.activeElement?.tagName === 'INPUT' || document.activeElement?.tagName === 'TEXTAREA'
  if (e.ctrlKey && e.key === 'n') { e.preventDefault(); if (currentCategoryId.value) showCreateDocumentDialog(); else ElMessage.warning('请先选择一个目录') }
  if (e.ctrlKey && e.key === 's') { e.preventDefault(); if (isEditing.value) submitDocument() }
  if (e.ctrlKey && e.key === 'e') { e.preventDefault(); if (selectedDocId.value && !isEditing.value) startEditing() }
  if (e.key === 'Delete' && !isFocused) { e.preventDefault(); if (selectedDocId.value && !isEditing.value) handleDeleteDocument(selectedDocId.value) }
  if (e.key === 'Escape') { if (isEditing.value) { cancelEdit(); activeImageForZoom.value = null } else if (categoryDialogVisible.value) categoryDialogVisible.value = false }
  if (e.ctrlKey && e.key === 'ArrowUp') { e.preventDefault(); navigateDocument(-1) }
  if (e.ctrlKey && e.key === 'ArrowDown') { e.preventDefault(); navigateDocument(1) }
}

const navigateDocument = (dir) => {
  if (!allDocuments.value.length) return
  let idx = allDocuments.value.findIndex(d => d.id === selectedDocId.value)
  if (idx === -1) idx = 0
  idx += dir
  if (idx < 0) idx = allDocuments.value.length - 1
  if (idx >= allDocuments.value.length) idx = 0
  selectDocument(allDocuments.value[idx])
}

onMounted(async () => {
  loadCategoryTree()
  loadTags()
  loadProfile()
  await applyTheme()
  document.addEventListener('keydown', handleGlobalKeydown)

  const docId = route.query.docId
  if (docId) {
    nextTick(() => {
      loadAllDocuments().then(() => {
        const doc = allDocuments.value.find(d => d.id == docId)
        if (doc) selectDocument(doc)
      })
    })
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', handleGlobalKeydown)
})

const loadCategoryTree = async () => {
  try { const res = await api.getCategoryTree({ userId: currentUser.value.id }); if (res.code === 200) categoryTree.value = res.data || [] } catch (err) { ElMessage.error('加载目录失败') }
}

const loadTags = async () => {
  try { const res = await api.listTags({ userId: currentUser.value.id }); if (res.code === 200) tags.value = res.data } catch (err) { ElMessage.error('加载标签失败') }
}

const loadAllDocuments = async () => {
  if (!currentCategoryId.value) return
  try {
    const ids = getDescendantIds(currentCategoryId.value)
    const promises = ids.map(id => api.listDocuments({ userId: currentUser.value.id, categoryId: id, keyword: searchKeyword.value || undefined, page: 1, size: 1000 }).then(res => res.code === 200 ? res.data.list || [] : []))
    const results = await Promise.all(promises)
    const unique = results.flat().filter((doc, idx, self) => self.findIndex(d => d.id === doc.id) === idx)
    allDocuments.value = unique
    currentPage.value = 1
  } catch (err) { ElMessage.error('加载文档失败') }
}

const onSearchInput = () => { clearTimeout(searchTimer); searchTimer = setTimeout(() => { loadAllDocuments() }, 400) }
const onSearchClear = () => { searchKeyword.value = ''; loadAllDocuments() }

const onGlobalSearchInput = () => {
  clearTimeout(globalSearchTimer)
  globalSearchTimer = setTimeout(() => {
    if (!globalSearchKeyword.value.trim()) { globalSearchResults.value = []; return }
    performGlobalSearch()
  }, 400)
}

const performGlobalSearch = async () => {
  try { const res = await api.listDocuments({ userId: currentUser.value.id, keyword: globalSearchKeyword.value, page: 1, size: 20 }); if (res.code === 200) globalSearchResults.value = res.data.list || [] } catch (err) { ElMessage.error('全局搜索失败') }
}

const clearGlobalSearch = () => { globalSearchKeyword.value = ''; globalSearchResults.value = [] }

const openDocumentFromSearch = (doc) => { currentCategoryId.value = doc.categoryId; currentCategoryName.value = ''; selectedDocId.value = doc.id; isEditing.value = false; globalSearchResults.value = []; selectDocument(doc); loadAllDocuments() }

const handleCategorySelect = async (id, name) => { currentCategoryId.value = id; currentCategoryName.value = name; selectedDocId.value = null; isEditing.value = false; searchKeyword.value = ''; globalSearchResults.value = []; globalSearchKeyword.value = ''; await loadAllDocuments() }

const toggleSidebar = () => {
  if (isSidebarCollapsed.value) {
    sidebarWidth.value = savedSidebarWidth.value
    isSidebarCollapsed.value = false
  } else {
    savedSidebarWidth.value = sidebarWidth.value
    sidebarWidth.value = 0
    isSidebarCollapsed.value = true
  }
}

const startResize = (e) => { isDragging.value = true; document.addEventListener('mousemove', onResize); document.addEventListener('mouseup', stopResize); e.preventDefault() }

const onResize = (e) => { if (!isDragging.value) return; const container = document.querySelector('.dashboard .dashboard-body'); const rect = container.getBoundingClientRect(); const w = e.clientX - rect.left; if (w >= 200 && w <= 600) sidebarWidth.value = w }

const stopResize = () => { isDragging.value = false; document.removeEventListener('mousemove', onResize); document.removeEventListener('mouseup', stopResize) }

const showCreateCategoryDialog = (parentId = null) => { categoryForm.name = ''; categoryForm.parentId = parentId; categoryDialogTitle.value = parentId ? '新建子目录' : '新建目录'; categoryDialogVisible.value = true; nextTick(() => categoryNameInputRef.value?.focus()) }

const submitCategory = async () => {
  if (!categoryForm.name.trim()) return ElMessage.warning('目录名称不能为空')
  try { const data = { name: categoryForm.name, userId: currentUser.value.id }; if (categoryForm.parentId !== null) data.parentId = categoryForm.parentId; const res = await api.createCategory(data); if (res.code === 200) { ElMessage.success('创建成功'); categoryDialogVisible.value = false; await loadCategoryTree(); if (currentCategoryId.value) loadAllDocuments() } } catch (err) { ElMessage.error('创建失败') }
}

const showEditCategoryDialog = (cat) => { categoryForm.name = cat.name; categoryForm.parentId = cat.parentId; categoryDialogTitle.value = '编辑目录'; categoryDialogVisible.value = true; nextTick(() => categoryNameInputRef.value?.focus()) }

const handleDeleteCategory = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该目录及其所有子目录？', '警告', { type: 'warning' })
    const res = await api.deleteCategory({ id, userId: currentUser.value.id, force: 1 })
    if (res.code === 200) { ElMessage.success('删除成功'); await loadCategoryTree(); if (currentCategoryId.value === id) { currentCategoryId.value = null; allDocuments.value = [] } else if (currentCategoryId.value) loadAllDocuments() }
  } catch (err) { if (err !== 'cancel') ElMessage.error('删除失败') }
}

const selectDocument = async (doc) => { selectedDocId.value = doc.id; isEditing.value = false; try { const res = await api.getDocumentDetail({ documentId: doc.id, userId: currentUser.value.id }); if (res.code === 200) currentDocument.value = res.data } catch (err) { ElMessage.error('加载文档失败') } }

const backToFolder = () => { selectedDocId.value = null; currentDocument.value = null }

const startEditing = () => {
  if (!currentDocument.value) return
  documentForm.title = currentDocument.value.title
  documentForm.content = currentDocument.value.content
  documentForm.categoryId = currentDocument.value.categoryId
  documentForm.tagIds = currentDocument.value.tags ? currentDocument.value.tags.map(t => t.id) : []
  isEditing.value = true
  nextTick(() => { if (editorRef.value) { editorRef.value.innerHTML = markdownToHtml(documentForm.content); editorRef.value.focus(); const range = document.createRange(); range.selectNodeContents(editorRef.value); range.collapse(false); const sel = window.getSelection(); sel.removeAllRanges(); sel.addRange(range); refreshImageLibrary() } })
}

const showCreateDocumentDialog = () => {
  if (!currentCategoryId.value) return ElMessage.warning('请先选择一个目录')
  documentForm.title = ''; documentForm.content = ''; documentForm.tagIds = []; documentForm.categoryId = currentCategoryId.value
  isEditing.value = true; selectedDocId.value = null; currentDocument.value = null
  nextTick(() => { if (editorRef.value) { editorRef.value.innerHTML = ''; editorRef.value.focus() } })
}

const editDocument = (doc) => {
  documentForm.title = doc.title; documentForm.content = doc.content; documentForm.categoryId = doc.categoryId
  documentForm.tagIds = doc.tags ? doc.tags.map(t => t.id) : []
  isEditing.value = true; selectedDocId.value = doc.id
  nextTick(() => { if (editorRef.value) { editorRef.value.innerHTML = markdownToHtml(documentForm.content); editorRef.value.focus(); refreshImageLibrary() } })
}

const cancelEdit = () => { isEditing.value = false; activeImageForZoom.value = null; if (selectedDocId.value) selectDocument({ id: selectedDocId.value }); else { documentForm.title = ''; documentForm.content = ''; documentForm.tagIds = [] } }

const submitDocument = async () => {
  if (!documentForm.title.trim()) return ElMessage.warning('标题不能为空')
  try {
    const data = { title: documentForm.title, content: documentForm.content, categoryId: documentForm.categoryId, userId: currentUser.value.id, tagIds: documentForm.tagIds.length > 0 ? documentForm.tagIds : undefined }
    const res = selectedDocId.value ? await api.updateDocument({ ...data, documentId: selectedDocId.value }) : await api.createDocument(data)
    if (res.code === 200) { ElMessage.success('保存成功'); isEditing.value = false; activeImageForZoom.value = null; await loadAllDocuments(); if (res.data?.id) selectDocument({ id: res.data.id }) }
  } catch (err) { ElMessage.error('保存失败') }
}

const handleDeleteDocument = async (id) => {
  try {
    await ElMessageBox.confirm('确认删除该文档？', '警告', { type: 'warning' })
    const res = await api.deleteDocument({ documentId: id, userId: currentUser.value.id })
    if (res.code === 200) { ElMessage.success('删除成功'); if (selectedDocId.value === id) { selectedDocId.value = null; currentDocument.value = null; isEditing.value = false } await loadAllDocuments() }
    else { ElMessage.error(res.message || '删除失败') }
  } catch (err) { if (err !== 'cancel') ElMessage.error('删除失败') }
}

const openTrash = async () => {
  trashVisible.value = true
  try {
    const [catRes, docRes] = await Promise.all([api.listTrashCategories(), api.listTrashDocuments()])
    if (catRes.code === 200) trashCategories.value = catRes.data || []
    if (docRes.code === 200) trashDocuments.value = docRes.data || []
  } catch (err) { ElMessage.error('加载回收站失败') }
}

const handleRestoreCategory = async (id) => {
  try {
    const res = await api.restoreCategory({ id, force: 1 })
    if (res.code === 200) { ElMessage.success('恢复成功'); trashCategories.value = trashCategories.value.filter(c => c.id !== id); await loadCategoryTree() }
  } catch (err) { ElMessage.error('恢复失败') }
}

const handleRestoreDocument = async (id) => {
  try {
    const res = await api.restoreDocument({ documentId: id })
    if (res.code === 200) { ElMessage.success('恢复成功'); trashDocuments.value = trashDocuments.value.filter(d => d.id !== id); await loadAllDocuments() }
  } catch (err) { ElMessage.error('恢复失败') }
}

const handlePermanentDeleteCategory = async (id) => {
  try {
    await ElMessageBox.confirm('确认彻底删除该目录？此操作不可恢复！', '警告', { type: 'error' })
    const res = await api.deleteCategory({ id, force: 1 })
    if (res.code === 200) { ElMessage.success('已彻底删除'); trashCategories.value = trashCategories.value.filter(c => c.id !== id) }
  } catch (err) { if (err !== 'cancel') ElMessage.error('删除失败') }
}

const handlePermanentDeleteDocument = async (id) => {
  try {
    await ElMessageBox.confirm('确认彻底删除该文档？此操作不可恢复！', '警告', { type: 'error' })
    const res = await api.deleteDocument({ documentId: id })
    if (res.code === 200) { ElMessage.success('已彻底删除'); trashDocuments.value = trashDocuments.value.filter(d => d.id !== id) }
    else { ElMessage.error(res.message || '删除失败') }
  } catch (err) { if (err !== 'cancel') ElMessage.error('删除失败') }
}

const handleContentClick = (e) => { if (e.target.tagName === 'IMG') previewImageUrl.value = e.target.src; else startEditing() }

const renderMarkdown = (mk) => md.render(mk || '')

const formatDate = (d) => d ? new Date(d).toLocaleString('zh-CN') : ''

const highlightKeyword = (text, kw) => {
  if (!kw || !text) return text
  const esc = kw.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return text.replace(new RegExp(`(${esc})`, 'gi'), '<mark class="highlight">$1</mark>')
}

const logout = async () => {
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    if (userInfo.token) {
      await api.logout({ token: userInfo.token })
    }
  } catch (err) { /* 静默失败 */ }
  localStorage.removeItem('userInfo')
  router.push('/login')
}

const loadProfile = async () => {
  try {
    const res = await api.getUserProfile()
    if (res.code === 200 && res.data) {
      profile.value = res.data
      Object.assign(profileForm, {
        nickname: res.data.nickname || '',
        avatar: res.data.avatar || '',
        phone: res.data.phone || '',
        email: res.data.email || '',
        gender: res.data.gender,
        age: res.data.age,
        region: res.data.region || '',
        bio: res.data.bio || ''
      })
    }
  } catch (err) { /* 静默失败 */ }
}

const openProfile = () => {
  profileEditing.value = false
  profileVisible.value = true
  loadProfile()
}

const startProfileEdit = () => {
  profileOriginal.value = JSON.stringify(profileForm)
  profileEditing.value = true
}

const cancelProfileEdit = () => {
  if (profileOriginal.value && profileOriginal.value !== JSON.stringify(profileForm)) {
    ElMessageBox.confirm('有未保存的更改，确定放弃吗？', '提示', { type: 'warning', confirmButtonText: '放弃', cancelButtonText: '继续编辑' })
      .then(() => {
        Object.assign(profileForm, JSON.parse(profileOriginal.value))
        profileEditing.value = false
        profileOriginal.value = ''
      })
      .catch(() => {})
  } else {
    profileEditing.value = false
    profileOriginal.value = ''
  }
}

const handleProfileClose = () => {
  if (profileEditing.value && profileOriginal.value !== JSON.stringify(profileForm)) {
    ElMessageBox.confirm('有未保存的更改，确定放弃吗？', '提示', { type: 'warning', confirmButtonText: '放弃', cancelButtonText: '继续编辑' })
      .then(() => {
        Object.assign(profileForm, JSON.parse(profileOriginal.value))
        profileEditing.value = false
        profileOriginal.value = ''
        profileVisible.value = false
      })
      .catch(() => {})
    return
  }
  profileEditing.value = false
  profileOriginal.value = ''
}

const submitProfile = async () => {
  profileSaving.value = true
  try {
    const res = await api.updateUserProfile({
      nickname: profileForm.nickname || undefined,
      avatar: profileForm.avatar || undefined,
      phone: profileForm.phone || undefined,
      email: profileForm.email || undefined,
      gender: profileForm.gender,
      age: profileForm.age,
      region: profileForm.region || undefined,
      bio: profileForm.bio || undefined
    })
    if (res.code === 200) {
      ElMessage.success('保存成功')
      profile.value = { ...profileForm }
      profileEditing.value = false
      profileOriginal.value = ''
    }
  } catch (err) {
    ElMessage.error('保存失败')
  } finally {
    profileSaving.value = false
  }
}

const viewAvatar = () => {
  previewAvatarUrl.value = profileForm.avatar || ''
  avatarPreviewVisible.value = true
}

const handleAvatarUpload = async (file) => {
  try {
    const res = await api.uploadFile(file)
    if (res.code === 200) {
      profileForm.avatar = res.data
      ElMessage.success('头像上传成功')
    }
  } catch (err) {
    ElMessage.error('头像上传失败')
  }
  return false
}

const handleClearTrash = async () => {
  try {
    await ElMessageBox.confirm('确认清空回收站？所有内容将被永久删除，不可恢复！', '⚠️ 危险操作', { type: 'error', confirmButtonText: '确认清空', cancelButtonText: '取消' })
    clearingTrash.value = true
    const allCats = [...trashCategories.value]
    const allDocs = [...trashDocuments.value]
    for (const cat of allCats) {
      await api.deleteCategory({ id: cat.id, force: 1 })
    }
    for (const doc of allDocs) {
      await api.deleteDocument({ documentId: doc.id })
    }
    trashCategories.value = []
    trashDocuments.value = []
    ElMessage.success('回收站已清空')
    await loadCategoryTree()
  } catch (err) {
    if (err !== 'cancel') ElMessage.error('清空失败')
  } finally {
    clearingTrash.value = false
  }
}
</script>

<style scoped>
.dashboard {
  --bg-main: #f5f7fa;
  --bg-header: #fff;
  --bg-sidebar: #fff;
  --bg-content: #f9fafb;
  --bg-card: #fff;
  --bg-card-hover: #f0f4ff;
  --bg-card-active: #e8f0fe;
  --bg-input: #fafafa;
  --bg-editor: #fff;
  --text-primary: #333;
  --text-secondary: #555;
  --text-heading: #222;
  --text-muted: #999;
  --border-color: #e8e8e8;
  --border-light: #f0f0f0;
  --accent: #667eea;
  --accent-border: #667eea;
  --accent-shadow: rgba(102,126,234,0.2);
  --editor-border: #dcdfe6;
  height: 100vh; display: flex; flex-direction: column; background: var(--bg-main); outline: none;
}
.header { background: var(--bg-header); display: flex; justify-content: space-between; align-items: center; padding: 0 30px; box-shadow: 0 2px 8px rgba(0,0,0,0.08); z-index: 10; }
.logo { color: var(--accent); margin: 0; font-size: 26px; font-weight: 700; }
.header-right { display: flex; align-items: center; gap: 15px; }
.username { color: var(--text-secondary); font-size: 14px; }
.user-profile-trigger { display: flex; align-items: center; gap: 10px; cursor: pointer; padding: 4px 12px; border-radius: 20px; transition: background 0.2s; }
.user-profile-trigger:hover { background: var(--bg-card-hover); }
.header-avatar { flex-shrink: 0; }
.header-nickname { font-size: 14px; color: var(--text-primary); font-weight: 500; }
.profile-content { display: flex; flex-direction: column; }
.profile-avatar-section { display: flex; flex-direction: column; align-items: center; gap: 12px; }
.profile-avatar { flex-shrink: 0; }
.profile-avatar-actions { display: flex; gap: 8px; }
.trash-toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid var(--border-light); }
.trash-count { font-size: 13px; color: var(--text-muted); }
.trash-item-left { display: flex; align-items: center; gap: 10px; }
.trash-item-info { display: flex; flex-direction: column; }
.dashboard-body { display: flex; flex: 1; overflow: hidden; }
.view-mode-bar { display: flex; justify-content: center; gap: 8px; padding: 12px 16px 6px; background: var(--bg-sidebar); border-bottom: 1px solid var(--border-light); }
.dash-sidebar { background: var(--bg-sidebar); display: flex; flex-direction: column; border-right: 1px solid var(--border-color); transition: width 0.2s ease; overflow: hidden; min-width: 0; }
.dash-sidebar.collapsed { border-right: none; width: 0 !important; min-width: 0 !important; }
.sidebar-inner {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}
.resize-handle {
  width: 6px;
  cursor: col-resize;
  background: var(--border-color);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  z-index: 10;
  transition: background 0.2s;
  flex-shrink: 0;
}
.resize-handle:hover,
.resize-handle.dragging {
  background: var(--accent, #409eff);
}
.collapse-toggle {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  left: -14px;
  width: 18px;
  height: 40px;
  background: var(--bg-card);
  border: 1px solid var(--border-color);
  border-radius: 4px 0 0 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 11px;
  color: var(--text-secondary);
  z-index: 11;
  transition: background 0.2s;
  user-select: none;
}
.collapse-toggle:hover {
  background: var(--bg-card-hover);
  color: var(--text-primary);
}
.sidebar-header { padding: 20px; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; }
.sidebar-header h3 { margin: 0; font-size: 16px; color: var(--text-primary); font-weight: 600; }
.header-actions { display: flex; gap: 8px; align-items: center; }
.sidebar-search { padding: 12px 16px; border-bottom: 1px solid var(--border-light); background: var(--bg-input); }
.tree-wrapper { flex: 1; overflow-y: auto; padding: 10px; }
.content-area { background: var(--bg-content); padding: 20px 30px; overflow-y: auto; }
.global-search-bar { margin-bottom: 24px; }
.global-search-results { margin-bottom: 24px; }
.search-result-item { background: var(--bg-card); padding: 16px 20px; border-radius: 8px; margin-bottom: 10px; cursor: pointer; transition: box-shadow 0.15s; }
.search-result-item:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.search-result-item h4 { margin: 0 0 8px; color: var(--text-primary); }
.result-preview { font-size: 13px; color: var(--text-secondary); line-height: 1.6; }
.result-meta { font-size: 12px; color: var(--text-muted); }
.top-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.top-bar-title { font-size: 18px; font-weight: 600; color: var(--text-primary); }
.top-actions { display: flex; gap: 8px; }
.subcategory-section { margin-bottom: 24px; }
.subcategory-section h3 { font-size: 15px; color: var(--text-secondary); margin-bottom: 12px; }
.subcategory-grid { display: flex; flex-wrap: wrap; gap: 10px; }
.subcategory-card { display: flex; align-items: center; gap: 8px; padding: 10px 16px; background: var(--bg-card); border-radius: 8px; cursor: pointer; transition: all 0.2s; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
.subcategory-card:hover { background: var(--bg-card-hover); transform: translateY(-1px); box-shadow: 0 2px 8px var(--accent-shadow); }
.folder-icon { font-size: 20px; }
.folder-name { font-size: 14px; color: var(--text-primary); }
.doc-section h3 { font-size: 15px; color: var(--text-secondary); margin-bottom: 12px; }
.doc-list { display: flex; flex-direction: column; gap: 6px; }
.doc-item { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; background: var(--bg-card); border-radius: 6px; cursor: pointer; transition: all 0.15s; }
.doc-item:hover { background: var(--bg-card-hover); }
.doc-item.active { background: var(--bg-card-active); border-left: 3px solid var(--accent-border); }
.doc-item-content { flex: 1; overflow: hidden; }
.doc-title { font-size: 14px; color: var(--text-heading); display: block; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.doc-preview { font-size: 12px; color: var(--text-muted); margin-top: 3px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.doc-actions { display: flex; gap: 4px; }
.empty-tip { text-align: center; color: var(--text-muted); padding: 30px; font-size: 14px; }
.pagination-box { display: flex; justify-content: center; margin-top: 16px; }
.edit-mode { background: var(--bg-card); border-radius: 8px; padding: 20px; display: flex; flex-direction: column; }
.edit-header { display: flex; align-items: center; margin-bottom: 15px; }
.edit-actions { display: flex; gap: 10px; }
.edit-toolbar { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.toolbar-hint { color: var(--text-muted); font-size: 12px; }
.zoom-hint { color: #409eff; font-weight: 500; }
.content-editable { flex: 1; padding: 20px; border: 1px solid var(--editor-border); border-radius: 6px; min-height: 450px; outline: none; background: var(--bg-editor); line-height: 1.8; font-size: 15px; overflow-y: auto; white-space: pre-wrap; tab-size: 4; color: var(--text-primary); }
.content-editable:empty::before { content: attr(placeholder); color: #c0c4cc; }
.content-editable img {
  max-width: 100%;
  border-radius: 4px;
  margin: 5px 0;
  cursor: pointer;
  transition: width 0.1s ease;
}
.tag-selector { margin-top: 15px; }
.view-mode { background: var(--bg-card); border-radius: 8px; }
.doc-header { padding: 30px 40px 20px; border-bottom: 1px solid var(--border-light); position: relative; }
.doc-header h2 { margin: 0 0 12px; font-size: 22px; padding-right: 160px; color: var(--text-heading); }
.doc-header-actions { position: absolute; top: 30px; right: 40px; display: flex; gap: 8px; }
.doc-meta { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.time { color: var(--text-muted); font-size: 12px; margin-left: auto; }
.doc-content { padding: 30px 40px; line-height: 1.8; cursor: pointer; color: var(--text-primary); }
.doc-content:hover { background: var(--bg-card-hover); }
.doc-content :deep(img) { max-width: 100%; border-radius: 4px; cursor: pointer; transition: opacity 0.2s; }
.doc-content :deep(img:hover) { opacity: 0.9; }
.image-drawer-content { display: flex; flex-wrap: wrap; gap: 10px; padding: 10px; }
.image-drawer-item { width: 100%; cursor: pointer; border: 1px solid #eee; border-radius: 4px; overflow: hidden; transition: box-shadow 0.2s; }
.image-drawer-item:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.15); }
.image-drawer-item img { width: 100%; display: block; }
.image-preview-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.8); z-index: 2000; display: flex; justify-content: center; align-items: center; }
.image-preview-container { position: relative; max-width: 90vw; max-height: 90vh; }
.preview-image { max-width: 90vw; max-height: 90vh; object-fit: contain; }
.close-btn { position: absolute; top: -15px; right: -15px; }
.empty-state { display: flex; flex-direction: column; justify-content: center; align-items: center; height: 100%; color: #bbb; }
.empty-icon { font-size: 60px; margin-bottom: 16px; opacity: 0.4; }
.hint { font-size: 13px; color: #ccc; }
.shortcut-hint { position: fixed; bottom: 30px; right: 30px; z-index: 1000; }
.focus-bar { display: flex; align-items: center; gap: 12px; padding: 8px 16px; background: var(--bg-card); border-radius: 8px; margin-bottom: 12px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.focus-label { font-size: 13px; color: var(--text-secondary); }
.shortcut-btn { width: 40px; height: 40px; font-size: 18px; box-shadow: 0 2px 8px rgba(0,0,0,0.15); }
.shortcut-list { text-align: left; line-height: 2; }
.shortcut-list kbd { display: inline-block; padding: 2px 6px; background: #f5f5f5; border: 1px solid #ddd; border-radius: 3px; font-size: 12px; margin-right: 8px; min-width: 60px; text-align: center; }
:deep(.highlight) { background-color: #dbeafe; color: #1e40af; padding: 1px 3px; border-radius: 2px; font-weight: 500; }

/* === 暖色主题 === */
.theme-warm.dashboard {
  --bg-main: #fdf6f0;
  --bg-header: #fff8f0;
  --bg-sidebar: #fffaf5;
  --bg-content: #fef7f0;
  --bg-card: #fffbf7;
  --bg-card-hover: #fff0e0;
  --bg-card-active: #ffe8d6;
  --bg-input: #fffaf5;
  --bg-editor: #fffbf7;
  --text-primary: #5a4030;
  --text-secondary: #8a7060;
  --text-heading: #4a3020;
  --text-muted: #b0a090;
  --border-color: #e8d8c8;
  --border-light: #f0e0d0;
  --accent: #e8956a;
  --accent-border: #e8956a;
  --accent-shadow: rgba(232,149,106,0.3);
  --editor-border: #e8d8c8;
}

/* === 深色主题 === */
.theme-dark.dashboard {
  --bg-main: #1a1a2e;
  --bg-header: #16213e;
  --bg-sidebar: #1a1a2e;
  --bg-content: #0f0f23;
  --bg-card: #16213e;
  --bg-card-hover: #1f2f4f;
  --bg-card-active: #1a2744;
  --bg-input: #16213e;
  --bg-editor: #16213e;
  --text-primary: #ddd;
  --text-secondary: #aaa;
  --text-heading: #eee;
  --text-muted: #888;
  --border-color: #2a2a4a;
  --border-light: #2a2a4a;
  --accent: #8b9cf7;
  --accent-border: #667eea;
  --accent-shadow: rgba(0,0,0,0.3);
  --editor-border: #2a2a4a;
}

/* === 深色主题 - Element Plus 组件穿透覆盖 === */
.theme-dark :deep(.el-input__wrapper) {
  background: var(--bg-input) !important;
  box-shadow: 0 0 0 1px var(--border-color) inset !important;
}
.theme-dark :deep(.el-input__inner) {
  color: var(--text-primary) !important;
}
.theme-dark :deep(.el-tree) {
  background: transparent;
}
.theme-dark :deep(.el-tree-node__content) {
  background: transparent;
  color: var(--text-primary);
}
.theme-dark :deep(.el-tree-node__content:hover) {
  background: var(--bg-card-hover);
}
.theme-dark :deep(.el-tree-node.is-current > .el-tree-node__content) {
  background: var(--bg-card-active);
}
.theme-dark :deep(.el-button--primary) {
  --el-button-bg-color: var(--accent);
  --el-button-border-color: var(--accent);
}

</style>