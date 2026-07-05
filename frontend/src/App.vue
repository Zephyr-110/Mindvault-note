<template>
  <!-- 登录页：纯白底，不显示侧边栏 -->
  <div v-if="$route.path === '/login'" class="login-wrapper">
    <router-view />
  </div>

  <!-- 已登录：推特拉杆布局 -->
  <div v-else class="app-layout">
    <!-- 左侧导航栏 -->
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-top">
        <!-- Logo -->
        <div class="logo" @click="$router.push('/notes')">
          <span class="logo-icon">🧠</span>
          <span v-show="!sidebarCollapsed" class="logo-text">MindVault</span>
        </div>

        <!-- 导航菜单 -->
        <nav class="nav-menu">
          <router-link to="/community" class="nav-item" active-class="nav-item--active">
            <span class="nav-icon">👥</span>
            <span v-show="!sidebarCollapsed" class="nav-label">去社区看看</span>
          </router-link>
          <router-link to="/notifications" class="nav-item" active-class="nav-item--active">
            <span class="nav-icon nav-icon-badge">
              🔔
              <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
            </span>
            <span v-show="!sidebarCollapsed" class="nav-label">消息</span>
          </router-link>
          <router-link to="/notes" class="nav-item" active-class="nav-item--active">
            <span class="nav-icon">📝</span>
            <span v-show="!sidebarCollapsed" class="nav-label">去记笔记</span>
          </router-link>
          <router-link to="/profile" class="nav-item" active-class="nav-item--active">
            <span class="nav-icon">👤</span>
            <span v-show="!sidebarCollapsed" class="nav-label">我的</span>
          </router-link>
        </nav>

        <!-- 折叠按钮 -->
        <div class="collapse-btn" @click="sidebarCollapsed = !sidebarCollapsed">
          {{ sidebarCollapsed ? '▶' : '◀' }}
        </div>
      </div>

      <!-- 底部用户信息 -->
      <div class="sidebar-bottom" @click="$router.push('/profile')">
        <el-avatar :size="40" class="user-avatar">
          {{ currentUser?.nickname?.charAt(0) || currentUser?.username?.charAt(0) || 'U' }}
        </el-avatar>
        <div v-show="!sidebarCollapsed" class="user-info">
          <span class="user-name">{{ currentUser?.nickname || currentUser?.username || '未登录' }}</span>
          <span class="user-handle">@{{ currentUser?.username || 'user' }}</span>
        </div>
        <el-button v-show="!sidebarCollapsed" class="logout-btn" link @click.stop="handleLogout">退出</el-button>
      </div>
    </aside>

    <!-- 主内容区 -->
    <main class="main-content" :class="{ expanded: sidebarCollapsed }">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import api from './api'

const router = useRouter()
const currentUser = ref(null)
const sidebarCollapsed = ref(false)
const unreadCount = ref(0)
let pollTimer = null

onMounted(() => {
  const userStr = localStorage.getItem('userInfo')
  if (!userStr) {
    router.push('/login')
    return
  }
  try {
    currentUser.value = JSON.parse(userStr)
  } catch (e) {
    // ignore
  }
  fetchUnreadCount()
  pollTimer = setInterval(fetchUnreadCount, 30000)
})

onUnmounted(() => {
  if (pollTimer) clearInterval(pollTimer)
})

async function fetchUnreadCount() {
  try {
    const [notiRes, msgRes] = await Promise.all([
      api.getUnreadNotificationCount(),
      api.getUnreadMessageCount()
    ])
    let count = 0
    if (notiRes.code === 200 && notiRes.data) {
      count += (notiRes.data.unreadCount || 0)
    }
    if (msgRes.code === 200 && msgRes.data) {
      count += (msgRes.data.unreadCount || 0)
    }
    unreadCount.value = count
  } catch (e) {
    // ignore
  }
}

function handleLogout() {
  localStorage.removeItem('userInfo')
  router.push('/login')
}
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: #f0f2f5;
  min-height: 100vh;
}

.login-wrapper {
  min-height: 100vh;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.app-layout {
  display: flex;
  min-height: 100vh;
}

/* ===== 左侧边栏 ===== */
.sidebar {
  width: 260px;
  min-width: 260px;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  padding: 20px 12px;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
  transition: width 0.25s ease, min-width 0.25s ease;
}

.sidebar.collapsed {
  width: 64px;
  min-width: 64px;
  padding: 20px 8px;
}

.sidebar-top {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* Logo */
.logo {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.2s;
}

.logo:hover {
  background: rgba(255, 255, 255, 0.1);
}

.logo-icon {
  font-size: 28px;
}

.logo-text {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  letter-spacing: -0.5px;
}

/* 导航菜单 */
.nav-menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px;
  border-radius: 12px;
  text-decoration: none;
  color: rgba(255, 255, 255, 0.75);
  font-size: 16px;
  font-weight: 500;
  transition: all 0.2s;
  justify-content: center;
}

.sidebar:not(.collapsed) .nav-item {
  justify-content: flex-start;
  padding: 12px 16px;
}

.nav-item:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.nav-item--active {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  font-weight: 600;
}

.nav-icon {
  font-size: 22px;
  position: relative;
}

.nav-icon-badge {
  position: relative;
}

.badge {
  position: absolute;
  top: -6px;
  right: -6px;
  background: #f56c6c;
  color: #fff;
  font-size: 11px;
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  font-weight: 600;
  line-height: 1;
}

.nav-icon {
  width: 28px;
  text-align: center;
}

.nav-label {
  white-space: nowrap;
}

/* 底部用户区 */
.collapse-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  margin-top: 8px;
  border-radius: 8px;
  cursor: pointer;
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
  transition: all 0.2s;
  user-select: none;
}

.collapse-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.sidebar-bottom {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 12px;
  border-radius: 12px;
  cursor: pointer;
  transition: background 0.2s;
  background: rgba(255, 255, 255, 0.08);
}

.sidebar:not(.collapsed) .sidebar-bottom {
  justify-content: flex-start;
}

.sidebar-bottom:hover {
  background: rgba(255, 255, 255, 0.15);
}

.user-avatar {
  flex-shrink: 0;
}

.user-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.user-name {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-handle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.logout-btn {
  color: rgba(255, 255, 255, 0.5) !important;
  font-size: 13px;
  flex-shrink: 0;
}

.logout-btn:hover {
  color: #fff !important;
}

/* ===== 主内容区 ===== */
.main-content {
  margin-left: 260px;
  flex: 1;
  min-height: 100vh;
  background: #f0f2f5;
  transition: margin-left 0.25s ease;
}

.main-content.expanded {
  margin-left: 64px;
}
</style>