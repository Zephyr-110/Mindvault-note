<template>
  <div class="tag-manager">
    <div v-if="tags.length === 0" class="empty-tags">
      <p>暂无标签</p>
    </div>

    <div v-else class="tag-list">
      <el-tag
        v-for="tag in tags"
        :key="tag.id"
        closable
        @close="$emit('delete', tag.id)"
        class="tag-item"
      >
        {{ tag.name }}
      </el-tag>
    </div>
  </div>
</template>

<script setup>
defineProps({
  tags: {
    type: Array,
    default: () => []
  }
})

defineEmits(['delete'])
</script>

<style scoped>
.tag-manager {
  padding: 15px;
  overflow-y: auto;
  flex: 1;
}

.empty-tags {
  text-align: center;
  color: #999;
  padding: 20px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.tag-item {
  cursor: pointer;
}
</style>
