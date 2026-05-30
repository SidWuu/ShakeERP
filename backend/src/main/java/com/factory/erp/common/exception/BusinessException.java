package com.factory.erp.common.exception;

/**
 * 业务异常，用于表示业务规则校验失败的情况。
 * 例如：库存不足、条码重复等。
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
