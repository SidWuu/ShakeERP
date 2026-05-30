<script setup>
import { reactive, ref } from "vue";
import { useAppStore } from "../stores/app";
import { createCustomer, deleteCustomer, updateCustomer } from "../api";
import { validateRequired } from "../utils/validate";

const store = useAppStore();

const selectedCode = ref("");

const form = reactive({
  code: "",
  name: "",
  contactPerson: "",
  phone: "",
  qq: "",
  wechat: "",
  address: "",
  remark: ""
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
  form.name = "";
  form.contactPerson = "";
  form.phone = "";
  form.qq = "";
  form.wechat = "";
  form.address = "";
  form.remark = "";
}

function editRow(row) {
  // 再次点击同一行取消选中
  if (selectedCode.value === row.code) {
    resetForm();
    return;
  }
  selectedCode.value = row.code;
  form.code = row.code;
  form.name = row.name;
  form.contactPerson = row.contactPerson;
  form.phone = row.phone;
  form.qq = row.qq || "";
  form.wechat = row.wechat || "";
  form.address = row.address || "";
  form.remark = row.remark || "";
}

async function search() {
  await store.runAction(async () => {
    await store.refreshCustomers(filters);
    store.message = `客户查询完成：${store.customers.length} 条`;
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
    { field: "name", label: "客户名称" },
    { field: "phone", label: "联系电话" }
  ]);
  if (err) {
    store.error = err;
    store.message = "";
    return;
  }
  await store.runAction(async () => {
    const saved = form.code
      ? await updateCustomer(form.code, form)
      : await createCustomer(form);
    store.message = `${form.code ? "已更新客户" : "已新增客户"}：${saved.code} ${saved.name}`;
    resetForm();
    await store.refreshCustomers(filters);
  });
}

async function remove(row) {
  if (!window.confirm(`确认删除客户 ${row.code} ${row.name}？`)) return;
  await store.runAction(async () => {
    const deleted = await deleteCustomer(row.code);
    store.message = `已删除客户：${deleted.code} ${deleted.name}`;
    await store.refreshCustomers(filters);
  });
}
</script>

<template>
  <section class="workspace">
    <div class="panel form-panel">
      <div class="panel-head">
        <h2>{{ form.code ? "编辑客户" : "新增客户" }}</h2>
        <button v-if="form.code" class="primary create-button" @click="resetForm">新增</button>
      </div>
      <el-form label-position="top">
        <el-form-item label="客户编码">
          <el-input v-model="form.code" disabled placeholder="自动生成" />
        </el-form-item>
        <el-form-item>
          <template #label><span class="required-label">客户名称</span></template>
          <el-input v-model="form.name" placeholder="客户名称" />
        </el-form-item>
        <div class="form-row">
          <el-form-item label="联系人">
            <el-input v-model="form.contactPerson" placeholder="联系人" />
          </el-form-item>
          <el-form-item>
            <template #label><span class="required-label">联系电话</span></template>
            <el-input v-model="form.phone" placeholder="手机号或座机" />
          </el-form-item>
        </div>
        <div class="form-row">
          <el-form-item label="QQ">
            <el-input v-model="form.qq" placeholder="QQ号" />
          </el-form-item>
          <el-form-item label="微信">
            <el-input v-model="form.wechat" placeholder="微信号" />
          </el-form-item>
        </div>
        <el-form-item label="地址">
          <el-input v-model="form.address" placeholder="客户地址" />
        </el-form-item>
        <el-form-item label="备注">
          <el-select v-model="form.remark" class="wide-select" filterable allow-create clearable placeholder="选择或输入备注">
            <el-option v-for="item in store.remarkTemplates" :key="item.id" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <button class="primary wide" :disabled="store.submitting" @click.prevent="submit">
          {{ form.code ? "更新客户" : "保存客户" }}
        </button>
      </el-form>
    </div>

    <div class="panel">
      <div class="panel-head">
        <h2>客户资料</h2>
        <span>{{ store.customers.length }} 条</span>
      </div>
      <div class="query-bar">
        <el-date-picker class="query-control" v-model="filters.startDate" value-format="YYYY-MM-DD" placeholder="开始日期" />
        <el-date-picker class="query-control" v-model="filters.endDate" value-format="YYYY-MM-DD" placeholder="结束日期" />
        <el-input class="query-control" v-model="filters.code" placeholder="编码" />
        <el-input class="query-control" v-model="filters.name" placeholder="名称" />
        <button class="primary" :disabled="store.submitting" @click="search">查询</button>
        <button class="secondary" @click="clearFilters">清空</button>
      </div>
      <el-table :data="store.customers" height="420" empty-text="暂无客户" highlight-current-row @row-click="editRow">
        <el-table-column width="50" align="center">
          <template #default="{ row }">
            <el-checkbox :model-value="selectedCode === row.code" @click.stop />
          </template>
        </el-table-column>
        <el-table-column prop="code" label="编码" width="110" />
        <el-table-column prop="name" label="客户名称" min-width="160" />
        <el-table-column prop="contactPerson" label="联系人" width="110" />
        <el-table-column prop="phone" label="电话" min-width="140" />
        <el-table-column prop="qq" label="QQ" width="120" />
        <el-table-column prop="wechat" label="微信" width="130" />
        <el-table-column prop="address" label="地址" min-width="180" />
        <el-table-column prop="remark" label="备注" min-width="140" />
        <el-table-column label="操作" width="96" fixed="right">
          <template #default="{ row }">
            <button class="danger small" @click.stop="remove(row)">删除</button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </section>
</template>
