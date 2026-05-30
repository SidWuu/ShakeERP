import { computed, ref } from "vue";
import { defineStore } from "pinia";
import {
  fetchCustomers,
  fetchHealth,
  fetchInventorySummary,
  fetchProducts,
  fetchStockMovements,
  fetchDictItems,
  loginUser,
  fetchCurrentUser
} from "../api";

export const useAppStore = defineStore("app", () => {
  const loading = ref(false);
  const submitting = ref(false);
  const error = ref("");
  const message = ref("");

  const health = ref(null);
  const user = ref(null);
  const token = ref(localStorage.getItem("shake_token") || "");
  const customers = ref([]);
  const products = ref([]);
  const inventory = ref([]);
  const movements = ref([]);
  const dictItems = ref([]);

  const isLoggedIn = computed(() => !!user.value);

  const lowStockCount = computed(() =>
    inventory.value.filter((item) => item.quantity <= item.safetyStock).length
  );

  const productOptions = computed(() =>
    products.value.map((product) => ({
      label: `${product.code} ${product.name}`,
      value: product.code
    }))
  );

  /** 按分类获取字典项 */
  function getDictByCategory(category) {
    return dictItems.value.filter((item) => item.category === category);
  }

  const categoryOptions = computed(() => getDictByCategory("商品分类"));
  const unitOptions = computed(() => getDictByCategory("商品单位"));
  const remarkTemplates = computed(() => getDictByCategory("备注模板"));

  /** 登录 */
  async function login(username, password) {
    submitting.value = true;
    error.value = "";
    try {
      const info = await loginUser(username, password);
      user.value = info;
      token.value = info.token;
      localStorage.setItem("shake_token", info.token);
    } catch (caught) {
      error.value = caught instanceof Error ? caught.message : "登录失败";
      throw caught;
    } finally {
      submitting.value = false;
    }
  }

  /** 退出登录 */
  function logout() {
    user.value = null;
    token.value = "";
    localStorage.removeItem("shake_token");
  }

  /** 尝试用已保存的 token 恢复登录状态 */
  async function restoreSession() {
    if (!token.value) return false;
    try {
      const info = await fetchCurrentUser();
      if (info) {
        user.value = info;
        return true;
      }
    } catch {
      // token 无效，静默清除
    }
    logout();
    return false;
  }

  /** 加载所有业务数据 */
  async function loadAll() {
    loading.value = true;
    error.value = "";
    try {
      const [healthData, customerData, productData, inventoryData, movementData, dictData] =
        await Promise.all([
          fetchHealth(),
          fetchCustomers({}),
          fetchProducts({}),
          fetchInventorySummary({}),
          fetchStockMovements({}),
          fetchDictItems({})
        ]);
      health.value = healthData;
      customers.value = customerData;
      products.value = productData;
      inventory.value = inventoryData;
      movements.value = movementData;
      dictItems.value = dictData;
    } catch (caught) {
      error.value = caught instanceof Error ? caught.message : "无法连接后端服务";
    } finally {
      loading.value = false;
    }
  }

  async function refreshCustomers(filters = {}) {
    customers.value = await fetchCustomers(filters);
  }

  async function refreshProducts(filters = {}) {
    products.value = await fetchProducts(filters);
  }

  async function refreshInventory(filters = {}) {
    inventory.value = await fetchInventorySummary(filters);
  }

  async function refreshMovements(filters = {}) {
    movements.value = await fetchStockMovements(filters);
  }

  function clearMessages() {
    error.value = "";
    message.value = "";
  }

  async function runAction(action) {
    submitting.value = true;
    error.value = "";
    message.value = "";
    try {
      await action();
    } catch (caught) {
      error.value = caught instanceof Error ? caught.message : "操作失败";
    } finally {
      submitting.value = false;
    }
  }

  return {
    loading,
    submitting,
    error,
    message,
    health,
    user,
    token,
    isLoggedIn,
    customers,
    products,
    inventory,
    movements,
    dictItems,
    lowStockCount,
    productOptions,
    categoryOptions,
    unitOptions,
    remarkTemplates,
    getDictByCategory,
    login,
    logout,
    restoreSession,
    loadAll,
    refreshCustomers,
    refreshProducts,
    refreshInventory,
    refreshMovements,
    clearMessages,
    runAction
  };
});
