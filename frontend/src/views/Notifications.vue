C:\Users\30149\IdeaProjects\MindVault-note-project\frontend\src\views\Notifications.vue
<template>
  <div class="notifications-page">
    <div class="page-header">
      <span class="back-btn" @click="$router.back()">←</span>
      <h1>消息</h1>
    </div>

    <!-- 通知横条 -->
    <div class="notify-bar" @click="showNotifications = true">
      <div class="notify-bar-left">
        <span class="notify-bar-icon">🔔</span>
        <div class="notify-bar-text">
          <span class="notify-bar-label">通知</span>
          <span class="notify-bar-preview" v-if="latestNotification">
            {{ getNotifyText(latestNotification) }}
          </span>
          <span class="notify-bar-preview empty" v-else>暂无新通知</span>
        </div>
      </div>
      <div class="notify-bar-right">
        <span v-if="unreadNotiCount > 0" class="red-dot">{{ unreadNotiCount > 99 ? '99+' : unreadNotiCount }}</span>
        <span class="arrow">›</span>
      </div>
    </div>

    <!-- 会话列表 -->
    <div class="conversation-section">
      <div v-if="conversations.length === 0 && !loadingConv" class="empty-state">
        <span class="empty-icon">💬</span>
        <p>暂无私信</p>
      </div>

      <div
          v-for="conv in conversations"
          :key="conv.userId"
          class="conv-card"
          @click="openConversation(conv)"
      >
        <el-avatar :size="48" :src="conv.avatar">{{ conv.nickname?.charAt(0) || 'U' }}</el-avatar>
        <div class="conv-body">
          <div class="conv-top">
            <span class="conv-name">{{ conv.nickname || '未知用户' }}</span>
            <span class="conv-time">{{ formatTime(conv.lastTime) }}</span>
          </div>
          <p class="conv-preview">{{ conv.lastMessage || '暂无消息' }}</p>
        </div>
        <span v-if="conv.unreadCount > 0" class="conv-badge">{{ conv.unreadCount > 99 ? '99+' : conv.unreadCount }}</span>
      </div>

      <div v-if="loadingConv" class="loading-tip">加载中...</div>
    </div>

    <!-- 通知列表弹窗 -->
    <el-dialog v-model="showNotifications" title="通知" width="480px">
      <div v-if="notifications.length === 0 && !loadingNoti" class="empty-state">
        <span class="empty-icon">📭</span>
        <p>暂无通知</p>
      </div>

      <div
          v-for="item in notifications"
          :key="item.id"
          class="notify-card"
          :class="{ unread: !item.isRead }"
          @click="handleReadNotification(item)"
      >
        <span class="notify-icon">{{ getNotifyIcon(item.type) }}</span>
        <div class="notify-body">
          <p class="notify-text">{{ getNotifyText(item) }}</p>
          <span class="notify-time">{{ formatTime(item.createTime) }}</span>
        </div>
        <span v-if="!item.isRead" class="unread-dot"></span>
      </div>

      <div v-if="loadingNoti" class="loading-tip">加载中...</div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import api from '../api'

const router = useRouter()

const showNotifications = ref(false)
const notifications = ref([])
const conversations = ref([])
const loadingNoti = ref(false)
const loadingConv = ref(false)

const unreadNotiCount = computed(() => notifications.value.filter(n => !n.isRead).length)
const latestNotification = computed(() => notifications.value.find(n => !n.isRead) || notifications.value[0])

onMounted(() => {
  loadNotifications()
  loadConversations()
})

async function loadNotifications() {
  loadingNoti.value = true
  try {
    const res = await api.listNotifications({ page: 1, size: 50 })
    if (res.code === 200 && res.data) {
      notifications.value = res.data.list || []
    }
  } catch (e) {
    // ignore
  } finally {
    loadingNoti.value = false
  }
}

async function loadConversations() {
  loadingConv.value = true
  try {
    const res = await api.listConversations({ page: 1, size: 50 })
    if (res.code === 200 && res.data) {
      conversations.value = res.data.list || []
    }
  } catch (e) {
    // ignore
  } finally {
    loadingConv.value = false
  }
}

async function handleReadNotification(item) {
  if (item.isRead) return
  try {
    await api.readNotification({ notificationId: item.id })
    item.isRead = true
  } catch (e) {
    // ignore
  }
}

async function openNotifications() {
  showNotifications.value = true
  await loadNotifications()
  await api.readNotification({ notificationId: 0 })
  notifications.value.forEach(n => n.isRead = true)
}

function openConversation(conv) {
  router.push(`/chat?with=${conv.userId}`)
}

function getNotifyIcon(type) {
  const map = { follow: '👤', like: '❤️', comment: '💬', reply: '↩️', favorite: '⭐' }
  return map[type] || '🔔'
}

function getNotifyText(item) {
  const map = {
    follow: '关注了你',
    like: '点赞了你的帖子',
    comment: '评论了你的帖子',
    reply: '回复了你的评论',
    favorite: '收藏了你的帖子'
  }
  return `${item.senderNickname || '用户'} ${map[item.type] || '与你互动'}`
}

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return d.toLocaleDateString()
}
</script>

<style scoped>
.notifications-page {
  max-width: 680px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.back-btn {
  font-size: 20px;
  cursor: pointer;
  color: #555;
}

.page-header h1 {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
}

.notify-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: 14px;
  padding: 16px 18px;
  margin-bottom: 16px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.06);
  cursor: pointer;
  transition: background 0.2s;
}

.notify-bar:hover {
  background: #fafbff;
}

.notify-bar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.notify-bar-icon {
  font-size: 24px;
}

.notify-bar-text {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.notify-bar-label {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.notify-bar-preview {
  font-size: 13px;
  color: #888;
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.notify-bar-preview.empty {
  color: #bbb;
}

.notify-bar-right {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.red-dot {
  background: #f56c6c;
  color: #fff;
  font-size: 12px;
  min-width: 20px;
  height: 20px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 6px;
}

.arrow {
  font-size: 20px;
  color: #ccc;
}

.conversation-section {
  background: #fff;
  border-radius: 14px;
  padding: 8px 0;
  box-shadow: 0 2px 10px rgba(0,0,0,0.06);
}

.empty-state {
  text-align: center;
  padding: 48px 24px;
}

.empty-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 12px;
}

.empty-state p {
  color: #999;
  font-size: 15px;
}

.conv-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  cursor: pointer;
  transition: background 0.2s;
}

.conv-card:hover {
  background: #fafafa;
}

.conv-card + .conv-card {
  border-top: 1px solid #f5f5f5;
}

.conv-body {
  flex: 1;
  min-width: 0;
}

.conv-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.conv-name {
  font-weight: 600;
  font-size: 15px;
  color: #333;
}

.conv-time {
  font-size: 12px;
  color: #999;
}

.conv-preview {
  font-size: 13px;
  color: #888;
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-badge {
  background: #f56c6c;
  color: #fff;
  font-size: 12px;
  min-width: 20px;
  height: 20px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  padding: 0 5px;
}

.loading-tip {
  text-align: center;
  color: #999;
  padding: 20px;
  font-size: 14px;
}

.notify-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.2s;
}

.notify-card:hover {
  background: #fafafa;
}

.notify-card.unread {
  background: #f0f4ff;
}

.notify-icon {
  font-size: 22px;
  flex-shrink: 0;
}

.notify-body {
  flex: 1;
}

.notify-text {
  font-size: 14px;
  color: #333;
}

.notify-time {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  display: block;
}

.unread-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #f56c6c;
  flex-shrink: 0;
}
</style>