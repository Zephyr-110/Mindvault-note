C:\Users\30149\IdeaProjects\MindVault-note-project\frontend\src\views\UserProfile.vue
<template>
  <div class="user-profile-page">
    <div class="page-header">
      <span class="back-btn" @click="$router.back()">←</span>
      <span class="header-title">用户主页</span>
    </div>
    <!-- 用户信息卡片 -->
    <div class="profile-card">
      <el-dropdown v-if="!isSelf" class="profile-menu" trigger="click" @command="handleMenuCommand">
        <span class="menu-trigger">⋯</span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item :command="isBlocked ? 'unblock' : 'block'">
              {{ isBlocked ? '取消拉黑' : '🚫 拉黑' }}
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
      <el-avatar :size="80" :src="userInfo.avatar" class="profile-avatar">{{ userInfo.nickname?.charAt(0) || 'U' }}</el-avatar>
      <h2 class="profile-nickname">{{ userInfo.nickname || '未知用户' }}</h2>
      <p class="profile-bio">{{ userInfo.bio || '这个人很懒，什么都没写...' }}</p>

      <div class="profile-info-row" v-if="userInfo.gender || userInfo.age || userInfo.region">
        <span v-if="userInfo.gender" class="info-tag">{{ userInfo.gender === 1 ? '男' : userInfo.gender === 2 ? '女' : '保密' }}</span>
        <span v-if="userInfo.age" class="info-tag">{{ userInfo.age }}岁</span>
        <span v-if="userInfo.region" class="info-tag">📍 {{ userInfo.region }}</span>
      </div>

      <div class="profile-stats">
        <div class="stat-item">
          <span class="stat-num">{{ postCount }}</span>
          <span class="stat-label">帖子</span>
        </div>
        <div class="stat-item" @click="switchTab('followees')">
          <span class="stat-num">{{ followeeCount }}</span>
          <span class="stat-label">关注</span>
        </div>
        <div class="stat-item" @click="switchTab('followers')">
          <span class="stat-num">{{ followerCount }}</span>
          <span class="stat-label">粉丝</span>
        </div>
      </div>

      <div class="profile-actions" v-if="!isSelf">
        <el-button
            :type="isFollowed ? 'default' : 'primary'"
            @click="handleFollow"
        >
          {{ isFollowed ? '已关注' : '+ 关注' }}
        </el-button>
        <el-button type="primary" @click="goToChat">💬 私信</el-button>
      </div>
    </div>

    <!-- 内容 Tab -->
    <div class="content-tabs">
      <span
          class="tab-item"
          :class="{ active: activeTab === 'posts' }"
          @click="switchTab('posts')"
      >
        帖子
      </span>
      <span
          class="tab-item"
          :class="{ active: activeTab === 'notes' }"
          @click="switchTab('notes')"
      >
        笔记
      </span>
      <span
          class="tab-item"
          :class="{ active: activeTab === 'followees' }"
          @click="switchTab('followees')"
      >
        关注
      </span>
      <span
          class="tab-item"
          :class="{ active: activeTab === 'followers' }"
          @click="switchTab('followers')"
      >
        粉丝
      </span>
    </div>

    <!-- 他的帖子 -->
    <div v-if="activeTab === 'posts'" class="user-content">
      <div v-if="posts.length === 0 && !loading" class="empty-state">
        <span class="empty-icon">📭</span>
        <p>暂无帖子</p>
      </div>

      <div v-for="post in posts" :key="post.id" class="post-card" @click="goToPost(post.id)">
        <div class="post-preview" v-if="extractFirstImage(post.content)">
          <img :src="extractFirstImage(post.content)" class="post-thumb" />
        </div>
        <h4 class="post-title" v-if="post.title">{{ post.title }}</h4>
        <div class="post-content" v-if="!extractFirstImage(post.content)">{{ truncateContent(post.content) }}</div>
        <div class="post-meta">
          <span>❤️ {{ post.likeCount || 0 }}</span>
          <span>💬 {{ post.commentCount || 0 }}</span>
          <span>⭐ {{ post.favoriteCount || 0 }}</span>
          <span class="post-time">{{ formatTime(post.createTime) }}</span>
        </div>
      </div>

      <div v-if="loading" class="loading-tip">加载中...</div>
    </div>

    <!-- 他的笔记 -->
    <div v-if="activeTab === 'notes'" class="user-content">
      <div v-if="notes.length === 0 && !notesLoading" class="empty-state">
        <span class="empty-icon">📝</span>
        <p>暂无笔记</p>
      </div>

      <div v-for="doc in notes" :key="doc.id" class="note-card" @click="goToNote(doc.id)">
        <div class="note-info">
          <h4 class="note-title">{{ doc.title || '无标题' }}</h4>
          <p class="note-preview">{{ doc.preview || doc.contentPreview || '...' }}</p>
        </div>
        <span class="arrow">›</span>
      </div>

      <div v-if="notesLoading" class="loading-tip">加载中...</div>
    </div>

    <!-- 关注列表 -->
    <div v-if="activeTab === 'followees'" class="user-content">
      <div v-if="followeeList.length === 0 && !followLoading" class="empty-state">
        <span class="empty-icon">👥</span>
        <p>还没有关注任何人</p>
      </div>

      <div v-for="user in followeeList" :key="user.userId" class="follow-user-item" @click="goToUser(user.userId, user.nickname)">
        <el-avatar :size="48" :src="user.avatar">{{ user.nickname?.charAt(0) || 'U' }}</el-avatar>
        <div class="follow-user-info">
          <span class="follow-user-name">{{ user.nickname || '未知用户' }}</span>
          <span class="follow-user-bio">{{ user.bio || '' }}</span>
        </div>
        <el-button v-if="user.userId !== currentUserId" size="small" :type="user.followed ? 'default' : 'primary'"
                   @click.stop="handleFollowUser(user)">
          {{ user.followed ? '已关注' : '+ 关注' }}
        </el-button>
      </div>

      <div v-if="followLoading" class="loading-tip">加载中...</div>
    </div>

    <!-- 粉丝列表 -->
    <div v-if="activeTab === 'followers'" class="user-content">
      <div v-if="followerList.length === 0 && !followLoading" class="empty-state">
        <span class="empty-icon">👥</span>
        <p>还没有粉丝</p>
      </div>

      <div v-for="user in followerList" :key="user.userId" class="follow-user-item" @click="goToUser(user.userId, user.nickname)">
        <el-avatar :size="48" :src="user.avatar">{{ user.nickname?.charAt(0) || 'U' }}</el-avatar>
        <div class="follow-user-info">
          <span class="follow-user-name">{{ user.nickname || '未知用户' }}</span>
          <span class="follow-user-bio">{{ user.bio || '' }}</span>
        </div>
        <el-button v-if="user.userId !== currentUserId" size="small" :type="user.followed ? 'default' : 'primary'"
                   @click.stop="handleFollowUser(user)">
          {{ user.followed ? '已关注' : '+ 关注' }}
        </el-button>
      </div>

      <div v-if="followLoading" class="loading-tip">加载中...</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const route = useRoute()
const router = useRouter()

const userId = computed(() => Number(route.params.id))
const currentUserId = ref(null)
const isSelf = computed(() => currentUserId.value === userId.value)

const userInfo = ref({ nickname: '', bio: '', avatar: '', gender: null, age: null, region: '' })
const isFollowed = ref(false)
const followeeCount = ref(0)
const followerCount = ref(0)
const posts = ref([])
const postCount = ref(0)
const loading = ref(false)

const activeTab = ref('posts')
const notes = ref([])
const notesLoading = ref(false)

const followeeList = ref([])
const followerList = ref([])
const followLoading = ref(false)
const isBlocked = ref(false)

onMounted(() => {
  const userStr = localStorage.getItem('userInfo')
  if (userStr) {
    try {
      currentUserId.value = JSON.parse(userStr).id
    } catch (e) { /* ignore */ }
  }
  loadUserProfile()
  loadUserPosts()
  if (!isSelf.value) {
    loadBlockStatus()
  }
})

async function loadUserProfile() {
  try {
    if (isSelf.value) {
      const res = await api.getUserProfile()
      if (res.code === 200 && res.data) {
        userInfo.value = res.data
      }
    } else {
      const res = await api.getUserProfileById(userId.value)
      if (res.code === 200 && res.data) {
        userInfo.value = res.data
      }
    }

    const followeeRes = await api.listFollowee({ userId: userId.value })
    if (followeeRes.code === 200 && followeeRes.data) {
      followeeCount.value = followeeRes.data.total || followeeRes.data.list?.length || 0
    }
    const followerRes = await api.listFollower({ userId: userId.value })
    if (followerRes.code === 200 && followerRes.data) {
      const list = followerRes.data.list || []
      followerCount.value = followerRes.data.total || list.length || 0
      isFollowed.value = list.some(f => f.userId === currentUserId.value)
    }
  } catch (e) {
    // ignore
  }
}

async function loadUserPosts() {
  loading.value = true
  try {
    const res = await api.getFeed({ page: 1, size: 20 })
    if (res.code === 200 && res.data) {
      const all = res.data.list || []
      posts.value = all.filter(p => p.authorId === userId.value)
      postCount.value = posts.value.length
    }
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

async function loadUserNotes() {
  notesLoading.value = true
  try {
    const res = await api.listDocuments({ userId: userId.value, page: 1, size: 50 })
    if (res.code === 200 && res.data) {
      notes.value = res.data.list || []
    }
  } catch (e) {
    // ignore
  } finally {
    notesLoading.value = false
  }
}

async function handleFollow() {
  try {
    const res = await api.toggleFollow({ followeeId: userId.value })
    if (res.code === 200 && res.data) {
      isFollowed.value = res.data.followed
      followerCount.value = res.data.followerCount
      followeeCount.value = res.data.followingCount
      ElMessage.success(res.data.followed ? '已关注' : '已取消关注')
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败，请稍后重试')
  }
}

function goToPost(postId) {
  router.push(`/post/${postId}`)
}

function goToNote(docId) {
  router.push(`/notes?docId=${docId}`)
}

function extractFirstImage(content) {
  if (!content) return null
  const mdMatch = content.match(/!\[.*?\]\((.+?)\)/)
  if (mdMatch) return mdMatch[1]
  const htmlMatch = content.match(/<img[^>]+src=["']([^"']+)["']/)
  if (htmlMatch) return htmlMatch[1]
  return null
}

function truncateContent(content) {
  if (!content) return ''
  const clean = content.replace(/[!\[\]\(\)\r\n]/g, ' ').trim()
  return clean.length > 60 ? clean.slice(0, 60) + '...' : clean
}

function goToUser(userId, nickname) {
  router.push({ path: `/user/${userId}`, query: { nickname: nickname || '' } })
}

async function loadFollowList(type) {
  followLoading.value = true
  try {
    const apiFn = type === 'followees' ? api.listFollowee : api.listFollower
    const res = await apiFn({ userId: userId.value, page: 1, size: 100 })
    if (res.code === 200 && res.data) {
      const list = res.data.list || []
      if (type === 'followees') {
        followeeList.value = list
      } else {
        followerList.value = list
      }
    }
  } catch (e) {
    // ignore
  } finally {
    followLoading.value = false
  }
}

function goToChat() {
  router.push(`/chat?with=${userId.value}`)
}

function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'followees' && followeeList.value.length === 0) loadFollowList('followees')
  if (tab === 'followers' && followerList.value.length === 0) loadFollowList('followers')
  if (tab === 'posts' && posts.value.length === 0) loadUserPosts()
  if (tab === 'notes' && notes.value.length === 0) loadUserNotes()
}

async function loadBlockStatus() {
  try {
    const res = await api.isBlocked({ userId: userId.value })
    if (res.code === 200) {
      isBlocked.value = res.data
    }
  } catch (e) { /* ignore */ }
}

async function handleMenuCommand(cmd) {
  if (cmd === 'block') {
    try {
      await ElMessageBox.confirm('确定要拉黑该用户吗？拉黑后对方将无法与你互动。', '拉黑确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      await api.blockUser({ userId: userId.value })
      isBlocked.value = true
      ElMessage.success('已拉黑')
    } catch (e) {
      // 取消或失败
    }
  } else if (cmd === 'unblock') {
    try {
      await api.unblockUser({ userId: userId.value })
      isBlocked.value = false
      ElMessage.success('已取消拉黑')
    } catch (e) {
      ElMessage.error('操作失败')
    }
  }
}

async function handleFollowUser(user) {
  try {
    const res = await api.toggleFollow({ followeeId: user.userId })
    if (res.code === 200 && res.data) {
      user.followed = res.data.followed
      followerCount.value = res.data.followerCount
      followeeCount.value = res.data.followingCount
      ElMessage.success(res.data.followed ? '已关注' : '已取消关注')
    } else {
      ElMessage.error(res.msg || '操作失败')
    }
  } catch (e) {
    ElMessage.error('操作失败，请稍后重试')
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
</script>

<style scoped>
.user-profile-page {
  max-width: 720px;
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
  font-size: 22px;
  cursor: pointer;
  color: #555;
}

.header-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
}

.profile-card {
  position: relative;
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  text-align: center;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.profile-menu {
  position: absolute;
  top: 16px;
  right: 20px;
}

.menu-trigger {
  font-size: 22px;
  color: #666;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.menu-trigger:hover {
  background: #f0f0f0;
}

.profile-avatar {
  margin-bottom: 12px;
}

.profile-nickname {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 8px;
}

.profile-bio {
  font-size: 14px;
  color: #888;
  margin: 0 0 16px;
}

.profile-info-row {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.info-tag {
  font-size: 13px;
  color: #667eea;
  background: #f0edff;
  padding: 4px 12px;
  border-radius: 12px;
}

.profile-stats {
  display: flex;
  justify-content: center;
  gap: 40px;
  margin-bottom: 20px;
}

.stat-item {
  cursor: pointer;
  text-align: center;
}

.stat-num {
  font-size: 20px;
  font-weight: 700;
  color: #333;
  display: block;
}

.stat-label {
  font-size: 13px;
  color: #999;
}

.profile-actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}

.content-tabs {
  background: #fff;
  border-radius: 16px 16px 0 0;
  padding: 0 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  gap: 0;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 16px 0;
  font-size: 15px;
  font-weight: 500;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 2px solid transparent;
}

.tab-item.active {
  color: #667eea;
  border-bottom-color: #667eea;
}

.user-content {
  background: #fff;
  border-radius: 0 0 16px 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 24px;
}

.user-posts {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  margin-bottom: 24px;
}

.section-title {
  font-size: 17px;
  font-weight: 600;
  color: #333;
  margin: 0 0 16px;
}

.empty-state {
  text-align: center;
  padding: 32px;
  color: #999;
}

.follow-user-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  border-radius: 12px;
  background: #fafafa;
  margin-bottom: 10px;
  cursor: pointer;
  transition: background 0.2s;
}

.follow-user-item:hover {
  background: #f0f0f0;
}

.follow-user-info {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.follow-user-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.follow-user-bio {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.empty-comments {
  text-align: center;
  padding: 24px;
  color: #999;
  font-size: 14px;
}

.empty-icon {
  font-size: 36px;
  display: block;
  margin-bottom: 8px;
}

.post-card {
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
}

.post-card:last-child {
  border-bottom: none;
}

.post-card:hover {
  background: #fafafa;
}

.post-preview {
  margin-bottom: 10px;
}

.post-thumb {
  width: 100%;
  max-height: 180px;
  object-fit: cover;
  border-radius: 8px;
}

.post-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 8px;
}

.post-content {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
  margin: 0 0 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-meta {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #999;
}

.post-time {
  margin-left: auto;
}

.loading-tip {
  text-align: center;
  color: #999;
  padding: 24px;
}

.note-card {
  display: flex;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
  transition: background 0.2s;
}

.note-card:last-child {
  border-bottom: none;
}

.note-card:hover {
  background: #fafbff;
}

.note-info {
  flex: 1;
}

.note-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 4px;
}

.note-preview {
  font-size: 13px;
  color: #888;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.note-card .arrow {
  font-size: 20px;
  color: #ccc;
  flex-shrink: 0;
  margin-left: 12px;
}
</style>