<template>
  <div class="document-list">
    <el-empty v-if="documents.length === 0" description="暂无文档" />

    <el-card
      v-for="doc in documents"
      :key="doc.id"
      class="doc-card"
      shadow="hover"
    >
      <div class="doc-header">
        <h4 @click="$emit('view', doc.id)">{{ doc.title }}</h4>
        <div class="doc-actions">
          <el-button size="small" @click="$emit('edit', doc)">编辑</el-button>
          <el-button size="small" type="danger" @click="$emit('delete', doc.id)">删除</el-button>
        </div>
      </div>
      <p class="doc-preview">{{ truncate(doc.content, 100) }}</p>
      <div class="doc-footer">
        <span>更新时间: {{ formatDate(doc.updateTime) }}</span>
      </div>
    </el-card>
  </div>
</template>

<script setup>
defineProps({
  documents: {
    type: Array,
    default: () => []
  }
})

defineEmits(['view', 'edit', 'delete'])

const truncate = (text, length) => {
  if (!text) return ''
  return text.length > length ? text.substring(0, length) + '...' : text
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}
</script>

<style scoped>
.document-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.doc-card {
  cursor: pointer;
  transition: transform 0.2s;
}

.doc-card:hover {
  transform: translateY(-2px);
}

.doc-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.doc-header h4 {
  margin: 0;
  color: #667eea;
  cursor: pointer;
}

.doc-actions {
  display: flex;
  gap: 10px;
}

.doc-preview {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  margin: 10px 0;
}

.doc-footer {
  color: #999;
  font-size: 12px;
  text-align: right;
}
</style>
