<script setup>
import { useAppStore } from "../stores/app";
import { useRouter } from "vue-router";

const store = useAppStore();
const router = useRouter();
</script>

<template>
  <div class="view">
    <div class="status-grid">
      <article class="metric">
        <span>后端状态</span>
        <strong>{{ store.health?.status || "检查中" }}</strong>
        <small>{{ store.health?.service || "shake-erp" }}</small>
      </article>
      <article class="metric">
        <span>当前用户</span>
        <strong>{{ store.user?.displayName || "未登录" }}</strong>
        <small>{{ store.user?.role || "演示账号" }}</small>
      </article>
      <article class="metric">
        <span>商品数量</span>
        <strong>{{ store.products.length }}</strong>
        <small>基础资料</small>
      </article>
      <article class="metric">
        <span>低库存</span>
        <strong>{{ store.lowStockCount }}</strong>
        <small>按安全库存判断</small>
      </article>
    </div>

    <div class="action-grid">
      <button @click="router.push('/products')">新增商品</button>
      <button @click="router.push('/inventory')">库存管理</button>
      <button @click="router.push('/ledger')">库存台账</button>
      <button @click="router.push('/customers')">客户管理</button>
    </div>
  </div>
</template>
