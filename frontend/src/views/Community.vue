<template>
  <div class="community-page">
    <div class="community-header">
      <h1>👥 社区</h1>
    </div>

    <!-- 发帖弹窗 -->
    <el-dialog v-model="showComposer" title="发布帖子" width="600px" :close-on-click-modal="true">
      <div class="post-composer">
        <el-input
            v-model="newPostTitle"
            placeholder="标题..."
            size="large"
            class="composer-title"
        />
        <el-input
            v-model="newPostContent"
            type="textarea"
            :rows="5"
            placeholder="分享你的想法..."
            class="composer-content"
        />

        <!-- 已上传图片/视频预览 -->
        <div class="media-preview" v-if="uploadedMedia.length > 0">
          <div v-for="(media, idx) in uploadedMedia" :key="idx" class="media-item">
            <img v-if="media.type === 'image'" :src="media.url" class="media-thumb" />
            <video v-else :src="media.url" class="media-thumb" />
            <el-icon class="media-remove" @click="removeMedia(idx)"><Close /></el-icon>
          </div>
        </div>

        <!-- 已选笔记附件 -->
        <div class="selected-notes" v-if="selectedNotes.length > 0">
          <span class="notes-label">📎 已选笔记：</span>
          <el-tag
              v-for="note in selectedNotes"
              :key="note.documentId"
              closable
              size="small"
              @close="removeNote(note.documentId)"
          >
            {{ note.title }}
          </el-tag>
        </div>

        <div class="composer-actions">
          <div class="composer-tools">
            <el-upload
                :http-request="handleUpload"
                :show-file-list="false"
                accept="image/*,video/*"
            >
              <el-button size="small" circle>🖼️</el-button>
            </el-upload>
            <el-button size="small" circle @click="openNotePicker">📄</el-button>
          </div>
          <el-button type="primary" @click="handleCreatePost" :loading="posting">
            发布
          </el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 笔记选择弹窗 -->
    <el-dialog v-model="showNotePicker" title="选择要附带的笔记" width="480px">
      <div v-if="loadingNotes" class="loading-tip">加载中...</div>
      <div v-else-if="noteList.length === 0" class="empty-comments">
        <p>你还没有笔记，先去写一篇吧~</p>
      </div>
      <div v-for="note in noteList" :key="note.id" class="note-pick-item"
           :class="{ picked: isNotePicked(note.id) }"
           @click="toggleNote(note)">
        <el-checkbox :model-value="isNotePicked(note.id)" />
        <span class="note-pick-title">{{ note.title || '无标题' }}</span>
      </div>
      <template #footer>
        <el-button @click="showNotePicker = false">完成</el-button>
      </template>
    </el-dialog>

    <!-- 帖子流 -->
    <div class="post-feed">
      <div v-if="posts.length === 0 && !loading" class="empty-feed">
        <span class="empty-icon">📭</span>
        <p>还没有帖子，快来发布第一条吧！</p>
      </div>

      <div v-for="post in posts" :key="post.id" class="post-card">
        <div class="post-header">
          <el-avatar :size="40" :src="post.authorAvatar" @click.stop="goToUser(post.authorId, post.authorNickname)" style="cursor:pointer">{{ post.authorNickname?.charAt(0) || 'U' }}</el-avatar>
          <div class="post-meta">
            <span class="post-author" @click.stop="goToUser(post.authorId, post.authorNickname)" style="cursor:pointer">{{ post.authorNickname || '未知用户' }}</span>
            <span class="post-time">{{ formatTime(post.createTime) }}</span>
          </div>
          <el-button
            v-if="String(post.authorId) !== String(currentUserId)"
            :type="post.isFollowed ? 'default' : 'primary'"
            size="small"
            class="follow-btn"
            @click="handleFollow(post)"
          >
            {{ post.isFollowed ? '已关注' : '+ 关注' }}
          </el-button>
          <span v-else class="self-badge-inline">我</span>
        </div>

        <div class="post-body" @click="goToPost(post.id)" style="cursor:pointer">
          <h3 class="post-title" v-if="post.title">{{ post.title }}</h3>
          <div class="post-media-strip" v-if="getPostMedia(post).length > 0">
            <div v-for="(media, idx) in getPostMedia(post).slice(0, 4)" :key="idx" class="media-strip-item">
              <img v-if="media.type === 'image'" :src="media.url" class="media-strip-thumb" />
              <div v-else class="media-strip-video">
                <span class="video-play-icon">▶</span>
              </div>
            </div>
            <div v-if="getPostMedia(post).length > 4" class="media-strip-more">
              +{{ getPostMedia(post).length - 4 }}
            </div>
          </div>
          <div class="post-text" v-text="stripMarkdown(post.content)"></div>
        </div>

        <div class="post-actions">
          <span class="action-btn" @click="handleLike(post)">
            {{ post.isLiked ? '❤️' : '🤍' }} {{ post.likeCount || 0 }}
          </span>
          <span class="action-btn" @click="openComments(post)">
            💬 {{ post.commentCount || 0 }}
          </span>
          <span class="action-btn" @click="handleFavorite(post)">
            {{ post.isFavorited ? '⭐' : '☆' }} {{ post.favoriteCount || 0 }}
          </span>
        </div>
      </div>

      <div v-if="loading" class="loading-tip">加载中...</div>
      <div v-if="!loading && posts.length > 0 && hasMore" class="load-more" @click="loadMore">
        加载更多
      </div>
    </div>

    <!-- 评论弹窗 -->
    <el-dialog
      v-model="showCommentDialog"
      :title="currentPost ? '评论' : ''"
      width="600px"
      :close-on-click-modal="true"
    >
      <div class="comment-list" v-if="currentPost">
        <div v-if="comments.length === 0 && !loadingComments" class="empty-comments">
          <p>暂无评论，快来抢沙发吧~</p>
        </div>

        <div v-for="comment in comments" :key="comment.id" class="comment-item">
          <el-avatar :size="32">{{ comment.authorNickname?.charAt(0) || 'U' }}</el-avatar>
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
      </div>

      <template #footer>
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
      </template>
    </el-dialog>

    <!-- 浮动发帖按钮 -->
    <div class="fab-btn" @click="showComposer = true">
      <span class="fab-icon">+</span>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Close } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import api from '../api'

const md = new MarkdownIt()

const router = useRouter()
const showComposer = ref(false)
const uploadedMedia = ref([])
const showNotePicker = ref(false)
const noteList = ref([])
const selectedNotes = ref([])
const loadingNotes = ref(false)

const posts = ref([])
const loading = ref(false)
const posting = ref(false)
const newPostTitle = ref('')
const newPostContent = ref('')
const currentPage = ref(1)
const hasMore = ref(true)
const pageSize = 10

const showCommentDialog = ref(false)
const currentPost = ref(null)
const comments = ref([])
const newComment = ref('')
const loadingComments = ref(false)
const commentPage = ref(1)

const currentUserId = ref(null)
onMounted(() => {
  const userStr = localStorage.getItem('userInfo')
  if (userStr) {
    try {
      currentUserId.value = JSON.parse(userStr).id
    } catch (e) { /* ignore */ }
  }
})

onMounted(() => {
  loadFeed()
})

async function loadFeed(reset = true) {
  if (loading.value) return
  loading.value = true
  if (reset) {
    currentPage.value = 1
    posts.value = []
  }
  try {
    const res = await api.getFeed({ page: currentPage.value, size: pageSize })
    if (res.code === 200 && res.data) {
      const list = res.data.list || []
      posts.value = reset ? list : [...posts.value, ...list]
      hasMore.value = list.length >= pageSize
    }
  } catch (e) {
    ElMessage.error('加载帖子失败')
  } finally {
    loading.value = false
  }
}

function loadMore() {
  currentPage.value++
  loadFeed(false)
}

async function handleCreatePost() {
  if (!newPostTitle.value.trim() && !newPostContent.value.trim()) {
    return ElMessage.warning('标题和内容至少填一个')
  }
  posting.value = true
  try {
    const payload = {
      title: newPostTitle.value.trim(),
      content: newPostContent.value.trim()
    }
    if (selectedNotes.value.length > 0) {
      payload.documentAccessories = selectedNotes.value.map(n => ({
        documentId: n.documentId || n.id,
        title: n.title
      }))
    }
    const res = await api.createPost(payload)
    if (res.code === 200) {
      ElMessage.success('发布成功')
      newPostTitle.value = ''
      newPostContent.value = ''
      uploadedMedia.value = []
      selectedNotes.value = []
      showComposer.value = false
      loadFeed()
    }
  } catch (e) {
    ElMessage.error('发布失败')
  } finally {
    posting.value = false
  }
}

async function handleLike(post) {
  try {
    const res = await api.toggleLike({ targetType: 'post', targetId: post.id })
    if (res.code === 200 && res.data) {
      post.isLiked = res.data.isLiked
      post.likeCount = res.data.likeCount
    }
  } catch (e) {
    // ignore
  }
}

async function handleFavorite(post) {
  try {
    const res = await api.toggleFavorite({ postId: post.id })
    if (res.code === 200 && res.data) {
      post.isFavorited = res.data.isFavorited
      post.favoriteCount = res.data.favoriteCount
    }
  } catch (e) {
    // ignore
  }
}

function openComments(post) {
  currentPost.value = post
  commentPage.value = 1
  comments.value = []
  newComment.value = ''
  showCommentDialog.value = true
  loadComments()
}

async function loadComments() {
  if (!currentPost.value) return
  loadingComments.value = true
  try {
    const res = await api.listComments({ postId: currentPost.value.id, page: commentPage.value, size: 20 })
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
    const res = await api.createComment({ postId: currentPost.value.id, content: newComment.value.trim() })
    if (res.code === 200) {
      newComment.value = ''
      loadComments()
      if (currentPost.value) {
        currentPost.value.commentCount = (currentPost.value.commentCount || 0) + 1
      }
    }
  } catch (e) {
    ElMessage.error('评论失败')
  }
}

async function handleFollow(post) {
  try {
    const res = await api.toggleFollow({ followeeId: post.authorId })
    if (res.code === 200 && res.data) {
      post.isFollowed = res.data.followed
      ElMessage.success(res.data.followed ? '已关注' : '已取消关注')
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败，请稍后重试')
  }
}

function goToUser(userId, nickname) {
  router.push({ path: `/user/${userId}`, query: { nickname: nickname || '' } })
}

function goToPost(postId) {
  router.push(`/post/${postId}`)
}

async function handleUpload(options) {
  try {
    const res = await api.uploadFile(options.file)
    if (res.code === 200 && res.data) {
      const url = res.data
      const isVideo = options.file.type.startsWith('video/')
      uploadedMedia.value.push({ url, type: isVideo ? 'video' : 'image' })
      const md = isVideo
          ? `\n<video src="${url}" controls></video>\n`
          : `\n![图片](${url})\n`
      newPostContent.value += md
    }
  } catch (e) {
    ElMessage.error('上传失败')
  }
}

function removeMedia(idx) {
  uploadedMedia.value.splice(idx, 1)
}

function openNotePicker() {
  showNotePicker.value = true
  loadNoteList()
}

async function loadNoteList() {
  loadingNotes.value = true
  try {
    const res = await api.listDocuments({ page: 1, size: 100 })
    if (res.code === 200 && res.data) {
      noteList.value = res.data.list || []
    }
  } catch (e) {
    // ignore
  } finally {
    loadingNotes.value = false
  }
}

function isNotePicked(noteId) {
  return selectedNotes.value.some(n => (n.documentId || n.id) === noteId)
}

function toggleNote(note) {
  const id = note.documentId || note.id
  const idx = selectedNotes.value.findIndex(n => (n.documentId || n.id) === id)
  if (idx >= 0) {
    selectedNotes.value.splice(idx, 1)
  } else {
    selectedNotes.value.push({ documentId: id, title: note.title })
  }
}

function removeNote(noteId) {
  const idx = selectedNotes.value.findIndex(n => (n.documentId || n.id) === noteId)
  if (idx >= 0) selectedNotes.value.splice(idx, 1)
}

function renderMarkdown(mk) {
  return md.render(mk || '')
}

function extractMedia(content) {
  if (!content) return []
  const media = []
  const mdImgRegex = /!\[[^\]]*\]\(([^)]+)\)/g
  let match
  while ((match = mdImgRegex.exec(content)) !== null) {
    media.push({ url: match[1], type: 'image' })
  }
  const videoRegex = /<video[^>]*src="([^"]+)"[^>]*>/gi
  while ((match = videoRegex.exec(content)) !== null) {
    media.push({ url: match[1], type: 'video' })
  }
  return media
}

function getPostMedia(post) {
  if (!post._media) {
    post._media = extractMedia(post.content)
  }
  return post._media
}

function stripMarkdown(content) {
  if (!content) return ''
  let text = content
  text = text.replace(/!\[[^\]]*\]\([^)]*\)/g, '')
  text = text.replace(/<video[^>]*src="[^"]+"[^>]*>(?:<\/video>)?/gi, '')
  text = text.replace(/\[[^\]]*\]\([^)]*\)/g, '')
  text = text.replace(/https?:\/\/[^\s)]+/g, '')
  text = text.replace(/<[^>]+>/g, '')
  text = text.replace(/[#*_~`>|!\[\]()\-]/g, ' ')
  text = text.replace(/\s+/g, ' ').trim()
  return text.length > 120 ? text.slice(0, 120) + '...' : text
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
.community-page {
  max-width: 680px;
  margin: 0 auto;
  padding: 24px;
}

.community-header h1 {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin-bottom: 20px;
}

/* 发帖区 */
.post-composer {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.composer-title {
  margin-bottom: 12px;
}

.composer-content {
  margin-bottom: 12px;
}

.composer-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.composer-tools {
  display: flex;
  gap: 8px;
}

.media-preview {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}

.media-item {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
}

.media-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  background: rgba(0,0,0,0.5);
  color: #fff;
  border-radius: 50%;
  cursor: pointer;
  font-size: 14px;
  padding: 2px;
}

.selected-notes {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 12px;
  padding: 8px;
  background: #f9f7ff;
  border-radius: 8px;
}

.notes-label {
  font-size: 13px;
  color: #666;
}

.note-pick-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.note-pick-item:hover {
  background: #f5f3ff;
}

.note-pick-item.picked {
  background: #ede9fe;
}

.note-pick-title {
  font-size: 14px;
  color: #333;
}

/* 帖子流 */
.post-feed {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-feed {
  text-align: center;
  padding: 48px 24px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.empty-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 12px;
}

.empty-feed p {
  color: #999;
  font-size: 15px;
}

/* 帖子卡片 */
.post-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s;
}

.post-card:hover {
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
}

.post-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.post-meta {
  display: flex;
  flex-direction: column;
}

.post-author {
  font-weight: 600;
  font-size: 15px;
  color: #333;
}

.post-time {
  font-size: 12px;
  color: #999;
}

.post-body {
  margin-bottom: 14px;
}

.post-title {
  font-size: 17px;
  font-weight: 600;
  color: #1a1a2e;
  margin-bottom: 8px;
}

.post-content {
  font-size: 15px;
  color: #444;
  line-height: 1.6;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-text {
  font-size: 15px;
  color: #444;
  line-height: 1.6;
  word-break: break-word;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  margin-top: 4px;
}

.post-media-strip {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
  margin-bottom: 10px;
}

.media-strip-item {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  overflow: hidden;
  background: #f5f5f5;
  flex-shrink: 0;
}

.media-strip-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.media-strip-video {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #1a1a2e;
}

.video-play-icon {
  color: #fff;
  font-size: 20px;
}

.media-strip-more {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  background: rgba(0,0,0,0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.post-actions {
  display: flex;
  gap: 24px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.action-btn {
  cursor: pointer;
  font-size: 14px;
  color: #888;
  transition: color 0.2s;
  user-select: none;
}

.action-btn:hover {
  color: #667eea;
}

.loading-tip {
  text-align: center;
  color: #999;
  padding: 20px;
}

.load-more {
  text-align: center;
  color: #667eea;
  cursor: pointer;
  padding: 12px;
  font-size: 14px;
  border-radius: 8px;
  transition: background 0.2s;
}

.load-more:hover {
  background: rgba(102, 126, 234, 0.08);
}

.follow-btn {
  margin-left: auto;
  flex-shrink: 0;
}

.self-badge-inline {
  margin-left: auto;
  flex-shrink: 0;
  display: inline-block;
  padding: 4px 12px;
  font-size: 13px;
  color: #667eea;
  background: rgba(102, 126, 234, 0.1);
  border-radius: 12px;
  font-weight: 600;
}

.comment-list {
  max-height: 400px;
  overflow-y: auto;
}

.empty-comments {
  text-align: center;
  padding: 24px;
  color: #999;
  font-size: 14px;
}

.comment-item {
  display: flex;
  gap: 10px;
  padding: 12px 0;
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
  font-size: 13px;
  color: #333;
}

.comment-reply-to {
  font-size: 12px;
  color: #667eea;
}

.comment-time {
  font-size: 12px;
  color: #999;
}

.comment-content {
  font-size: 14px;
  color: #444;
  line-height: 1.5;
}

.comment-input-row {
  display: flex;
  gap: 10px;
  width: 100%;
}

.comment-input-row .el-input {
  flex: 1;
}

/* 浮动发帖按钮 */
.fab-btn {
  position: fixed;
  bottom: 32px;
  right: 32px;
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea, #764ba2);
  box-shadow: 0 4px 16px rgba(102, 126, 234, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  z-index: 100;
}

.fab-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 6px 24px rgba(102, 126, 234, 0.6);
}

.fab-icon {
  font-size: 28px;
  color: #fff;
  font-weight: 300;
  line-height: 1;
}
</style>