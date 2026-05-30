const API_BASE = import.meta.env.VITE_API_BASE_URL || `${window.location.protocol}//${window.location.hostname}:8080`;

async function request(path, options = {}) {
  const token = localStorage.getItem("shake_token") || "";
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {})
  };
  // 登录接口不带 token
  if (token && !path.includes("/auth/login")) {
    headers["Authorization"] = `Bearer ${token}`;
  }
  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers
  });

  const payload = await response.json();
  if (!response.ok || !payload.success) {
    throw new Error(payload.message || "请求失败");
  }
  return payload.data;
}

function withQuery(path, filters = {}) {
  const params = new URLSearchParams();
  Object.entries(filters).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== "") {
      params.set(key, value);
    }
  });
  const query = params.toString();
  return query ? `${path}?${query}` : path;
}

export function fetchHealth() {
  return request("/api/health");
}

import md5 from "blueimp-md5";

export function loginUser(username, password) {
  const passwordMd5 = md5(password);
  return request("/api/auth/login", {
    method: "POST",
    body: JSON.stringify({ username, password: passwordMd5 })
  });
}

export function fetchCurrentUser() {
  return request("/api/auth/me");
}

export function fetchCustomers(filters) {
  return request(withQuery("/api/customers", filters));
}

export function createCustomer(customer) {
  return request("/api/customers", {
    method: "POST",
    body: JSON.stringify(customer)
  });
}

export function updateCustomer(code, customer) {
  return request(`/api/customers/${encodeURIComponent(code)}`, {
    method: "PUT",
    body: JSON.stringify(customer)
  });
}

export function deleteCustomer(code) {
  return request(`/api/customers/${encodeURIComponent(code)}`, {
    method: "DELETE"
  });
}

export function fetchProducts(filters) {
  return request(withQuery("/api/products", filters));
}

export function createProduct(product) {
  return request("/api/products", {
    method: "POST",
    body: JSON.stringify(product)
  });
}

export function updateProduct(code, product) {
  return request(`/api/products/${encodeURIComponent(code)}`, {
    method: "PUT",
    body: JSON.stringify(product)
  });
}

export function deleteProduct(code) {
  return request(`/api/products/${encodeURIComponent(code)}`, {
    method: "DELETE"
  });
}

export function fetchInventorySummary(filters) {
  return request(withQuery("/api/inventory/summary", filters));
}

export function saveInventory(inventory) {
  return request("/api/inventory", {
    method: "POST",
    body: JSON.stringify(inventory)
  });
}

export function updateInventory(code, inventory) {
  return request(`/api/inventory/${encodeURIComponent(code)}`, {
    method: "PUT",
    body: JSON.stringify(inventory)
  });
}

export function deleteInventory(productCode, operator = "系统管理员") {
  return request(withQuery(`/api/inventory/${encodeURIComponent(productCode)}`, { operator }), {
    method: "DELETE"
  });
}

export function scanProduct(barcode) {
  return request(`/api/scanner/products/${encodeURIComponent(barcode)}`);
}

export function stockIn(action) {
  return request("/api/stock-in", {
    method: "POST",
    body: JSON.stringify(action)
  });
}

export function stockOut(action) {
  return request("/api/stock-out", {
    method: "POST",
    body: JSON.stringify(action)
  });
}

export function fetchStockMovements(filters) {
  return request(withQuery("/api/stock-movements", filters));
}

export function createBackup() {
  return request("/api/admin/backup", { method: "POST" });
}

export function fetchBackups() {
  return request("/api/admin/backups");
}

export function getExportUrl() {
  return `${API_BASE}/api/admin/export`;
}

export function restoreBackup(filename) {
  return request(`/api/admin/restore/${encodeURIComponent(filename)}`, { method: "POST" });
}

export function deleteBackup(filename) {
  return request(`/api/admin/backups/${encodeURIComponent(filename)}`, { method: "DELETE" });
}

export function fetchDictCategories() {
  return request("/api/admin/dict/categories");
}

export function createDictCategory(name) {
  return request("/api/admin/dict/categories", {
    method: "POST",
    body: JSON.stringify({ name })
  });
}

export function deleteDictCategory(id) {
  return request(`/api/admin/dict/categories/${id}`, { method: "DELETE" });
}

export function fetchDictItems(filters = {}) {
  return request(withQuery("/api/admin/dict", filters));
}

export function createDictItem(item) {
  return request("/api/admin/dict", {
    method: "POST",
    body: JSON.stringify(item)
  });
}

export function deleteDictItem(id) {
  return request(`/api/admin/dict/${id}`, { method: "DELETE" });
}

export function fetchUsers() {
  return request("/api/admin/users");
}

export async function uploadImage(file) {
  const token = localStorage.getItem("shake_token") || "";
  const formData = new FormData();
  formData.append("file", file);

  const response = await fetch(`${API_BASE}/api/upload`, {
    method: "POST",
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    },
    body: formData
  });

  const payload = await response.json();
  if (!response.ok || !payload.success) {
    throw new Error(payload.message || "上传失败");
  }
  return payload.data;
}

export function getImageUrl(path) {
  if (!path) return "";
  if (path.startsWith("http")) return path;
  return `${API_BASE}${path}`;
}
