C:\Users\30149\IdeaProjects\MindVault-note-project\frontend\src\views\PostDetail.vue
<template>
  <div class="post-detail-page">
    <!-- 返回按钮 -->
    <div class="back-bar" @click="$router.back()">
      <el-icon class="back-icon"><ArrowLeft /></el-icon>
      <span class="back-text">返回社区</span>
    </div>

    <div class="post-detail-card" v-if="post">
      <!-- 作者信息 -->
      <div class="post-author-bar">
        <el-avatar :size="48" :src="post.authorAvatar" @click="goToUser(post.authorId)">{{ post.authorNickname?.charAt(0) || 'U' }}</el-avatar>
        <div class="author-info" @click="goToUser(post.authorId)">
          <span class="author-name">{{ post.authorNickname || '未知用户' }}</span>
          <span class="post-time">{{ formatTime(post.createTime) }}</span>
        </div>
        <el-button
            v-if="String(post.authorId) !== String(currentUserId)"
            :type="post.isFollowed ? 'default' : 'primary'"
            size="small"
            @click="handleFollow"
        >
          {{ post.isFollowed ? '已关注' : '+ 关注' }}
        </el-button>
        <span v-else class="self-badge">我</span>
      </div>

      <!-- 帖子内容 -->
      <div class="post-body">
        <h2 class="post-title" v-if="post.title">{{ post.title }}</h2>
        <!-- 媒体画廊 -->
        <div class="media-gallery" v-if="parsedContent.media.length > 0">
          <div v-for="(media, idx) in parsedContent.media" :key="idx" class="gallery-item">
            <img v-if="media.type === 'image'" :src="media.url" @click="zoomImage = media.url" />
            <video v-else :src="media.url" controls preload="metadata"></video>
          </div>
        </div>
        <!-- 文字内容 -->
        <div class="post-text-content" v-if="parsedContent.textContent" v-html="renderMarkdown(parsedContent.textContent)"></div>
        <!-- 笔记附件 -->
        <div v-if="post.documentAccessories && post.documentAccessories.length > 0" class="note-attachments">
          <span class="attachment-label">📎 附带的笔记：</span>
          <span
              v-for="note in post.documentAccessories"
              :key="note.documentId"
              class="attachment-tag"
              @click="goToNote(note.documentId)"
          >
            {{ note.title }}
          </span>
        </div>
      </div>

      <!-- 操作栏 -->
      <div class="post-actions-bar">
        <span class="action-btn" @click="handleLike">
          {{ post.isLiked ? '❤️' : '🤍' }} {{ post.likeCount || 0 }}
        </span>
        <span class="action-btn">
          💬 {{ post.commentCount || 0 }}
        </span>
        <span class="action-btn" @click="handleFavorite">
          {{ post.isFavorited ? '⭐' : '☆' }} {{ post.favoriteCount || 0 }}
        </span>
      </div>
    </div>

    <!-- 加载中 -->
    <div v-if="loading" class="loading-tip">加载中...</div>

    <!-- 评论区 -->
    <div class="comment-section" v-if="post">
      <h3 class="comment-title">评论 ({{ comments.length }})</h3>

      <div v-if="comments.length === 0 && !loadingComments" class="empty-comments">
        <p>暂无评论，快来抢沙发吧~</p>
      </div>

      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <el-avatar :size="36">{{ comment.authorNickname?.charAt(0) || 'U' }}</el-avatar>
        <div class="comment-body">
          <div class="comment-top">
            <span class="comment-author">{{ comment.authorNickname || '未知用户' }}</span>
            <span v-if="comment.parentNickname" class="comment-reply-to">
              回复 @{{ comment.parentNickname }}
            </span>
            <span class="comment-time">{{ formatTime(comment.createTime) }}</span>
          </div>
          <p class="comment-content">{{ comment.content }}</p>
        </div>
      </div>

      <div v-if="loadingComments" class="loading-tip">加载中...</div>

      <!-- 发评论 -->
      <div class="comment-input-row">
        <el-input
            v-model="newComment"
            placeholder="写下你的评论..."
            @keyup.enter="handleCreateComment"
        />
        <el-button type="primary" @click="handleCreateComment" :disabled="!newComment.trim()">
          发送
        </el-button>
      </div>
    </div>

    <!-- 图片放大弹窗 -->
    <div v-if="zoomImage" class="image-zoom-overlay" @click="zoomImage = null">
      <img :src="zoomImage" class="zoom-image" />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import MarkdownIt from 'markdown-it'
import api from '../api'

const md = new MarkdownIt()

const route = useRoute()
const router = useRouter()

const post = ref(null)
const zoomImage = ref(null)
const loading = ref(true)
const comments = ref([])
const newComment = ref('')
const loadingComments = ref(false)
const currentUserId = ref(null)

onMounted(() => {
  const userStr = localStorage.getItem('userInfo')
  if (userStr) {
    try {
      currentUserId.value = JSON.parse(userStr).id
    } catch (e) { /* ignore */ }
  }
  loadPost()
})

function parseContent(content) {
  if (!content) return { media: [], textContent: '' }
  const media = []
  let textContent = content.replace(/!\[[^\]]*\]\(([^)]+)\)/g, (_, url) => {
    media.push({ url, type: 'image' })
    return ''
  })
  textContent = textContent.replace(/<video[^>]*src="([^"]+)"[^>]*>(?:<\/video>)?/gi, (_, url) => {
    media.push({ url, type: 'video' })
    return ''
  })
  return { media, textContent: textContent.trim() }
}

const parsedContent = computed(() => {
  if (!post.value) return { media: [], textContent: '' }
  return parseContent(post.value.content)
})

async function loadPost() {
  loading.value = true
  try {
    const res = await api.getPostDetail({ postId: Number(route.params.id) })
    if (res.code === 200 && res.data) {
      post.value = res.data
      loadComments()
    }
  } catch (e) {
    ElMessage.error('加载帖子失败')
  } finally {
    loading.value = false
  }
}

async function loadComments() {
  loadingComments.value = true
  try {
    const res = await api.listComments({ postId: post.value.id, page: 1, size: 50 })
    if (res.code === 200 && res.data) {
      comments.value = res.data.list || []
    }
  } catch (e) {
    // ignore
  } finally {
    loadingComments.value = false
  }
}

async function handleCreateComment() {
  if (!newComment.value.trim()) return
  try {
    const res = await api.createComment({ postId: post.value.id, content: newComment.value.trim() })
    if (res.code === 200) {
      newComment.value = ''
      loadComments()
      post.value.commentCount = (post.value.commentCount || 0) + 1
    }
  } catch (e) {
    ElMessage.error('评论失败')
  }
}

async function handleLike() {
  try {
    const res = await api.toggleLike({ targetType: 'post', targetId: post.value.id })
    if (res.code === 200 && res.data) {
      post.value.isLiked = res.data.isLiked
      post.value.likeCount = res.data.likeCount
    }
  } catch (e) {
    // ignore
  }
}

async function handleFavorite() {
  try {
    const res = await api.toggleFavorite({ postId: post.value.id })
    if (res.code === 200 && res.data) {
      post.value.isFavorited = res.data.isFavorited
      post.value.favoriteCount = res.data.favoriteCount
    }
  } catch (e) {
    // ignore
  }
}

async function handleFollow() {
  try {
    const res = await api.toggleFollow({ followeeId: post.value.authorId })
    if (res.code === 200 && res.data) {
      post.value.isFollowed = res.data.followed
      ElMessage.success(res.data.followed ? '已关注' : '已取消关注')
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败，请稍后重试')
  }
}

function goToUser(userId) {
  router.push(`/user/${userId}`)
}

function goToNote(docId) {
  router.push(`/notes?docId=${docId}`)
}

function handleContentClick(e) {
  if (e.target.tagName === 'IMG') {
    zoomImage.value = e.target.src
  }
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

function renderMarkdown(mk) {
  return md.render(mk || '')
}
</script>

<style scoped>
.post-detail-page {
  max-width: 720px;
  margin: 0 auto;
  padding: 24px;
}

.post-detail-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 20px;
}

.post-author-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}

.author-info {
  flex: 1;
  cursor: pointer;
}

.author-name {
  font-weight: 600;
  font-size: 15px;
  color: #333;
  display: block;
}

.post-time {
  font-size: 12px;
  color: #999;
}

.self-badge {
  display: inline-block;
  padding: 4px 12px;
  font-size: 13px;
  color: #667eea;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 12px;
  font-weight: 600;
}

.post-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 12px;
}

.media-gallery {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.gallery-item {
  width: 120px;
  height: 120px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
}

.gallery-item img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
  transition: opacity 0.2s;
}

.gallery-item img:hover {
  opacity: 0.85;
}

.gallery-item video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 8px;
}

.post-text-content {
  font-size: 15px;
  line-height: 1.8;
  color: #444;
  word-break: break-word;
}

.note-attachments {
  margin-top: 16px;
  padding: 12px;
  background: #f8f9ff;
  border-radius: 8px;
}

.attachment-label {
  font-size: 13px;
  color: #666;
  margin-right: 8px;
}

.attachment-tag {
  display: inline-block;
  background: #e8eaff;
  color: #4a5af5;
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 13px;
  margin: 3px 6px 3px 0;
  cursor: pointer;
}

.attachment-tag:hover {
  background: #d0d5ff;
}

.post-actions-bar {
  display: flex;
  gap: 32px;
  padding-top: 16px;
  margin-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.action-btn {
  font-size: 15px;
  color: #666;
  cursor: pointer;
  user-select: none;
}

.action-btn:hover {
  color: #333;
}

.comment-section {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.comment-title {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.empty-comments {
  text-align: center;
  padding: 32px;
  color: #999;
}

.comment-item {
  display: flex;
  gap: 10px;
  padding: 14px 0;
  border-bottom: 1px solid #f5f5f5;
}

.comment-body {
  flex: 1;
}

.comment-top {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.comment-author {
  font-weight: 600;
  font-size: 14px;
  color: #333;
}

.comment-reply-to {
  font-size: 12px;
  color: #4a5af5;
}

.comment-time {
  font-size: 12px;
  color: #999;
  margin-left: auto;
}

.comment-content {
  font-size: 14px;
  color: #555;
  line-height: 1.6;
}

.comment-input-row {
  display: flex;
  gap: 10px;
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #f0f0f0;
}

.loading-tip {
  text-align: center;
  color: #999;
  padding: 24px;
}

/* 返回按钮 */
.back-bar {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  padding: 10px 18px;
  cursor: pointer;
  color: #555;
  font-size: 15px;
  font-weight: 500;
  border-radius: 24px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.2s;
}

.back-bar:hover {
  color: #667eea;
  background: #f5f3ff;
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.2);
  transform: translateX(-2px);
}

.back-icon {
  font-size: 18px;
}

.image-zoom-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.85);
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}

.zoom-image {
  max-width: 90vw;
  max-height: 90vh;
  border-radius: 8px;
  object-fit: contain;
}
</style>