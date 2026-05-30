<script setup>
import { reactive, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { useAppStore } from "../stores/app";
import { deleteInventory, saveInventory, updateInventory, stockIn, stockOut, scanProduct } from "../api";
import { validateRequired } from "../utils/validate";
import BarcodeScanner from "../components/BarcodeScanner.vue";
import MultiImageUpload from "../components/MultiImageUpload.vue";

const store = useAppStore();
const router = useRouter();

// 左侧面板模式：inventory / stockIn / stockOut
const panelMode = ref("inventory");
const selectedCode = ref("");

// 库存表单
const form = reactive({
  code: "",
  productCode: "",
  quantity: 0,
  remark: ""
});

// 入库表单
const stockInForm = reactive({
  productCode: "",
  quantity: 1,
  remark: "",
  customerCode: "",
  images: []
});

// 出库表单
const stockOutForm = reactive({
  productCode: "",
  quantity: 1,
  remark: "",
  customerCode: ""
});

const showScanner = ref(false);

const filters = reactive({
  startDate: "",
  endDate: "",
  code: "",
  name: ""
});

function syncQuantity() {
  const item = store.inventory.find((i) => i.productCode === form.productCode);
  form.quantity = item?.quantity || 0;
}

function resetForm() {
  selectedCode.value = "";
  form.code = "";
  form.productCode = store.products[0]?.code || "";
  syncQuantity();
  form.remark = "";
}

function editRow(row) {
  if (selectedCode.value === row.code) {
    resetForm();
    panelMode.value = "inventory";
    return;
  }
  selectedCode.value = row.code;
  panelMode.value = "inventory";
  form.code = row.code;
  form.productCode = row.productCode;
  form.quantity = row.quantity;
  form.remark = "";
}

function switchToStockIn() {
  panelMode.value = "stockIn";
  showScanner.value = false;
  stockInForm.productCode = store.products[0]?.code || "";
  stockInForm.quantity = 1;
  stockInForm.remark = "";
  stockInForm.customerCode = "";
  stockInForm.images = [];
}

function switchToStockOut() {
  panelMode.value = "stockOut";
  showScanner.value = false;
  stockOutForm.productCode = store.products[0]?.code || "";
  stockOutForm.quantity = 1;
  stockOutForm.remark = "";
  stockOutForm.customerCode = "";
}

function switchToInventory() {
  panelMode.value = "inventory";
  resetForm();
}

watch(
  () => store.products.length,
  () => {
    if (!form.productCode && store.products.length) {
      form.productCode = store.products[0].code;
      syncQuantity();
    }
  },
  { immediate: true }
);

// 扫码回调
async function onScanned(barcode) {
  showScanner.value = false;
  store.clearMessages();

  if (panelMode.value === "stockIn") {
    try {
      const product = await scanProduct(barcode);
      stockInForm.productCode = product.code;
      store.message = `已识别：${product.name}（当前库存 ${product.quantity}${product.unit}）`;
    } catch {
      store.message = "商品不存在，跳转新增商品";
      router.push({ path: "/products", query: { barcode } });
    }
  } else if (panelMode.value === "stockOut") {
    try {
      const product = await scanProduct(barcode);
      const inv = store.inventory.find((i) => i.productCode === product.code);
      if (!inv || inv.quantity <= 0) {
        store.error = `商品「${product.name}」当前没有库存，无法出库`;
        return;
      }
      stockOutForm.productCode = product.code;
      store.message = `已识别：${product.name}（当前库存 ${inv.quantity}${inv.unit}）`;
    } catch {
      store.error = "该条码对应的商品不存在，无法出库";
    }
  }
}

function onScanError(msg) {
  store.error = msg;
}

async function search() {
  await store.runAction(async () => {
    await store.refreshInventory(filters);
    store.message = `库存查询完成：${store.inventory.length} 条`;
  });
}

async function clearFilters() {
  filters.startDate = "";
  filters.endDate = "";
  filters.code = "";
  filters.name = "";
  await search();
}

async function submitInventory() {
  const err = validateRequired(form, [{ field: "productCode", label: "商品" }]);
  if (err) { store.error = err; store.message = ""; return; }
  await store.runAction(async () => {
    const operator = store.user?.displayName || "系统管理员";
    const saved = form.code
      ? await updateInventory(form.code, { ...form, operator })
      : await saveInventory({ ...form, operator });
    store.message = `${form.code ? "已更新库存" : "已保存库存"}：${saved.code} ${saved.productName}`;
    resetForm();
    await store.refreshInventory(filters);
    await store.refreshMovements({});
  });
}

async function submitStockIn() {
  const err = validateRequired(stockInForm, [
    { field: "productCode", label: "商品" },
    { field: "quantity", label: "数量" }
  ]);
  if (err) { store.error = err; store.message = ""; return; }
  await store.runAction(async () => {
    const updated = await stockIn({
      productCode: stockInForm.productCode,
      quantity: Number(stockInForm.quantity),
      operator: store.user?.displayName || "系统管理员",
      remark: stockInForm.remark,
      customerCode: stockInForm.customerCode || null,
      images: stockInForm.images.length > 0 ? stockInForm.images : null
    });
    store.message = `入库完成：${updated.productName} 当前库存 ${updated.quantity}${updated.unit}`;
    stockInForm.quantity = 1;
    stockInForm.remark = "";
    stockInForm.customerCode = "";
    stockInForm.images = [];
    await store.refreshInventory(filters);
    await store.refreshMovements({});
  });
}

async function submitStockOut() {
  const err = validateRequired(stockOutForm, [
    { field: "productCode", label: "商品" },
    { field: "quantity", label: "数量" }
  ]);
  if (err) { store.error = err; store.message = ""; return; }
  await store.runAction(async () => {
    const updated = await stockOut({
      productCode: stockOutForm.productCode,
      quantity: Number(stockOutForm.quantity),
      operator: store.user?.displayName || "系统管理员",
      remark: stockOutForm.remark,
      customerCode: stockOutForm.customerCode || null
    });
    store.message = `出库完成：${updated.productName} 当前库存 ${updated.quantity}${updated.unit}`;
    stockOutForm.quantity = 1;
    stockOutForm.remark = "";
    stockOutForm.customerCode = "";
    await store.refreshInventory(filters);
    await store.refreshMovements({});
  });
}

async function remove(row) {
  if (!window.confirm(`确认删除 ${row.productCode} 的库存记录？`)) return;
  await store.runAction(async () => {
    const operator = store.user?.displayName || "系统管理员";
    const updated = await deleteInventory(row.productCode, operator);
    store.message = `已删除库存：${updated.productName}`;
    await store.refreshInventory(filters);
    await store.refreshMovements({});
  });
}
</script>

<template>
  <section class="workspace">
    <!-- 左侧面板 -->
    <div class="panel form-panel">
      <!-- 操作切换按钮 -->
      <div class="panel-head">
        <div class="mode-buttons">
          <button :class="panelMode === 'stockIn' ? 'primary' : 'secondary'" @click="switchToStockIn">入库</button>
          <button :class="panelMode === 'stockOut' ? 'danger' : 'secondary'" @click="switchToStockOut">出库</button>
          <button :class="panelMode === 'inventory' ? 'primary' : 'secondary'" @click="switchToInventory">库存调整</button>
        </div>
      </div>

      <!-- 入库表单 -->
      <el-form v-if="panelMode === 'stockIn'" label-position="top">
        <div class="scan-toggle">
          <button class="secondary" @click="showScanner = !showScanner">
            {{ showScanner ? "关闭扫码" : "扫码入库" }}
          </button>
        </div>
        <div v-if="showScanner" class="scanner-section">
          <BarcodeScanner auto-start @scanned="onScanned" @error="onScanError" />
        </div>
        <el-form-item label="客户">
          <el-select v-model="stockInForm.customerCode" class="wide-select" clearable placeholder="选择客户（可选）">
            <el-option v-for="c in store.customers" :key="c.code" :label="`${c.code} ${c.name}`" :value="c.code" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <template #label><span class="required-label">商品</span></template>
          <el-select v-model="stockInForm.productCode" class="wide-select">
            <el-option v-for="item in store.productOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <template #label><span class="required-label">数量</span></template>
          <el-input-number v-model="stockInForm.quantity" :min="1" />
        </el-form-item>
        <el-form-item label="库存图片">
          <MultiImageUpload v-model="stockInForm.images" :max="9" />
        </el-form-item>
        <el-form-item label="备注">
          <el-select v-model="stockInForm.remark" class="wide-select" filterable allow-create clearable placeholder="选择或输入备注">
            <el-option v-for="item in store.remarkTemplates" :key="item.id" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <button class="primary wide" :disabled="store.submitting" @click.prevent="submitStockIn">确认入库</button>
      </el-form>

      <!-- 出库表单 -->
      <el-form v-else-if="panelMode === 'stockOut'" label-position="top">
        <div class="scan-toggle">
          <button class="secondary" @click="showScanner = !showScanner">
            {{ showScanner ? "关闭扫码" : "扫码出库" }}
          </button>
        </div>
        <div v-if="showScanner" class="scanner-section">
          <BarcodeScanner auto-start @scanned="onScanned" @error="onScanError" />
        </div>
        <el-form-item label="客户">
          <el-select v-model="stockOutForm.customerCode" class="wide-select" clearable placeholder="选择客户（可选）">
            <el-option v-for="c in store.customers" :key="c.code" :label="`${c.code} ${c.name}`" :value="c.code" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <template #label><span class="required-label">商品</span></template>
          <el-select v-model="stockOutForm.productCode" class="wide-select">
            <el-option v-for="item in store.productOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <template #label><span class="required-label">数量</span></template>
          <el-input-number v-model="stockOutForm.quantity" :min="1" />
        </el-form-item>
        <el-form-item label="备注">
          <el-select v-model="stockOutForm.remark" class="wide-select" filterable allow-create clearable placeholder="选择或输入备注">
            <el-option v-for="item in store.remarkTemplates" :key="item.id" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <button class="danger wide" :disabled="store.submitting" @click.prevent="submitStockOut">确认出库</button>
      </el-form>

      <!-- 库存调整表单 -->
      <el-form v-else label-position="top">
        <el-form-item label="库存编码">
          <el-input v-model="form.code" disabled placeholder="自动生成" />
        </el-form-item>
        <el-form-item>
          <template #label><span class="required-label">商品</span></template>
          <el-select v-model="form.productCode" class="wide-select" @change="syncQuantity">
            <el-option v-for="item in store.productOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="库存数量">
          <el-input-number v-model="form.quantity" :min="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-select v-model="form.remark" class="wide-select" filterable allow-create clearable placeholder="选择或输入备注">
            <el-option v-for="item in store.remarkTemplates" :key="item.id" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <button class="primary wide" :disabled="store.submitting" @click.prevent="submitInventory">
          {{ form.code ? "更新库存" : "保存库存" }}
        </button>
      </el-form>
    </div>

    <!-- 右侧库存汇总列表 -->
    <div class="panel">
      <div class="panel-head">
        <h2>库存汇总</h2>
        <span>{{ store.inventory.length }} 条</span>
      </div>
      <div class="query-bar">
        <el-date-picker class="query-control" v-model="filters.startDate" value-format="YYYY-MM-DD" placeholder="开始日期" />
        <el-date-picker class="query-control" v-model="filters.endDate" value-format="YYYY-MM-DD" placeholder="结束日期" />
        <el-input class="query-control" v-model="filters.code" placeholder="编码" />
        <el-input class="query-control" v-model="filters.name" placeholder="名称" />
        <button class="primary" :disabled="store.submitting" @click="search">查询</button>
        <button class="secondary" @click="clearFilters">清空</button>
      </div>
      <el-table :data="store.inventory" height="420" empty-text="暂无库存" highlight-current-row @row-click="editRow">
        <el-table-column width="50" align="center">
          <template #default="{ row }">
            <el-checkbox :model-value="selectedCode === row.code" @click.stop />
          </template>
        </el-table-column>
        <el-table-column prop="code" label="库存编码" width="110" />
        <el-table-column prop="productCode" label="商品编码" width="110" />
        <el-table-column prop="productName" label="商品" min-width="180" />
        <el-table-column label="数量" width="110">
          <template #default="{ row }">
            <strong :class="{ warning: row.quantity <= row.safetyStock }">
              {{ row.quantity }} {{ row.unit }}
            </strong>
          </template>
        </el-table-column>
        <el-table-column prop="safetyStock" label="安全库存" width="100" />
        <el-table-column label="操作" width="96" fixed="right">
          <template #default="{ row }">
            <button class="danger small" @click.stop="remove(row)">删除</button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>

<style scoped>
.mode-buttons {
  display: flex;
  gap: 8px;
}

.scan-toggle {
  margin-bottom: 12px;
}

.scanner-section {
  margin-bottom: 12px;
}
</style>
