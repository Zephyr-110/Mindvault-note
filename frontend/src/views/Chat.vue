<template>
  <div class="chat-container">
    <div class="chat-header">
      <el-button type="text" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        返回
      </el-button>
      <span class="chat-title">{{ otherNickname }}</span>
    </div>

    <div class="chat-messages" ref="msgBox">
      <div v-if="loading" class="loading-tip">加载中...</div>
      <div
          v-for="msg in messages"
          :key="msg.id"
          :class="['message-row', isMine(msg) ? 'mine' : 'other']"
      >
        <el-avatar v-if="!isMine(msg)" :size="36" :src="otherAvatar" class="msg-avatar">
          {{ otherNickname?.charAt(0) || 'U' }}
        </el-avatar>
        <div class="msg-body">
          <div :class="['message-bubble', isMine(msg) ? 'mine' : 'other']">
            {{ msg.content }}
          </div>
          <div class="time">{{ formatTime(msg.createTime) }}</div>
        </div>
        <el-avatar v-if="isMine(msg)" :size="36" :src="myAvatar" class="msg-avatar">
          {{ myNickname?.charAt(0) || '我' }}
        </el-avatar>
      </div>
    </div>

    <div class="chat-input-area">
      <div class="input-row">
        <el-input
            v-model="inputContent"
            :placeholder="'发送给 ' + (otherNickname || '对方')"
            @keyup.enter="sendMsg"
            maxlength="500"
            show-word-limit
        />
        <el-button type="primary" @click="sendMsg" :disabled="!inputContent.trim()">
          发送
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft } from '@element-plus/icons-vue'
import { connect, disconnect, subscribeChat, sendMessage } from '@/utils/websocket'
import api from '@/api/index'

const route = useRoute()
const router = useRouter()

const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
const myUserId = userInfo.id
const myAvatar = userInfo.avatar || ''
const myNickname = userInfo.nickname || ''

const otherUserId = ref(Number(route.query.with))
const messages = ref([])
const inputContent = ref('')
const loading = ref(true)
const otherNickname = ref('')
const otherAvatar = ref('')
const msgBox = ref(null)

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  const pad = (n) => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function isMine(msg) {
  return msg.senderId === myUserId
}

function scrollToBottom() {
  nextTick(() => {
    if (msgBox.value) {
      msgBox.value.scrollTop = msgBox.value.scrollHeight
    }
  })
}

async function loadHistory() {
  try {
    const res = await api.getMessageHistory({ withUserId: otherUserId.value, page: 1, size: 50 })
    if (res.data && res.data.list) {
      messages.value = res.data.list.reverse()
    }
    scrollToBottom()
    api.markMessageRead({ withUserId: otherUserId.value }).catch(() => {})
  } finally {
    loading.value = false
  }
}

async function init() {
  if (!otherUserId.value) {
    router.back()
    return
  }
  try {
    const userRes = await api.getUserProfileById(otherUserId.value)
    if (userRes.data) {
      otherNickname.value = userRes.data.nickname || '用户'
      otherAvatar.value = userRes.data.avatar || ''
    } else {
      otherNickname.value = '用户'
    }
  } catch (e) {
    otherNickname.value = '用户'
  }
  await loadHistory()
  try {
    await connect(myUserId)
    subscribeChat(myUserId, (msg) => {
      if (msg.error) {
        const idx = messages.value.findIndex(m => m.id < 0)
        if (idx >= 0) messages.value.splice(idx, 1)
        ElMessage.error(msg.message || '发送失败')
        return
      }
      if (msg.senderId === myUserId) {
        const idx = messages.value.findIndex(m => m.id < 0)
        if (idx >= 0) {
          messages.value.splice(idx, 1, msg)
        } else {
          messages.value.push(msg)
        }
      } else {
        messages.value.push(msg)
      }
      scrollToBottom()
    })
  } catch (e) {
    console.warn('WebSocket 连接失败，消息不会实时推送', e)
  }
}

async function sendMsg() {
  const content = inputContent.value.trim()
  if (!content) return
  inputContent.value = ''

  const tempId = -Date.now()
  const tempMsg = {
    id: tempId,
    senderId: myUserId,
    receiverId: otherUserId.value,
    content: content,
    createTime: new Date().toISOString()
  }

  // 乐观更新：立即显示
  messages.value.push(tempMsg)
  scrollToBottom()

  const msg = {
    senderId: myUserId,
    receiverId: otherUserId.value,
    content: content
  }
  const sent = sendMessage(msg)
  if (!sent) {
    try {
      const res = await api.sendMessage(msg)
      if (res.code === 200 && res.data) {
        const idx = messages.value.findIndex(m => m.id === tempId)
        if (idx >= 0) messages.value.splice(idx, 1, res.data)
        scrollToBottom()
      } else {
        const idx = messages.value.findIndex(m => m.id === tempId)
        if (idx >= 0) messages.value.splice(idx, 1)
        ElMessage.error(res.message || '发送失败')
      }
    } catch (e) {
      const idx = messages.value.findIndex(m => m.id === tempId)
      if (idx >= 0) messages.value.splice(idx, 1)
      ElMessage.error('发送失败，请检查网络')
    }
  }
}

function goBack() {
  router.back()
}

onMounted(() => {
  init()
})

onUnmounted(() => {
  disconnect()
})
</script>

<style scoped>
.chat-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f5f5;
}
.chat-header {
  height: 56px;
  display: flex;
  align-items: center;
  padding: 0 16px;
  background: #fff;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}
.chat-title {
  margin-left: 12px;
  font-size: 17px;
  font-weight: 500;
}
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.loading-tip {
  text-align: center;
  color: #999;
  font-size: 13px;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  max-width: 80%;
}
.message-row.mine {
  align-self: flex-end;
  flex-direction: row-reverse;
}
.message-row.other {
  align-self: flex-start;
}

.msg-avatar {
  flex-shrink: 0;
}

.msg-body {
  display: flex;
  flex-direction: column;
}
.message-row.mine .msg-body {
  align-items: flex-end;
}
.message-row.other .msg-body {
  align-items: flex-start;
}

.message-bubble {
  padding: 10px 14px;
  border-radius: 8px;
  line-height: 1.5;
  word-break: break-word;
  font-size: 15px;
  position: relative;
}
.message-row.mine .message-bubble {
  background: #1989fa;
  color: #fff;
  border-top-right-radius: 2px;
}
.message-row.other .message-bubble {
  background: #fff;
  color: #333;
  border-top-left-radius: 2px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.06);
}

.msg-body .time {
  font-size: 11px;
  color: #999;
  margin-top: 4px;
  padding: 0 4px;
}
.chat-input-area {
  padding: 12px 16px;
  background: #fff;
  border-top: 1px solid #eee;
  flex-shrink: 0;
}
.input-row {
  display: flex;
  gap: 10px;
}
.input-row .el-input {
  flex: 1;
}
</style>