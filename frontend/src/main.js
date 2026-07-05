import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import Login from './views/Login.vue'
import Dashboard from './views/Dashboard.vue'
import Community from './views/Community.vue'
import Notifications from './views/Notifications.vue'
import Profile from './views/Profile.vue'
import PostDetail from './views/PostDetail.vue'
import UserProfile from './views/UserProfile.vue'
import Chat from './views/Chat.vue'
import Settings from './views/Settings.vue'
import BlockList from './views/BlockList.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', redirect: '/notes' },
    { path: '/login', component: Login },
    { path: '/notes', component: Dashboard },
    { path: '/community', component: Community },
    { path: '/post/:id', component: PostDetail },
    { path: '/user/:id', component: UserProfile },
    { path: '/notifications', component: Notifications },
    { path: '/profile', component: Profile },
    { path: '/chat', component: Chat },
    { path: '/settings', component: Settings },
    { path: '/block-list', component: BlockList }
  ]
})

// 路由守卫：未登录跳转登录页
router.beforeEach((to, from, next) => {
  const userStr = localStorage.getItem('userInfo')
  let hasToken = false
  if (userStr) {
    try {
      const user = JSON.parse(userStr)
      hasToken = !!user.token
    } catch (e) { /* ignore */ }
  }
  if (to.path !== '/login' && !hasToken) {
    next('/login')
  } else {
    next()
  }
})

const app = createApp(App)
app.use(router)
app.use(ElementPlus)
app.mount('#app')