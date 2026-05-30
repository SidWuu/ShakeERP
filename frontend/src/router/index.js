import { createRouter, createWebHashHistory } from "vue-router";

const routes = [
  { path: "/login", name: "login", component: () => import("../views/LoginView.vue"), meta: { public: true } },
  { path: "/", name: "dashboard", component: () => import("../views/DashboardView.vue") },
  { path: "/customers", name: "customers", component: () => import("../views/CustomerView.vue") },
  { path: "/products", name: "products", component: () => import("../views/ProductView.vue") },
  { path: "/inventory", name: "inventory", component: () => import("../views/InventoryView.vue") },
  { path: "/ledger", name: "ledger", component: () => import("../views/LedgerView.vue") },
  { path: "/admin", name: "admin", component: () => import("../views/AdminView.vue") }
];

const router = createRouter({
  history: createWebHashHistory(),
  routes
});

// 路由守卫：未登录时跳转到登录页
router.beforeEach(async (to) => {
  if (to.meta.public) return true;

  const { useAppStore } = await import("../stores/app");
  const store = useAppStore();

  if (store.isLoggedIn) return true;

  // 尝试恢复 session
  const restored = await store.restoreSession();
  if (restored) return true;

  return { name: "login" };
});

export default router;
