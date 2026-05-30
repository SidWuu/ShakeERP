package com.factory.erp.common;

/**
 * 统一 API 响应结构。
 * 所有接口返回此格式，前端根据 success 字段判断请求是否成功。
 *
 * @param success 是否成功
 * @param data    响应数据（失败时为 null）
 * @param message 提示信息
 */
public record ApiResponse<T>(boolean success, T data, String message) {

    /** 成功响应 */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, "OK");
    }

    /** 失败响应 */
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
