C:\Users\30149\IdeaProjects\MindVault-note-project\frontend\src\views\Settings.vue
<template>
  <div class="settings-page">
    <div class="page-header">
      <span class="back-btn" @click="$router.back()">←</span>
      <h2 class="page-title">设置</h2>
    </div>

    <!-- 外观设置 -->
    <section class="setting-group">
      <h3 class="group-title">🎨 外观设置</h3>
      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">主题</span>
          <span class="label-desc">{{ themeLabel }}</span>
        </div>
        <el-radio-group v-model="settings['appearance.theme']" @change="v => update('appearance.theme', v)">
          <el-radio-button value="default">☀️ 默认</el-radio-button>
          <el-radio-button value="warm">🔥 暖色</el-radio-button>
          <el-radio-button value="dark">🌙 深色</el-radio-button>
        </el-radio-group>
      </div>
      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">字体大小</span>
          <span class="label-desc">{{ fontSizeLabel }}</span>
        </div>
        <el-radio-group v-model="settings['appearance.fontSize']" @change="v => update('appearance.fontSize', v)">
          <el-radio-button value="small">小</el-radio-button>
          <el-radio-button value="medium">中</el-radio-button>
          <el-radio-button value="large">大</el-radio-button>
        </el-radio-group>
      </div>
    </section>

    <!-- 编辑器设置 -->
    <section class="setting-group">
      <h3 class="group-title">✏️ 编辑器设置</h3>
      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">自动保存</span>
          <span class="label-desc">{{ settings['editor.autoSave'] === 'true' ? '已开启' : '已关闭' }}</span>
        </div>
        <el-switch
            :model-value="settings['editor.autoSave'] === 'true'"
            @change="v => update('editor.autoSave', v ? 'true' : 'false')"
        />
      </div>
      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">默认视图</span>
          <span class="label-desc">打开笔记时的默认展示方式</span>
        </div>
        <el-radio-group v-model="settings['editor.defaultView']" @change="v => update('editor.defaultView', v)">
          <el-radio-button value="normal">📄 普通</el-radio-button>
          <el-radio-button value="2d">🗺️ 导图</el-radio-button>
          <el-radio-button value="3d">🌳 3D</el-radio-button>
        </el-radio-group>
      </div>
    </section>

    <!-- 通知设置 -->
    <section class="setting-group">
      <h3 class="group-title">🔔 通知设置</h3>
      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">点赞通知</span>
          <span class="label-desc">有人点赞你的内容时通知</span>
        </div>
        <el-switch
            :model-value="settings['notification.like'] === 'true'"
            @change="v => update('notification.like', v ? 'true' : 'false')"
        />
      </div>
      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">评论通知</span>
          <span class="label-desc">有人评论你的帖子时通知</span>
        </div>
        <el-switch
            :model-value="settings['notification.comment'] === 'true'"
            @change="v => update('notification.comment', v ? 'true' : 'false')"
        />
      </div>
      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">回复通知</span>
          <span class="label-desc">有人回复你的评论时通知</span>
        </div>
        <el-switch
            :model-value="settings['notification.reply'] === 'true'"
            @change="v => update('notification.reply', v ? 'true' : 'false')"
        />
      </div>
      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">关注通知</span>
          <span class="label-desc">有人关注你时通知</span>
        </div>
        <el-switch
            :model-value="settings['notification.follow'] === 'true'"
            @change="v => update('notification.follow', v ? 'true' : 'false')"
        />
      </div>
      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">收藏通知</span>
          <span class="label-desc">有人收藏你的帖子时通知</span>
        </div>
        <el-switch
            :model-value="settings['notification.favorite'] === 'true'"
            @change="v => update('notification.favorite', v ? 'true' : 'false')"
        />
      </div>
    </section>

    <!-- 隐私设置 -->
    <section class="setting-group">
      <h3 class="group-title">🔒 隐私设置</h3>
      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">主页可见性</span>
          <span class="label-desc">{{ visibilityLabel }}</span>
        </div>
        <el-radio-group v-model="settings['privacy.profileVisibility']" @change="v => update('privacy.profileVisibility', v)">
          <el-radio-button value="public">🌐 公开</el-radio-button>
          <el-radio-button value="friendsOnly">👥 仅好友</el-radio-button>
          <el-radio-button value="private">🔒 私密</el-radio-button>
        </el-radio-group>
      </div>
      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">邮箱可见</span>
          <span class="label-desc">个人主页是否展示邮箱</span>
        </div>
        <el-switch
            :model-value="settings['privacy.showEmail'] === 'true'"
            @change="v => update('privacy.showEmail', v ? 'true' : 'false')"
        />
      </div>
      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">允许陌生人私信</span>
          <span class="label-desc">非好友能否给你发私信</span>
        </div>
        <el-switch
            :model-value="settings['privacy.allowStrangerChat'] === 'true'"
            @change="v => update('privacy.allowStrangerChat', v ? 'true' : 'false')"
        />
      </div>
    </section>

    <!-- 账号安全 -->
    <section class="setting-group">
      <h3 class="group-title">🔐 账号安全</h3>
      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">修改用户名</span>
          <span class="label-desc">修改后需要重新登录</span>
        </div>
        <div style="display: flex; gap: 8px; align-items: center;">
          <el-input v-model="usernameForm.newUsername" placeholder="输入新用户名" size="small" style="width: 180px" />
          <el-button type="primary" size="small" @click="submitChangeUsername" :loading="usernameLoading">确认</el-button>
        </div>
      </div>
      <div class="setting-item">
        <div class="setting-label">
          <span class="label-text">修改密码</span>
          <span class="label-desc">修改后需要重新登录</span>
        </div>
        <div style="display: flex; flex-direction: column; gap: 8px;">
          <el-input v-model="passwordForm.oldPassword" type="password" placeholder="输入旧密码" size="small" show-password />
          <el-input v-model="passwordForm.newPassword" type="password" placeholder="输入新密码" size="small" show-password />
          <el-button type="primary" size="small" @click="submitChangePassword" :loading="passwordLoading">确认修改</el-button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import api from '../api'

const settings = ref({})

const usernameForm = reactive({ newUsername: '' })
const usernameLoading = ref(false)
const passwordForm = reactive({ oldPassword: '', newPassword: '' })
const passwordLoading = ref(false)

const themeLabel = computed(() => {
  const map = { default: '默认', warm: '暖色', dark: '深色' }
  return map[settings.value['appearance.theme']] || '默认'
})
const fontSizeLabel = computed(() => {
  const map = { small: '小', medium: '中', large: '大' }
  return map[settings.value['appearance.fontSize']] || '中'
})
const visibilityLabel = computed(() => {
  const map = { public: '公开', friendsOnly: '仅好友', private: '私密' }
  return map[settings.value['privacy.profileVisibility']] || '公开'
})

const categories = ['appearance', 'editor', 'notification', 'privacy']

onMounted(async () => {
  for (const cat of categories) {
    try {
      const res = await api.getSettings({ category: cat })
      if (res.code === 200 && res.data) {
        Object.assign(settings.value, res.data)
      }
    } catch (e) { /* ignore */ }
  }
})

async function update(key, value) {
  settings.value[key] = value
  try {
    await api.updateSetting({ settingKey: key, settingValue: value })
  } catch (e) {
    ElMessage.error('设置更新失败')
  }
}

const submitChangeUsername = async () => {
  if (!usernameForm.newUsername.trim()) { ElMessage.warning('请输入新用户名'); return }
  usernameLoading.value = true
  try {
    const res = await api.changeUsername({ newUsername: usernameForm.newUsername })
    if (res.code === 200) {
      const userInfo = JSON.parse(localStorage.getItem('userInfo'))
      userInfo.username = usernameForm.newUsername
      localStorage.setItem('userInfo', JSON.stringify(userInfo))
      usernameForm.newUsername = ''
      ElMessage.success('用户名修改成功')
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '修改失败')
  } finally {
    usernameLoading.value = false
  }
}

const submitChangePassword = async () => {
  if (!passwordForm.oldPassword) { ElMessage.warning('请输入旧密码'); return }
  if (!passwordForm.newPassword) { ElMessage.warning('请输入新密码'); return }
  passwordLoading.value = true
  try {
    const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
    const res = await api.changePassword({
      username: userInfo.username || '',
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword,
      token: userInfo.token || ''
    })
    if (res.code === 200) {
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      ElMessage.success('密码修改成功')
    }
  } catch (err) {
    ElMessage.error(err.response?.data?.message || '修改失败')
  } finally {
    passwordLoading.value = false
  }
}
</script>

<style scoped>
.settings-page {
  max-width: 680px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.back-btn {
  font-size: 22px;
  cursor: pointer;
  color: #555;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.setting-group {
  background: #fff;
  border-radius: 14px;
  padding: 20px 24px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.group-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin: 0 0 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.setting-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 0;
  border-bottom: 1px solid #f8f8f8;
}

.setting-item:last-child {
  border-bottom: none;
}

.setting-label {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.label-text {
  font-size: 15px;
  color: #333;
  font-weight: 500;
}

.label-desc {
  font-size: 12px;
  color: #999;
}

:deep(.el-radio-group) {
  flex-shrink: 0;
}

:deep(.el-radio-button__inner) {
  font-size: 13px;
  padding: 6px 14px;
}
</style>