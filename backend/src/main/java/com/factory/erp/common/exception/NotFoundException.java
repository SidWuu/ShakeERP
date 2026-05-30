package com.factory.erp.common.exception;

/**
 * 资源未找到异常，用于表示请求的数据不存在。
 * 例如：商品不存在、客户不存在等。
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
