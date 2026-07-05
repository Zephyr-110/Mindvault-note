<template>
  <div class="profile-page">
    <div class="page-header">
      <span class="back-btn" @click="$router.back()">←</span>
      <span class="header-title">我的主页</span>
    </div>
    <!-- 用户信息卡片 -->
    <div class="profile-card">
      <el-dropdown class="profile-menu" trigger="click" @command="handleMenuCommand">
        <span class="menu-trigger">⋯</span>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="settings">⚙️ 设置</el-dropdown-item>
            <el-dropdown-item command="blacklist">🚫 黑名单</el-dropdown-item>
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

      <el-button type="primary" @click="goToEditProfile">编辑资料</el-button>
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

    <!-- 我的帖子 -->
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

    <!-- 我的笔记 -->
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
        <el-button size="small" :type="user.followed ? 'default' : 'primary'"
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
        <el-button size="small" :type="user.followed ? 'default' : 'primary'"
                   @click.stop="handleFollowUser(user)">
          {{ user.followed ? '已关注' : '+ 关注' }}
        </el-button>
      </div>

      <div v-if="followLoading" class="loading-tip">加载中...</div>
    </div>

    <!-- 编辑资料弹窗 -->
    <el-dialog v-model="editVisible" title="✏️ 编辑资料" width="480px">
      <el-form :model="editForm" label-width="70px">
        <el-form-item label="头像">
          <div style="display: flex; align-items: center; gap: 12px;">
            <el-avatar :size="60" :src="editForm.avatar">{{ editForm.nickname?.charAt(0) || 'U' }}</el-avatar>
            <el-upload :show-file-list="false" :before-upload="handleAvatarUpload" accept="image/*">
              <el-button size="small">更换头像</el-button>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" placeholder="设置昵称" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" placeholder="设置手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="设置邮箱" />
        </el-form-item>
        <el-form-item label="性别">
          <el-select v-model="editForm.gender" placeholder="选择性别" style="width: 100%">
            <el-option label="保密" :value="0" />
            <el-option label="男" :value="1" />
            <el-option label="女" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="年龄">
          <el-input-number v-model="editForm.age" :min="1" :max="150" style="width: 100%" />
        </el-form-item>
        <el-form-item label="地区">
          <el-input v-model="editForm.region" placeholder="设置地区" />
        </el-form-item>
        <el-form-item label="个性签名">
          <el-input v-model="editForm.bio" type="textarea" :rows="3" placeholder="写一句个性签名..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEditProfile" :loading="editSaving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const router = useRouter()

const currentUserId = ref(null)
const userInfo = ref({ nickname: '', bio: '', avatar: '', gender: null, age: null, region: '' })
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

const editVisible = ref(false)
const editSaving = ref(false)
const editForm = reactive({
  nickname: '', avatar: '', phone: '', email: '',
  gender: null, age: null, region: '', bio: ''
})

onMounted(() => {
  const userStr = localStorage.getItem('userInfo')
  if (userStr) {
    try {
      currentUserId.value = JSON.parse(userStr).id
    } catch (e) { /* ignore */ }
  }
  loadUserProfile()
  loadUserPosts()
})

async function loadUserProfile() {
  try {
    const res = await api.getUserProfile()
    if (res.code === 200 && res.data) {
      userInfo.value = res.data
    }

    const followeeRes = await api.listFollowee({ userId: currentUserId.value })
    if (followeeRes.code === 200 && followeeRes.data) {
      followeeCount.value = followeeRes.data.total || followeeRes.data.list?.length || 0
    }
    const followerRes = await api.listFollower({ userId: currentUserId.value })
    if (followerRes.code === 200 && followerRes.data) {
      followerCount.value = followerRes.data.total || followerRes.data.list?.length || 0
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
      posts.value = all.filter(p => p.authorId === currentUserId.value)
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
    const res = await api.listDocuments({ userId: currentUserId.value, page: 1, size: 50 })
    if (res.code === 200 && res.data) {
      notes.value = res.data.list || []
    }
  } catch (e) {
    // ignore
  } finally {
    notesLoading.value = false
  }
}

async function loadFollowList(type) {
  followLoading.value = true
  try {
    const apiFn = type === 'followees' ? api.listFollowee : api.listFollower
    const res = await apiFn({ userId: currentUserId.value, page: 1, size: 100 })
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

async function handleFollowUser(user) {
  try {
    const res = await api.toggleFollow({ followeeId: user.userId })
    if (res.code === 200 && res.data) {
      user.followed = res.data.followed
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

function goToUser(userId, nickname) {
  router.push({ path: `/user/${userId}`, query: { nickname: nickname || '' } })
}

function goToEditProfile() {
  Object.assign(editForm, {
    nickname: userInfo.value.nickname || '',
    avatar: userInfo.value.avatar || '',
    phone: userInfo.value.phone || '',
    email: userInfo.value.email || '',
    gender: userInfo.value.gender,
    age: userInfo.value.age,
    region: userInfo.value.region || '',
    bio: userInfo.value.bio || ''
  })
  editVisible.value = true
}

async function submitEditProfile() {
  editSaving.value = true
  try {
    const res = await api.updateUserProfile({
      nickname: editForm.nickname || undefined,
      avatar: editForm.avatar || undefined,
      phone: editForm.phone || undefined,
      email: editForm.email || undefined,
      gender: editForm.gender,
      age: editForm.age,
      region: editForm.region || undefined,
      bio: editForm.bio || undefined
    })
    if (res.code === 200) {
      ElMessage.success('保存成功')
      userInfo.value = { ...editForm }
      editVisible.value = false
    }
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    editSaving.value = false
  }
}

async function handleAvatarUpload(file) {
  try {
    const res = await api.uploadFile(file)
    if (res.code === 200) {
      editForm.avatar = res.data
      ElMessage.success('头像上传成功')
    }
  } catch (e) {
    ElMessage.error('头像上传失败')
  }
  return false
}

function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'followees' && followeeList.value.length === 0) loadFollowList('followees')
  if (tab === 'followers' && followerList.value.length === 0) loadFollowList('followers')
  if (tab === 'posts' && posts.value.length === 0) loadUserPosts()
  if (tab === 'notes' && notes.value.length === 0) loadUserNotes()
}

function handleMenuCommand(cmd) {
  if (cmd === 'settings') {
    router.push('/settings')
  } else if (cmd === 'blacklist') {
    router.push('/block-list')
  }
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

function formatTime(time) {
  if (!time) return ''
  const d = new Date(time)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 86400000) return `${Math.floor(diff / 86400000)}天前`
  return d.toLocaleDateString()
}
</script>

<style scoped>
.profile-page {
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
  margin-bottom: 24px;
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
}

.tab-item.active {
  color: #409eff;
  border-bottom: 2px solid #409eff;
  background: #f8fcff;
}

.user-content {
  background: #fff;
  border-radius: 0 0 16px 16px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  padding: 16px 24px 24px;
  min-height: 200px;
}

.empty-state {
  text-align: center;
  padding: 48px 24px;
  color: #999;
}

.empty-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 12px;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}

.post-card {
  padding: 16px;
  border-radius: 12px;
  background: #fafafa;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.post-card:hover {
  background: #f5f5f5;
}

.post-preview {
  border-radius: 8px;
  overflow: hidden;
  margin-bottom: 10px;
}

.post-thumb {
  width: 100%;
  max-height: 200px;
  object-fit: cover;
}

.post-title {
  margin: 0 0 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1a1a2e;
}

.post-content {
  font-size: 14px;
  color: #666;
  line-height: 1.5;
}

.post-meta {
  margin-top: 10px;
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #999;
}

.note-card {
  display: flex;
  align-items: center;
  padding: 16px;
  border-radius: 12px;
  background: #fafafa;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.note-card:hover {
  background: #f5f5f5;
}

.note-info {
  flex: 1;
}

.note-title {
  margin: 0 0 4px;
  font-size: 15px;
  font-weight: 600;
  color: #1a1a2e;
}

.note-preview {
  margin: 0;
  font-size: 13px;
  color: #888;
}

.arrow {
  color: #ccc;
  font-size: 20px;
  font-weight: 300;
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
  transition: all 0.2s;
}

.follow-user-item:hover {
  background: #f5f5f5;
}

.follow-user-info {
  flex: 1;
}

.follow-user-name {
  display: block;
  font-size: 15px;
  font-weight: 500;
  color: #1a1a2e;
  margin-bottom: 2px;
}

.follow-user-bio {
  display: block;
  font-size: 13px;
  color: #888;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.loading-tip {
  text-align: center;
  padding: 32px;
  color: #999;
  font-size: 14px;
}
</style>