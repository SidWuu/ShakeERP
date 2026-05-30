<script setup>
import { reactive } from "vue";
import { useRouter } from "vue-router";
import { useAppStore } from "../stores/app";

const store = useAppStore();
const router = useRouter();

const form = reactive({
  username: "admin",
  password: "admin123"
});

async function submit() {
  if (!form.username.trim() || !form.password.trim()) {
    store.error = "请输入用户名和密码";
    return;
  }
  try {
    await store.login(form.username, form.password);
    await store.loadAll();
    router.push("/");
  } catch {
    // error already set in store
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-brand">
        <span class="brand-mark">工</span>
        <div>
          <p>ShakeERP</p>
          <strong>金东进销存</strong>
        </div>
      </div>

      <el-form label-position="top" @submit.prevent="submit">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="admin / warehouse / sales" @keyup.enter="submit" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" placeholder="请输入密码" show-password @keyup.enter="submit" />
        </el-form-item>

        <div v-if="store.error" class="login-error">{{ store.error }}</div>

        <button class="primary wide login-btn" :disabled="store.submitting" @click.prevent="submit">
          {{ store.submitting ? "登录中..." : "登 录" }}
        </button>
      </el-form>

      <div class="login-hint">
        <p>默认账号：</p>
        <ul>
          <li>老板：admin / admin123</li>
          <li>仓管：warehouse / wh123</li>
          <li>销售：sales / sales123</li>
        </ul>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: #f4f7f6;
}

.login-card {
  width: 380px;
  padding: 36px 32px;
  background: #fff;
  border-radius: 12px;
  border: 1px solid #dce6e2;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06);
}

.login-brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.login-brand .brand-mark {
  width: 42px;
  height: 42px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: #f0b85a;
  color: #14352b;
  font-weight: 800;
}

.login-brand p {
  margin: 0;
  color: #a9c7bd;
  font-size: 12px;
}

.login-brand strong {
  margin: 0;
  font-size: 18px;
  color: #14352b;
}

.login-error {
  margin-bottom: 12px;
  padding: 10px 12px;
  border-radius: 6px;
  background: #fff1f0;
  color: #a23a33;
  font-size: 13px;
  border: 1px solid #ffd1cd;
}

.login-btn {
  margin-top: 8px;
  height: 44px;
  font-size: 16px;
}

.login-hint {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e6eeeb;
  color: #657a72;
  font-size: 12px;
}

.login-hint p {
  margin: 0 0 6px;
}

.login-hint ul {
  margin: 0;
  padding-left: 18px;
  line-height: 1.8;
}
</style>
