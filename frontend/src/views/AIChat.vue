<template>
  <div class="ai-chat-page">
    <!-- 左侧会话列表 -->
    <div class="session-sidebar" :class="{ hidden: sessionListHidden }">
      <div class="session-sidebar-header">
        <span class="session-list-title">会话列表</span>
        <el-button class="new-session-btn" size="small" @click="createNewSession">
          <span class="plus-icon">+</span> 新建
        </el-button>
      </div>
      <div class="session-list">
        <div
          v-for="s in sessions"
          :key="s.id"
          :class="['session-item', { active: currentSessionId === s.id }]"
          @click="selectSession(s.id)"
          @dblclick.stop="startEditTitle(s)"
        >
          <input
            v-if="editingSessionId === s.id"
            v-model="editingTitle"
            class="session-title-input"
            @blur="saveTitle(s)"
            @keydown.enter="saveTitle(s)"
            @keydown.escape="cancelEdit"
            @click.stop
          />
          <span v-else class="session-title">{{ s.title || '无标题会话' }}</span>
          <el-button class="session-delete-btn" link size="small" @click.stop="handleDeleteSession(s.id)">✕</el-button>
        </div>
        <div v-if="sessions.length === 0" class="empty-sessions">暂无会话</div>
      </div>
    </div>

    <!-- 收缩按钮 -->
    <div class="session-toggle" @click="sessionListHidden = !sessionListHidden">
      {{ sessionListHidden ? '▶' : '◀' }}
    </div>

    <!-- 聊天区 -->
    <div class="chat-area">
      <div class="chat-header">
        <span class="chat-title">🤖 小M助手</span>
        <el-button v-if="currentSessionId" type="danger" plain size="small" @click="clearCurrentChat">清空对话</el-button>
      </div>

      <div class="chat-messages" ref="msgBox">
        <div v-if="!currentSessionId" class="empty-chat">
          <p>👋 你好！我是你的笔记助手</p>
          <p class="hint">新建一个会话，开始和 AI 聊天吧</p>
        </div>
        <div v-else-if="historyMessages.length === 0 && !loading" class="empty-chat">
          <p>开始新的对话吧</p>
        </div>

        <div v-for="(msg, idx) in historyMessages" :key="idx" :class="['msg-row', msg.role]">
          <div class="msg-avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
          <div class="msg-bubble markdown-body" v-html="renderMarkdown(msg.content)"></div>
        </div>

        <div v-if="reasoningContent" class="reasoning-card">
          <div class="reasoning-header" @click="reasoningExpanded = !reasoningExpanded">
            <span class="reasoning-title">💭 思考过程</span>
            <span class="reasoning-toggle">{{ reasoningExpanded ? '收起 ▲' : '展开 ▼' }}</span>
          </div>
          <div v-show="reasoningExpanded" class="reasoning-body">{{ reasoningContent }}</div>
        </div>

        <div v-if="thinkingCards.length > 0" class="thinking-section">
          <div v-for="card in thinkingCards" :key="card.id" class="thinking-card">
            <span class="thinking-icon">{{ card.icon }}</span>
            <span class="thinking-text">{{ card.text }}</span>
          </div>
        </div>

        <div v-if="streamingContent" class="msg-row assistant">
          <div class="msg-avatar">🤖</div>
          <div class="msg-bubble markdown-body" v-html="renderMarkdown(streamingContent)"></div>
          <span class="cursor">|</span>
        </div>

        <div v-if="loading && !streamingContent && thinkingCards.length === 0" class="msg-row assistant">
          <div class="msg-avatar">🤖</div>
          <div class="msg-bubble typing">思考中<span class="dots">...</span></div>
        </div>
      </div>

      <div class="chat-input">
        <el-input
          v-model="inputText"
          type="textarea"
          :autosize="{ minRows: 1, maxRows: 5 }"
          placeholder="输入你的问题... (Enter 换行，Ctrl+Enter 发送)"
          :disabled="loading"
          class="input-field"
          @keydown="handleInputKeydown"
        />
        <el-button
          class="send-btn"
          type="primary"
          :disabled="loading || !inputText.trim()"
          @click="sendMessage"
        >
          <span class="send-icon">↑</span>
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api/index'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'

const md = new MarkdownIt({
  html: true,
  breaks: true,
  highlight: function (str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' +
          hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
          '</code></pre>'
      } catch (__) {}
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  }
})

function renderMarkdown(text) {
  if (!text) return ''
  return md.render(text)
}

const sessionListHidden = ref(false)
const sessions = ref([])
const currentSessionId = ref(null)
const historyMessages = ref([])
const inputText = ref('')
const loading = ref(false)
const streamingContent = ref('')
const thinkingCards = ref([])
const reasoningContent = ref('')
const reasoningExpanded = ref(true)
const editingSessionId = ref(null)
const editingTitle = ref('')
const msgBox = ref(null)

function scrollToBottom() {
  nextTick(() => {
    if (msgBox.value) {
      msgBox.value.scrollTop = msgBox.value.scrollHeight
    }
  })
}

async function loadSessions() {
  try {
    const res = await api.aiListSessions({ page: 1, size: 50 })
    sessions.value = res.data || []
  } catch (e) {
    // ignore
  }
}

async function createNewSession() {
  try {
    await api.aiCreateSession()
    await loadSessions()
    if (sessions.value.length > 0) {
      selectSession(sessions.value[0].id)
    }
  } catch (e) {
    ElMessage.error('创建会话失败')
  }
}

async function handleDeleteSession(sessionId) {
  try {
    await ElMessageBox.confirm('确定删除该会话？', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.aiDeleteSession({ sessionId, page: 1, size: 50 })
    if (currentSessionId.value === sessionId) {
      currentSessionId.value = null
      historyMessages.value = []
    }
    await loadSessions()
    ElMessage.success('已删除')
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

async function selectSession(sessionId) {
  currentSessionId.value = sessionId
  historyMessages.value = []
  streamingContent.value = ''
  try {
    const res = await api.aiListMessagesHistory({ sessionId, page: 1, size: 200 })
    historyMessages.value = (res.data || []).map(m => ({
      role: m.role,
      content: m.content
    }))
    scrollToBottom()
  } catch (e) {
    // ignore
  }
}

function handleInputKeydown(e) {
  if (e.ctrlKey && e.key === 'Enter') {
    e.preventDefault()
    sendMessage()
  }
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  inputText.value = ''
  loading.value = true
  streamingContent.value = ''
  thinkingCards.value = []
  reasoningContent.value = ''
  reasoningExpanded.value = true

  historyMessages.value.push({ role: 'user', content: text })
  scrollToBottom()

  try {
    const response = await api.aiAgentChatStream({
      query: text,
      sessionId: currentSessionId.value ? String(currentSessionId.value) : null
    })

    if (!response.ok) {
      throw new Error('HTTP ' + response.status)
    }

    const reader = response.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''
    let fullContent = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop()

      for (const line of lines) {
        if (line.startsWith('data:')) {
          try {
            const event = JSON.parse(line.substring(5).trim())
            handleSSEEvent(event)
            if (event.type === 'content') {
              fullContent += event.text
            }
          } catch (e) {
            // 忽略解析错误
          }
        }
      }
    }

    historyMessages.value.push({ role: 'assistant', content: fullContent || '(空)' })
    streamingContent.value = ''
    thinkingCards.value = []
    reasoningContent.value = ''
    reasoningExpanded.value = true
    scrollToBottom()
    await loadSessions()
  } catch (e) {
    const msg = e?.message || '请求失败'
    historyMessages.value.push({ role: 'assistant', content: '出错了: ' + msg })
    thinkingCards.value = []
    reasoningContent.value = ''
    reasoningExpanded.value = true
    scrollToBottom()
  } finally {
    loading.value = false
  }
}

function handleSSEEvent(event) {
  switch (event.type) {
    case 'thinking':
      thinkingCards.value.push({ id: Date.now(), icon: '💭', text: '思考中...' })
      break
    case 'session_created':
      currentSessionId.value = event.sessionId
      loadSessions()
      break
    case 'reasoning':
      reasoningContent.value += event.text
      reasoningExpanded.value = true
      break
    case 'tool_call':
      thinkingCards.value.push({
        id: Date.now(),
        icon: '🔍',
        text: `正在搜索${event.name === 'search_notes' ? '笔记' : '帖子'}："${event.arg?.query || ''}"`
      })
      break
    case 'tool_result':
      const last = thinkingCards.value[thinkingCards.value.length - 1]
      if (last) {
        last.icon = '✅'
        last.text = last.text.replace('正在搜索', '已完成搜索')
      }
      break
    case 'content':
      if (thinkingCards.value.length > 0) thinkingCards.value = []
      if (reasoningContent.value) reasoningExpanded.value = false
      streamingContent.value += event.text
      scrollToBottom()
      break
  }
}

function clearCurrentChat() {
  historyMessages.value = []
  streamingContent.value = ''
  thinkingCards.value = []
  reasoningContent.value = ''
  reasoningExpanded.value = true
}

function startEditTitle(session) {
  editingSessionId.value = session.id
  editingTitle.value = session.title || ''
  nextTick(() => {
    const input = document.querySelector('.session-title-input')
    if (input) input.select()
  })
}

async function saveTitle(session) {
  const title = editingTitle.value.trim()
  if (title && title !== session.title) {
    try {
      await api.aiUpdateSessionTitle({ sessionId: session.id, newTitle: title, page: 1, size: 50 })
      await loadSessions()
    } catch (e) {
      ElMessage.error('修改标题失败')
    }
  }
  editingSessionId.value = null
}

function cancelEdit() {
  editingSessionId.value = null
}

onMounted(() => {
  loadSessions()
})
</script>

<style scoped>
.ai-chat-page {
  display: flex;
  height: 100vh;
  background: #fff;
}

.session-sidebar {
  width: 220px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
  background: #fafafa;
  transition: width 0.3s;
  overflow: hidden;
}

.session-sidebar.hidden {
  width: 0;
  border-right: none;
}

.session-sidebar-header {
  padding: 14px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e4e7ed;
  white-space: nowrap;
}

.session-list-title {
  font-weight: 700;
  font-size: 14px;
  color: #303133;
}

.new-session-btn {
  background: linear-gradient(135deg, #667eea, #764ba2) !important;
  color: #fff !important;
  border: none !important;
  border-radius: 6px !important;
  padding: 4px 10px !important;
  font-size: 12px !important;
  font-weight: 500;
  transition: all 0.2s;
}

.new-session-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 2px 10px rgba(118, 75, 162, 0.35);
}

.plus-icon {
  font-weight: 700;
  font-size: 14px;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px;
}

.session-item {
  padding: 12px;
  border-radius: 10px;
  cursor: pointer;
  font-size: 13px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
  white-space: nowrap;
  background: #fff;
  border: 1px solid #ebeef5;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
  transition: all 0.2s;
}

.session-item:hover {
  background: #f3f0ff;
  border-color: #c4b5fd;
  box-shadow: 0 2px 8px rgba(118, 75, 162, 0.1);
}

.session-item.active {
  background: linear-gradient(135deg, #f3f0ff, #ede9fe);
  border-color: #a78bfa;
  font-weight: 500;
  box-shadow: 0 2px 8px rgba(118, 75, 162, 0.15);
}

.session-title {
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
}

.session-title-input {
  flex: 1;
  border: 1px solid #667eea;
  border-radius: 4px;
  padding: 2px 6px;
  font-size: 13px;
  outline: none;
  background: #fff;
}

.session-delete-btn {
  opacity: 0;
  transition: opacity 0.2s;
  color: #909399;
}

.session-item:hover .session-delete-btn {
  opacity: 1;
}

.session-delete-btn:hover {
  color: #f56c6c;
}

.empty-sessions {
  text-align: center;
  color: #909399;
  font-size: 12px;
  margin-top: 20px;
}

.session-toggle {
  width: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: #f0f2f5;
  color: #909399;
  font-size: 13px;
  user-select: none;
  border-right: 1px solid #e4e7ed;
  transition: background 0.2s;
}

.session-toggle:hover {
  background: #e4e7ed;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.chat-header {
  padding: 14px 20px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff;
}

.chat-title {
  font-weight: 700;
  font-size: 17px;
  color: #303133;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-chat {
  text-align: center;
  color: #909399;
  margin-top: 80px;
}

.empty-chat .hint {
  font-size: 13px;
  margin-top: 6px;
}

.msg-row {
  display: flex;
  gap: 10px;
  max-width: 75%;
}

.msg-row.user {
  flex-direction: row-reverse;
  align-self: flex-end;
}

.msg-row.assistant {
  align-self: flex-start;
}

.msg-avatar {
  font-size: 24px;
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.msg-row.user .msg-avatar {
  background: #409eff;
}

.msg-row.assistant .msg-avatar {
  background: #f0f2f5;
}

.msg-bubble {
  padding: 12px 16px;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.65;
  word-break: break-word;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  position: relative;
  white-space: pre-wrap;
}

.msg-bubble.markdown-body :deep(p) {
  margin: 0 0 8px;
}
.msg-bubble.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}
.msg-bubble.markdown-body :deep(pre) {
  background: #f6f8fa;
  border-radius: 8px;
  padding: 12px;
  overflow-x: auto;
  margin: 8px 0;
}
.msg-bubble.markdown-body :deep(code) {
  font-family: 'SF Mono', 'Fira Code', monospace;
  font-size: 13px;
}
.msg-bubble.markdown-body :deep(ul),
.msg-bubble.markdown-body :deep(ol) {
  padding-left: 20px;
  margin: 6px 0;
}
.msg-bubble.markdown-body :deep(blockquote) {
  border-left: 3px solid #dfe2e5;
  padding-left: 12px;
  color: #6a737d;
  margin: 8px 0;
}

.msg-row.user .msg-bubble {
  background: linear-gradient(135deg, #409eff, #5b7fff);
  color: #fff;
  border-bottom-right-radius: 6px;
}

.msg-row.assistant .msg-bubble {
  background: #fff;
  color: #303133;
  border: 1px solid #e8ecf1;
  border-bottom-left-radius: 6px;
}

.msg-bubble.typing {
  color: #909399;
  font-style: italic;
}

.dots::after {
  content: '';
  animation: dots 1.5s steps(3, end) infinite;
}

@keyframes dots {
  0% { content: ''; }
  33% { content: '.'; }
  66% { content: '..'; }
  100% { content: '...'; }
}

.thinking-section {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 0 4px;
}

.thinking-card {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: #e8f4fd;
  border-radius: 6px;
  font-size: 12px;
  color: #409eff;
  width: fit-content;
}

.thinking-icon {
  font-size: 13px;
  flex-shrink: 0;
}

.thinking-text {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.reasoning-card {
  margin: 4px 0;
  border: 1px solid #d0e0f0;
  border-radius: 8px;
  background: #f8fafc;
  overflow: hidden;
}
.reasoning-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 12px;
  cursor: pointer;
  user-select: none;
  background: #eef4fb;
  font-size: 13px;
  color: #4a5568;
}
.reasoning-header:hover {
  background: #e3ecf5;
}
.reasoning-title {
  font-weight: 500;
}
.reasoning-toggle {
  font-size: 11px;
  color: #8899aa;
}
.reasoning-body {
  padding: 8px 12px;
  font-size: 12px;
  color: #5a6a7a;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 300px;
  overflow-y: auto;
}

.chat-input {
  padding: 12px 20px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  gap: 10px;
  align-items: flex-end;
  background: #fff;
}

.input-field {
  flex: 1;
}

.input-field :deep(textarea) {
  border-radius: 12px !important;
  padding: 12px 14px !important;
  font-size: 14px !important;
  line-height: 1.6 !important;
  resize: none !important;
  border-color: #e4e7ed !important;
  transition: border-color 0.2s;
}

.input-field :deep(textarea):focus {
  border-color: #a78bfa !important;
  box-shadow: 0 0 0 2px rgba(118, 75, 162, 0.1) !important;
}

.send-btn {
  width: 40px !important;
  height: 40px !important;
  border-radius: 50% !important;
  padding: 0 !important;
  display: flex !important;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  transition: all 0.2s;
  flex-shrink: 0;
  margin-bottom: 2px;
}

.send-btn:not(:disabled):hover {
  transform: scale(1.08);
  box-shadow: 0 4px 12px rgba(118, 75, 162, 0.35);
}

.send-icon {
  line-height: 1;
  font-weight: 700;
}
</style>