/**
 * 校验必填字段。
 * @param {object} form - 表单数据对象
 * @param {Array<{field: string, label: string}>} rules - 必填规则
 * @returns {string|null} 校验失败返回错误提示，通过返回 null
 */
export function validateRequired(form, rules) {
  const missing = [];
  for (const rule of rules) {
    const value = form[rule.field];
    if (value === undefined || value === null || String(value).trim() === "") {
      missing.push(rule.label);
    }
  }
  if (missing.length > 0) {
    return `请填写必填项：${missing.join("、")}`;
  }
  return null;
}
