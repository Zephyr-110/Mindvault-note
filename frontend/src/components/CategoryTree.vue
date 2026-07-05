<template>
  <div class="category-tree">
    <el-tree
        :data="treeData"
        node-key="id"
        :props="{ label: 'name', children: 'children' }"
        default-expand-all
        @node-click="handleNodeClick"
    >
      <template #default="{ node, data }">
        <div class="tree-node">
          <span class="node-icon">
            <span v-if="data.documentId">📄</span>
            <span v-else>📁</span>
          </span>
          <span class="node-label">{{ node.label }}</span>
          <div class="tree-actions">
            <el-button
                link
                size="small"
                @click.stop="$emit('create-sub', data.id)"
            >
              +
            </el-button>
            <el-button
                link
                size="small"
                @click.stop="$emit('edit', data)"
            >
              ✏️
            </el-button>
            <el-button
                link
                size="small"
                type="danger"
                @click.stop="data.documentId ? $emit('delete-document', data.documentId) : $emit('delete', data.id)"
            >
              🗑️
            </el-button>
          </div>
        </div>
      </template>
    </el-tree>
  </div>
</template>

<script setup>
const emit = defineEmits(['select', 'create-sub', 'edit', 'delete', 'delete-document'])

defineProps({
  treeData: {
    type: Array,
    required: true
  }
})

const handleNodeClick = (data) => {
  if (data.documents !== undefined && data.children !== undefined) {
    emit('select', data.id, data.name)
  } else {
    emit('document-select', data.documentId, data.name)
  }
}
</script>

<style scoped>
.category-tree {
  padding: 10px;
  overflow-y: auto;
  flex: 1;
  background: var(--bg-sidebar);
  color: var(--text-primary);
}

.tree-node {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  gap: 6px;
}

.node-icon {
  font-size: 14px;
  flex-shrink: 0;
}

.node-label {
  flex: 1;
}

.tree-actions {
  display: flex;
  gap: 5px;
  opacity: 0;
  transition: opacity 0.2s;
}

.tree-node:hover .tree-actions {
  opacity: 1;
}
</style>