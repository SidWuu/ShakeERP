<script setup>
import { onMounted, reactive, ref } from "vue";
import { useAppStore } from "../stores/app";
import { createBackup, fetchBackups, getExportUrl, restoreBackup, deleteBackup, fetchDictItems, createDictItem, deleteDictItem, fetchUsers } from "../api";

const store = useAppStore();
const activeTab = ref("dict");

// --- 系统备份 ---
const backups = ref([]);

async function loadBackups() {
  await store.runAction(async () => {
    backups.value = await fetchBackups();
  });
}

async function doBackup() {
  await store.runAction(async () => {
    const result = await createBackup();
    store.message = `备份成功：${result.filename}`;
    await loadBackups();
  });
}

async function doRestore(filename) {
  if (!window.confirm(`确认从备份 ${filename} 恢复数据库？\n\n恢复前会自动备份当前数据库。\n恢复后需要重启后端服务才能生效。`)) return;
  await store.runAction(async () => {
    const result = await restoreBackup(filename);
    store.message = `${result.message}（恢复前备份：${result.previousBackup}）`;
    await loadBackups();
  });
}

async function doDeleteBackup(filename) {
  if (!window.confirm(`确认删除备份 ${filename}？`)) return;
  await store.runAction(async () => {
    await deleteBackup(filename);
    store.message = `已删除备份：${filename}`;
    await loadBackups();
  });
}

function doExport() {
  window.open(getExportUrl(), "_blank");
}

function formatSize(bytes) {
  if (!bytes) return "-";
  if (bytes < 1024) return bytes + " B";
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
  return (bytes / 1024 / 1024).toFixed(1) + " MB";
}

// --- 数据字典 ---
const dictItems = ref([]);
const dictCategories = ref([
  { id: 1, name: "商品分类" },
  { id: 2, name: "商品单位" },
  { id: 3, name: "备注模板" }
]);
const dictForm = reactive({ category: "", label: "", value: "" });
const dictFilter = reactive({ category: "" });

async function loadDictItems() {
  await store.runAction(async () => {
    dictItems.value = await fetchDictItems(dictFilter);
  });
}

async function submitDictItem() {
  if (!dictForm.category || !dictForm.label.trim()) {
    store.error = "请填写分类和名称";
    return;
  }
  await store.runAction(async () => {
    await createDictItem({
      category: dictForm.category,
      label: dictForm.label.trim(),
      value: dictForm.value.trim() || dictForm.label.trim()
    });
    store.message = `已添加：${dictForm.category} - ${dictForm.label}`;
    dictForm.label = "";
    dictForm.value = "";
    await loadDictItems();
  });
}

async function removeDictItem(id) {
  if (!window.confirm("确认删除该字典项？")) return;
  await store.runAction(async () => {
    await deleteDictItem(id);
    store.message = "已删除";
    await loadDictItems();
  });
}

async function searchDict() {
  await loadDictItems();
}

async function clearDictFilter() {
  dictFilter.category = "";
  await loadDictItems();
}

// --- 用户管理 ---
const users = ref([]);

async function loadUsers() {
  await store.runAction(async () => {
    users.value = await fetchUsers();
  });
}

// --- 初始化 ---
onMounted(async () => {
  await loadBackups();
  await loadDictItems();
  await loadUsers();
});
</script>

<template>
  <div class="view">
    <div class="admin-tabs">
      <button :class="activeTab === 'dict' ? 'primary' : 'secondary'" @click="activeTab = 'dict'">数据字典</button>
      <button :class="activeTab === 'backup' ? 'primary' : 'secondary'" @click="activeTab = 'backup'">系统备份</button>
      <button :class="activeTab === 'users' ? 'primary' : 'secondary'" @click="activeTab = 'users'">用户管理</button>
    </div>

    <!-- 系统备份 -->
    <div v-if="activeTab === 'backup'">
      <div class="admin-actions">
        <button class="primary" :disabled="store.submitting" @click="doBackup">创建备份</button>
        <button class="secondary" @click="doExport">导出数据库</button>
      </div>
      <p class="admin-hint">系统每天凌晨 2:00 自动备份，保留最近 30 个自动备份。</p>
      <div class="panel" style="margin-top: 16px;">
        <div class="panel-head">
          <h2>备份记录</h2>
          <span>{{ backups.length }} 个</span>
        </div>
        <el-table :data="backups" height="360" empty-text="暂无备份">
          <el-table-column prop="filename" label="文件名" min-width="280" />
          <el-table-column label="大小" width="120">
            <template #default="{ row }">{{ formatSize(row.size) }}</template>
          </el-table-column>
          <el-table-column prop="createdAt" label="创建时间" min-width="200" />
          <el-table-column label="操作" width="180" fixed="right">
            <template #default="{ row }">
              <button class="primary small" @click.stop="doRestore(row.filename)">恢复</button>
              <button class="danger small" style="margin-left: 6px;" @click.stop="doDeleteBackup(row.filename)">删除</button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 数据字典 -->
    <div v-else-if="activeTab === 'dict'" class="workspace">
      <div class="panel form-panel">
        <div class="panel-head"><h2>新增字典项</h2></div>
        <el-form label-position="top">
          <el-form-item label="分类">
            <el-select v-model="dictForm.category" class="wide-select" placeholder="选择分类">
              <el-option v-for="cat in dictCategories" :key="cat.id" :label="cat.name" :value="cat.name" />
            </el-select>
          </el-form-item>
          <el-form-item label="名称">
            <el-input v-model="dictForm.label" placeholder="例如：五金件" />
          </el-form-item>
          <el-form-item label="值（可选，默认同名称）">
            <el-input v-model="dictForm.value" placeholder="留空则与名称相同" />
          </el-form-item>
          <button class="primary wide" :disabled="store.submitting" @click.prevent="submitDictItem">添加字典项</button>
        </el-form>
      </div>

      <div class="panel">
        <div class="panel-head">
          <h2>字典列表</h2>
          <span>{{ dictItems.length }} 条</span>
        </div>
        <div class="query-bar">
          <el-select class="query-control" v-model="dictFilter.category" clearable placeholder="按分类筛选">
            <el-option v-for="cat in dictCategories" :key="cat.id" :label="cat.name" :value="cat.name" />
          </el-select>
          <button class="primary" :disabled="store.submitting" @click="searchDict">查询</button>
          <button class="secondary" @click="clearDictFilter">清空</button>
        </div>
        <el-table :data="dictItems" height="420" empty-text="暂无字典数据">
          <el-table-column prop="category" label="分类" width="140" />
          <el-table-column prop="label" label="名称" min-width="180" />
          <el-table-column prop="value" label="值" min-width="180" />
          <el-table-column label="操作" width="96" fixed="right">
            <template #default="{ row }">
              <button class="danger small" @click.stop="removeDictItem(row.id)">删除</button>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 用户管理 -->
    <div v-else-if="activeTab === 'users'">
      <p class="admin-hint">用户管理功能预留，当前展示系统默认用户。</p>
      <div class="panel" style="margin-top: 16px;">
        <div class="panel-head">
          <h2>用户列表</h2>
          <span>{{ users.length }} 人</span>
        </div>
        <el-table :data="users" height="360" empty-text="暂无用户">
          <el-table-column prop="username" label="用户名" width="160" />
          <el-table-column prop="displayName" label="显示名称" min-width="180" />
          <el-table-column prop="role" label="角色" width="120" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.admin-actions {
  display: flex;
  gap: 12px;
}

.admin-hint {
  margin: 12px 0 0;
  color: #657a72;
  font-size: 13px;
}
</style>
