C:\Users\30149\IdeaProjects\MindVault-note-project\frontend\src\views\BlockList.vue
<template>
  <div class="blocklist-page">
    <div class="page-header">
      <span class="back-btn" @click="$router.back()">←</span>
      <h2 class="page-title">黑名单</h2>
    </div>

    <div v-if="list.length === 0 && !loading" class="empty-state">
      <span class="empty-icon">🚫</span>
      <p>黑名单为空</p>
    </div>

    <div v-for="user in list" :key="user.userId" class="block-item">
      <div class="block-user" @click="goToUser(user.userId)">
        <el-avatar :size="48" :src="user.avatar">{{ user.nickname?.charAt(0) || 'U' }}</el-avatar>
        <div class="block-user-info">
          <span class="block-user-name">{{ user.nickname || '未知用户' }}</span>
        </div>
      </div>
      <el-button type="danger" plain size="small" @click="handleUnblock(user)">取消拉黑</el-button>
    </div>

    <div v-if="loading" class="loading-tip">加载中...</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api'

const router = useRouter()
const list = ref([])
const loading = ref(false)

onMounted(() => {
  loadList()
})

async function loadList() {
  loading.value = true
  try {
    const res = await api.listBlockedUsers({ page: 1, size: 50 })
    if (res.code === 200 && res.data) {
      list.value = res.data.list || []
    }
  } catch (e) {
    // ignore
  } finally {
    loading.value = false
  }
}

async function handleUnblock(user) {
  try {
    await ElMessageBox.confirm(`确定取消拉黑「${user.nickname}」吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await api.unblockUser({ userId: user.userId })
    ElMessage.success('已取消拉黑')
    list.value = list.value.filter(u => u.userId !== user.userId)
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error('操作失败')
    }
  }
}

function goToUser(userId) {
  router.push(`/user/${userId}`)
}
</script>

<style scoped>
.blocklist-page {
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
  font-size: 20px;
  cursor: pointer;
  color: #555;
}

.page-title {
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}

.block-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: 12px;
  padding: 14px 20px;
  margin-bottom: 10px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.block-user {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.block-user-info {
  display: flex;
  flex-direction: column;
}

.block-user-name {
  font-size: 15px;
  font-weight: 500;
  color: #333;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: #aaa;
}

.empty-icon {
  font-size: 48px;
  display: block;
  margin-bottom: 12px;
}

.loading-tip {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 14px;
}
</style>