<script setup>
import { onMounted, reactive, ref } from "vue";
import { useRoute } from "vue-router";
import { useAppStore } from "../stores/app";
import { createProduct, deleteProduct, updateProduct, getImageUrl } from "../api";
import { validateRequired } from "../utils/validate";
import MultiImageUpload from "../components/MultiImageUpload.vue";

const store = useAppStore();
const route = useRoute();

const selectedCode = ref("");

const form = reactive({
  code: "",
  barcode: "",
  name: "",
  category: "",
  unit: "件",
  safetyStock: 0,
  initialQuantity: 0,
  remark: "",
  images: []
});

// 接收从扫码页带过来的 barcode
onMounted(() => {
  const fromScan = route.query.barcode;
  if (fromScan) {
    form.barcode = fromScan;
  }
});

const filters = reactive({
  startDate: "",
  endDate: "",
  code: "",
  name: ""
});

function resetForm() {
  selectedCode.value = "";
  form.code = "";
  form.barcode = "";
  form.name = "";
  form.category = "";
  form.unit = "件";
  form.safetyStock = 0;
  form.initialQuantity = 0;
  form.remark = "";
  form.images = [];
}

function editRow(row) {
  if (selectedCode.value === row.code) {
    resetForm();
    return;
  }
  selectedCode.value = row.code;
  form.code = row.code;
  form.barcode = row.barcode;
  form.name = row.name;
  form.category = row.category;
  form.unit = row.unit;
  form.safetyStock = row.safetyStock;
  form.initialQuantity = 0;
  form.remark = row.remark || "";
  form.images = row.imageUrl ? [row.imageUrl] : (row.images || []);
}


async function search() {
  await store.runAction(async () => {
    await store.refreshProducts(filters);
    store.message = `商品查询完成：${store.products.length} 条`;
  });
}

async function clearFilters() {
  filters.startDate = "";
  filters.endDate = "";
  filters.code = "";
  filters.name = "";
  await search();
}

async function submit() {
  const err = validateRequired(form, [
    { field: "name", label: "商品名称" }
  ]);
  if (err) {
    store.error = err;
    store.message = "";
    return;
  }
  // 前端预检查重复条码
  if (form.barcode && !form.code) {
    const existing = store.products.find(p => p.barcode === form.barcode);
    if (existing) {
      store.error = `条码 ${form.barcode} 已被商品 ${existing.code} ${existing.name} 使用`;
      store.message = "";
      return;
    }
  }
  if (form.barcode && form.code) {
    const existing = store.products.find(p => p.barcode === form.barcode && p.code !== form.code);
    if (existing) {
      store.error = `条码 ${form.barcode} 已被商品 ${existing.code} ${existing.name} 使用`;
      store.message = "";
      return;
    }
  }
  await store.runAction(async () => {
    const saved = form.code
      ? await updateProduct(form.code, form)
      : await createProduct(form);
    store.message = `${form.code ? "已更新商品" : "已新增商品"}：${saved.code} ${saved.name}`;
    resetForm();
    await store.refreshProducts(filters);
    await store.refreshInventory({});
  });
}

async function remove(row) {
  if (!window.confirm(`确认删除商品 ${row.code} ${row.name}？`)) return;
  await store.runAction(async () => {
    const deleted = await deleteProduct(row.code);
    store.message = `已删除商品：${deleted.code} ${deleted.name}`;
    await store.refreshProducts(filters);
  });
}
</script>

<template>
  <section class="workspace">
    <div class="panel form-panel">
      <div class="panel-head">
        <h2>{{ form.code ? "编辑商品" : "新增商品" }}</h2>
        <button v-if="form.code" class="primary create-button" @click="resetForm">新增</button>
      </div>
      <el-form label-position="top">
        <el-form-item label="商品编码">
          <el-input v-model="form.code" disabled placeholder="自动生成" />
        </el-form-item>
        <el-form-item label="条码">
          <el-input v-model="form.barcode" placeholder="例如 6900000020015" />
        </el-form-item>
        <el-form-item>
          <template #label><span class="required-label">名称</span></template>
          <el-input v-model="form.name" placeholder="商品名称" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="分类">
            <el-select v-model="form.category" class="wide-select" filterable allow-create placeholder="选择分类">
              <el-option v-for="item in store.categoryOptions" :key="item.id" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="单位">
            <el-select v-model="form.unit" class="wide-select" filterable allow-create placeholder="选择单位">
              <el-option v-for="item in store.unitOptions" :key="item.id" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="安全库存">
          <el-input-number v-model="form.safetyStock" :min="0" />
        </el-form-item>
        <el-form-item label="增加库存">
          <el-input-number v-model="form.initialQuantity" :min="0" />
        </el-form-item>
        <el-form-item label="备注">
          <el-select v-model="form.remark" class="wide-select" filterable allow-create clearable placeholder="选择或输入备注">
            <el-option v-for="item in store.remarkTemplates" :key="item.id" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品图片">
          <MultiImageUpload v-model="form.images" :max="9" />
        </el-form-item>
        <button class="primary wide" :disabled="store.submitting" @click.prevent="submit">
          {{ form.code ? "更新商品" : "保存商品" }}
        </button>
      </el-form>
    </div>

    <div class="panel">
      <div class="panel-head">
        <h2>商品资料</h2>
        <span>{{ store.products.length }} 条</span>
      </div>
      <div class="query-bar">
        <el-date-picker class="query-control" v-model="filters.startDate" value-format="YYYY-MM-DD" placeholder="开始日期" />
        <el-date-picker class="query-control" v-model="filters.endDate" value-format="YYYY-MM-DD" placeholder="结束日期" />
        <el-input class="query-control" v-model="filters.code" placeholder="编码" />
        <el-input class="query-control" v-model="filters.name" placeholder="名称" />
        <button class="primary" :disabled="store.submitting" @click="search">查询</button>
        <button class="secondary" @click="clearFilters">清空</button>
      </div>
      <el-table :data="store.products" height="420" empty-text="暂无商品" highlight-current-row @row-click="editRow">
        <el-table-column width="50" align="center">
          <template #default="{ row }">
            <el-checkbox :model-value="selectedCode === row.code" @click.stop />
          </template>
        </el-table-column>
        <el-table-column label="图片" width="70">
          <template #default="{ row }">
            <img
              v-if="row.imageUrl || (row.images && row.images.length)"
              :src="getImageUrl(row.imageUrl || row.images[0])"
              alt="商品图片"
              class="table-thumbnail"
            />
            <span v-else class="no-image">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="编码" width="110" />
        <el-table-column prop="name" label="名称" min-width="170" />
        <el-table-column prop="barcode" label="条码" min-width="150" />
        <el-table-column prop="category" label="分类" width="100" />
        <el-table-column prop="unit" label="单位" width="80" />
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
.table-thumbnail {
  width: 40px;
  height: 40px;
  border-radius: 4px;
  object-fit: cover;
  border: 1px solid #e4e7ed;
}

.no-image {
  color: #c0c4cc;
}
</style>
