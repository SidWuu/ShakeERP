<script setup>
import { computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAppStore } from "./stores/app";

const store = useAppStore();
const router = useRouter();
const route = useRoute();

const menuItems = [
  { key: "dashboard", path: "/", label: "工作台" },
  { key: "customers", path: "/customers", label: "客户" },
  { key: "products", path: "/products", label: "商品" },
  { key: "inventory", path: "/inventory", label: "库存管理" },
  { key: "ledger", path: "/ledger", label: "库存台账" },
  { key: "admin", path: "/admin", label: "系统管理" }
];

const isLoginPage = computed(() => route.name === "login");

const currentTitle = computed(() => {
  const item = menuItems.find((m) => m.key === route.name);
  return item?.label || "工作台";
});

const showBackendHint = computed(() =>
  Boolean(store.error) && ["Failed", "fetch", "无法连接"].some((kw) => store.error.includes(kw))
);

function navigate(item) {
  store.clearMessages();
  router.push(item.path);
}

function handleLogout() {
  store.logout();
  router.push("/login");
}

onMounted(async () => {
  if (store.isLoggedIn) {
    await store.loadAll();
  }
});
</script>

<template>
  <!-- 登录页不显示壳 -->
  <router-view v-if="isLoginPage" />

  <!-- 主应用壳 -->
  <div v-else class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <span class="brand-mark">工</span>
        <div>
          <p>ShakeERP</p>
          <strong>金东进销存</strong>
        </div>
      </div>

      <nav class="nav-list" aria-label="主菜单">
        <button
          v-for="item in menuItems"
          :key="item.key"
          :class="{ active: route.name === item.key }"
          @click="navigate(item)"
        >
          {{ item.label }}
        </button>
      </nav>

      <div class="sidebar-footer">
        <div class="user-info">
          <strong>{{ store.user?.displayName }}</strong>
          <small>{{ store.user?.role }}</small>
        </div>
        <button class="logout-btn" @click="handleLogout">退出</button>
      </div>
    </aside>

    <main class="content">
      <header class="topbar">
        <div>
          <p class="eyebrow">第一版功能开发</p>
          <h1>{{ currentTitle }}</h1>
        </div>
        <button class="primary" :disabled="store.loading" @click="store.loadAll">
          {{ store.loading ? "刷新中" : "刷新数据" }}
        </button>
      </header>

      <div class="notice-area">
        <section v-if="store.error" class="notice error">
          {{ store.error }}<template v-if="showBackendHint">。请确认后端运行在 http://localhost:8080。</template>
        </section>
        <section v-else-if="store.message" class="notice success">
          {{ store.message }}
        </section>
        <section v-else class="notice placeholder" aria-hidden="true">占位</section>
      </div>

      <router-view />
    </main>
  </div>
</template>

<style>
body {
  margin: 0;
  font-family: "PingFang SC", "Microsoft YaHei", Arial, sans-serif;
  background: #f4f7f6;
  color: #17211d;
}

* {
  box-sizing: border-box;
}

.app-shell {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 236px minmax(0, 1fr);
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 4px 4px 24px;
}

.brand-mark {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: #f0b85a;
  color: #14352b;
  font-weight: 800;
}

.brand p,
.brand strong {
  margin: 0;
}

.brand p {
  color: #a9c7bd;
  font-size: 12px;
}

.nav-list {
  display: grid;
  gap: 6px;
  flex: 1;
}

.sidebar {
  min-height: 100vh;
  padding: 22px 16px;
  background: #14352b;
  color: #f4fbf8;
  display: flex;
  flex-direction: column;
}

.sidebar-footer {
  margin-top: auto;
  padding-top: 16px;
  border-top: 1px solid #235344;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.user-info {
  display: flex;
  flex-direction: column;
}

.user-info strong {
  font-size: 13px;
  color: #f4fbf8;
}

.user-info small {
  font-size: 11px;
  color: #a9c7bd;
}

.logout-btn {
  min-height: 32px;
  padding: 0 12px;
  background: #235344;
  color: #d9ebe5;
  font-size: 12px;
}

button {
  border: 0;
  border-radius: 6px;
  cursor: pointer;
  font: inherit;
}

button:disabled {
  cursor: wait;
  opacity: 0.68;
}

.nav-list button {
  min-height: 42px;
  padding: 0 12px;
  text-align: left;
  background: transparent;
  color: #d9ebe5;
}

.nav-list button.active,
.nav-list button:hover {
  background: #235344;
  color: #ffffff;
}

.content {
  min-width: 0;
  padding: 28px;
}

.topbar,
.panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.eyebrow {
  margin: 0 0 4px;
  color: #547066;
  font-size: 13px;
}

h1,
h2 {
  margin: 0;
}

h1 {
  font-size: 28px;
}

h2 {
  font-size: 18px;
}

.view {
  margin-top: 22px;
}

.primary,
.danger,
.secondary,
.action-grid button {
  min-height: 38px;
  padding: 0 16px;
}

.primary,
.danger,
.action-grid button {
  color: #ffffff;
}

.primary,
.action-grid button {
  background: #1c6b55;
}

.danger {
  background: #a84432;
}

.secondary {
  background: #e7efeb;
  color: #244239;
}

.small {
  min-height: 30px;
  padding: 0 10px;
  font-size: 13px;
}

.create-button {
  min-height: 34px;
  min-width: 76px;
  padding: 0 18px;
  font-weight: 700;
}

.wide {
  width: 100%;
}

.required-label::before {
  content: "*";
  margin-right: 4px;
  color: #c43b2f;
  font-weight: 700;
}

.notice-area {
  margin-top: 18px;
  height: 44px;
}

.notice {
  box-sizing: border-box;
  min-height: 44px;
  padding: 12px 14px;
  border-radius: 6px;
}

.notice.placeholder {
  visibility: hidden;
}

.error {
  background: #fff1f0;
  color: #a23a33;
  border: 1px solid #ffd1cd;
}

.success {
  background: #edf8f1;
  color: #23623c;
  border: 1px solid #cbeed7;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(150px, 1fr));
  gap: 14px;
}

.metric,
.panel {
  background: #ffffff;
  border: 1px solid #dce6e2;
  border-radius: 8px;
}

.metric {
  padding: 16px;
}

.metric span,
.metric small,
.empty-state p,
.scanner-result p {
  display: block;
  color: #657a72;
}

.metric strong {
  display: block;
  margin: 8px 0 4px;
  font-size: 26px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(130px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.workspace,
.mobile-layout {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 16px;
  margin-top: 22px;
}

.panel {
  min-width: 0;
  overflow: hidden;
}

.panel-head {
  padding: 16px;
  border-bottom: 1px solid #e6eeeb;
}

.panel-head span {
  color: #657a72;
}

.query-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px 16px;
  border-bottom: 1px solid #e6eeeb;
}

.query-bar .query-control {
  flex: 0 0 150px;
  height: 38px;
  width: 150px;
}

.query-bar button {
  flex: 0 0 auto;
  min-width: 64px;
}

.query-bar .query-control.el-date-editor,
.query-bar .query-control.el-input,
.query-bar .query-control .el-input,
.query-bar .query-control .el-input__wrapper {
  box-sizing: border-box;
  height: 38px;
  width: 150px;
}

.query-bar .el-input__wrapper,
.query-bar .el-select__wrapper {
  align-items: center;
  box-sizing: border-box;
  height: 38px;
  line-height: 38px;
}

.el-table__row {
  cursor: pointer;
}

.ledger-panel {
  margin-top: 22px;
}

.form-panel {
  padding: 0 16px 16px;
}

.form-panel .panel-head {
  margin: 0 -16px 16px;
}

.form-row {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.narrow {
  max-width: 520px;
  margin-top: 22px;
}

.wide-select {
  width: 100%;
}

.warning {
  color: #b25b00;
}

.empty-state {
  margin-top: 22px;
  padding: 24px;
}

.scanner-result {
  padding: 22px;
}

.scanner-result strong {
  display: block;
  margin: 18px 0;
  font-size: 34px;
}

.tag {
  display: inline-block;
  margin-bottom: 10px;
  padding: 4px 8px;
  border-radius: 6px;
  background: #e7f2ee;
  color: #1c6b55;
  font-size: 12px;
}

.button-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

@media (max-width: 980px) {
  .app-shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    min-height: auto;
  }

  .nav-list {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .nav-list button {
    text-align: center;
  }

  .status-grid,
  .workspace,
  .mobile-layout {
    grid-template-columns: 1fr;
  }

  .query-bar .query-control {
    flex-basis: calc(50% - 5px);
    width: calc(50% - 5px);
  }
}

@media (max-width: 560px) {
  .content {
    padding: 18px;
  }

  .topbar {
    align-items: stretch;
    flex-direction: column;
  }

  .status-grid,
  .action-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .query-bar .query-control,
  .query-bar button {
    flex-basis: 100%;
    width: 100%;
  }
}
</style>
