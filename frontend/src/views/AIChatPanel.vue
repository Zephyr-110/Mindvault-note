<template>
  <div class="ai-chat-panel" :class="{ collapsed: !visible }">
    <div class="panel-toggle" @click="visible = !visible">
      <span class="toggle-icon">{{ visible ? '▶' : '◀' }}</span>
      <span v-if="!visible" class="toggle-label">AI</span>
    </div>

    <div v-show="visible" class="panel-body">
      <!-- 左侧会话列表 -->
      <div class="session-sidebar" :class="{ hidden: sessionListHidden }">
        <div class="session-sidebar-header">
          <span class="session-list-title">会话</span>
          <el-button type="primary" size="small" link @click="createNewSession">+ 新建</el-button>
        </div>
        <div class="session-list">
          <div
            v-for="s in sessions"
            :key="s.id"
            :class="['session-item', { active: currentSessionId === s.id }]"
            @click="selectSession(s.id)"
          >
            <span class="session-title">{{ s.title || '无标题会话' }}</span>
            <el-button class="session-delete-btn" link size="small" @click.stop="handleDeleteSession(s.id)">✕</el-button>
          </div>
          <div v-if="sessions.length === 0" class="empty-sessions">暂无会话</div>
        </div>
      </div>

      <!-- 收缩按钮 -->
      <div class="session-toggle" @click="sessionListHidden = !sessionListHidden">
        {{ sessionListHidden ? '☰' : '✕' }}
      </div>

      <!-- 聊天区 -->
      <div class="chat-area">
        <div class="panel-header">
          <span class="panel-title">🤖 MindVault AI</span>
          <el-button v-if="currentSessionId" link size="small" @click="clearCurrentChat">清空</el-button>
        </div>

        <div class="panel-messages" ref="msgBox">
          <div v-if="!currentSessionId" class="empty-chat">
            <p>👋 你好！我是你的笔记助手</p>
            <p class="hint">新建一个会话，开始和 AI 聊天吧</p>
          </div>
          <div v-else-if="historyMessages.length === 0 && !loading" class="empty-chat">
            <p>开始新的对话吧</p>
          </div>

          <div v-for="(msg, idx) in historyMessages" :key="idx" :class="['msg-row', msg.role]">
            <div class="msg-avatar">{{ msg.role === 'user' ? '👤' : '🤖' }}</div>
            <div class="msg-bubble">{{ msg.content }}</div>
          </div>

          <div v-if="streamingContent" class="msg-row assistant">
            <div class="msg-avatar">🤖</div>
            <div class="msg-bubble">{{ streamingContent }}<span class="cursor">|</span></div>
          </div>

          <div v-if="loading && !streamingContent" class="msg-row assistant">
            <div class="msg-avatar">🤖</div>
            <div class="msg-bubble typing">思考中<span class="dots">...</span></div>
          </div>
        </div>

        <div class="panel-input">
          <el-input
            v-model="inputText"
            placeholder="问AI关于你的笔记..."
            @keyup.enter="sendMessage"
            :disabled="loading"
            size="small"
          >
            <template #append>
              <el-button
                @click="sendMessage"
                :disabled="loading || !inputText.trim()"
                size="small"
              >
                发送
              </el-button>
            </template>
          </el-input>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api/index'

const visible = ref(true)
const sessionListHidden = ref(false)
const sessions = ref([])
const currentSessionId = ref(null)
const historyMessages = ref([])
const inputText = ref('')
const loading = ref(false)
const streamingContent = ref('')
const msgBox = ref(null)

function scrollToBottom() {
  nextTick(() => {
    if (msgBox.value) {
      msgBox.value.scrollTop = msgBox.value.scrollHeight
    }
  })
}

function getToken() {
  const userInfo = localStorage.getItem('userInfo')
  if (userInfo) {
    try {
      return JSON.parse(userInfo).token || ''
    } catch (e) {
      return ''
    }
  }
  return ''
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

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || loading.value) return

  if (!currentSessionId.value) {
    await createNewSession()
    if (!currentSessionId.value) {
      ElMessage.error('创建会话失败')
      return
    }
  }

  inputText.value = ''
  loading.value = true
  streamingContent.value = ''

  historyMessages.value.push({ role: 'user', content: text })
  scrollToBottom()

  const sessionId = currentSessionId.value

  try {
    const response = await fetch('/api/ai/chat/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + getToken()
      },
      body: JSON.stringify({
        query: text,
        sessionId: String(sessionId),
        stream: true
      })
    })

    if (!response.ok) {
      historyMessages.value.push({ role: 'assistant', content: '请求失败: HTTP ' + response.status })
      loading.value = false
      scrollToBottom()
      return
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
      buffer = lines.pop() || ''

      for (const line of lines) {
        const trimmed = line.trim()
        if (trimmed.startsWith('data:')) {
          const dataStr = trimmed.substring(5).trim()
          try {
            const data = JSON.parse(dataStr)
            if (data.error) {
              fullContent = '出错了: ' + data.error
              streamingContent.value = fullContent
            } else if (data.done) {
              // 流结束标记，跳过
            } else if (data.content !== undefined) {
              fullContent += data.content
              streamingContent.value = fullContent
              scrollToBottom()
            } else {
              fullContent += dataStr
              streamingContent.value = fullContent
              scrollToBottom()
            }
          } catch (e) {
            fullContent += dataStr
            streamingContent.value = fullContent
            scrollToBottom()
          }
        }
      }
    }

    streamingContent.value = ''
    historyMessages.value.push({ role: 'assistant', content: fullContent || '(空)' })
    scrollToBottom()
    await loadSessions()
  } catch (e) {
    historyMessages.value.push({ role: 'assistant', content: '连接失败: ' + e.message })
    scrollToBottom()
  } finally {
    loading.value = false
  }
}

function clearCurrentChat() {
  historyMessages.value = []
  streamingContent.value = ''
}

watch(visible, (val) => {
  if (val) {
    loadSessions()
    scrollToBottom()
  }
})

onMounted(() => {
  loadSessions()
})
</script>

<style scoped>
.ai-chat-panel {
  position: fixed;
  right: 0;
  top: 60px;
  bottom: 0;
  z-index: 100;
  display: flex;
  transition: transform 0.3s ease;
}

.ai-chat-panel.collapsed {
  transform: translateX(calc(100% - 40px));
}

.panel-toggle {
  width: 40px;
  min-width: 40px;
  background: #409eff;
  color: #fff;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  border-radius: 8px 0 0 8px;
  user-select: none;
  font-size: 14px;
  gap: 4px;
}

.toggle-label {
  writing-mode: vertical-rl;
  font-size: 12px;
  letter-spacing: 2px;
}

.panel-body {
  display: flex;
  background: #fff;
  border-left: 1px solid #e4e7ed;
  box-shadow: -2px 0 12px rgba(0, 0, 0, .08);
  width: 600px;
}

/* 会话列表 */
.session-sidebar {
  width: 180px;
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
  padding: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #e4e7ed;
  white-space: nowrap;
}

.session-list-title {
  font-weight: 600;
  font-size: 13px;
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 6px;
}

.session-item {
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  font-size: 13px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 2px;
  white-space: nowrap;
}

.session-item:hover {
  background: #e8f0fe;
}

.session-item.active {
  background: #d0e4ff;
  font-weight: 500;
}

.session-title {
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
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
  width: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  background: #f0f2f5;
  color: #909399;
  font-size: 12px;
  user-select: none;
  border-right: 1px solid #e4e7ed;
}

.session-toggle:hover {
  background: #e4e7ed;
}

/* 聊天区 */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.panel-header {
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-title {
  font-weight: 600;
  font-size: 15px;
}

.panel-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.empty-chat {
  text-align: center;
  color: #909399;
  margin-top: 60px;
}

.empty-chat .hint {
  font-size: 12px;
  margin-top: 4px;
}

.msg-row {
  display: flex;
  gap: 8px;
  max-width: 100%;
}

.msg-row.user {
  flex-direction: row-reverse;
}

.msg-avatar {
  font-size: 20px;
  flex-shrink: 0;
}

.msg-bubble {
  padding: 8px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.5;
  word-break: break-word;
  max-width: 300px;
  white-space: pre-wrap;
}

.msg-row.user .msg-bubble {
  background: #409eff;
  color: #fff;
  border-bottom-right-radius: 4px;
}

.msg-row.assistant .msg-bubble {
  background: #f0f2f5;
  color: #303133;
  border-bottom-left-radius: 4px;
}

.msg-bubble.typing {
  color: #909399;
}

.cursor {
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
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

.panel-input {
  padding: 10px;
  border-top: 1px solid #e4e7ed;
}
</style>