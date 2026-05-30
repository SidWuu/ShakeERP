<script setup>
import { reactive } from "vue";
import { useAppStore } from "../stores/app";
import { formatDateTime } from "../utils/format";

const store = useAppStore();

const filters = reactive({
  startDate: "",
  endDate: "",
  code: "",
  name: ""
});

async function search() {
  await store.runAction(async () => {
    await store.refreshMovements(filters);
    store.message = `台账查询完成：${store.movements.length} 条`;
  });
}

async function clearFilters() {
  filters.startDate = "";
  filters.endDate = "";
  filters.code = "";
  filters.name = "";
  await search();
}
</script>

<template>
  <section class="panel ledger-panel">
    <div class="panel-head">
      <h2>库存台账</h2>
      <span>{{ store.movements.length }} 条</span>
    </div>
    <div class="query-bar">
      <el-date-picker class="query-control" v-model="filters.startDate" value-format="YYYY-MM-DD" placeholder="开始日期" />
      <el-date-picker class="query-control" v-model="filters.endDate" value-format="YYYY-MM-DD" placeholder="结束日期" />
      <el-input class="query-control" v-model="filters.code" placeholder="编码" />
      <el-input class="query-control" v-model="filters.name" placeholder="名称" />
      <button class="primary" :disabled="store.submitting" @click="search">查询</button>
      <button class="secondary" @click="clearFilters">清空</button>
    </div>
    <el-table :data="store.movements" height="520" empty-text="暂无台账">
      <el-table-column label="时间" min-width="190">
        <template #default="{ row }">
          {{ formatDateTime(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="type" label="类型" width="120" />
      <el-table-column prop="productCode" label="编码" width="110" />
      <el-table-column prop="productName" label="名称" min-width="170" />
      <el-table-column prop="quantity" label="变动数量" width="100" />
      <el-table-column prop="beforeQuantity" label="变动前" width="90" />
      <el-table-column prop="afterQuantity" label="变动后" width="90" />
      <el-table-column prop="operator" label="经办人" width="110" />
      <el-table-column prop="customerCode" label="客户" width="110" />
      <el-table-column prop="remark" label="备注" min-width="140" />
    </el-table>
  </section>
</template>
