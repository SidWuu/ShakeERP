package com.factory.erp.common.exception;

import com.factory.erp.common.ApiResponse;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器。
 * 拦截所有 Controller 抛出的异常，统一转换为 ApiResponse 格式返回给前端。
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * 处理业务异常（如库存不足、条码重复）。
     */
    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleBusinessException(BusinessException exception) {
        return ApiResponse.error(exception.getMessage());
    }

    /**
     * 处理资源未找到异常（如商品不存在）。
     */
    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ApiResponse<Void> handleNotFoundException(NotFoundException exception) {
        return ApiResponse.error(exception.getMessage());
    }

    /**
     * 处理 @Valid 校验失败异常，返回具体缺少哪些字段。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleValidationException(MethodArgumentNotValidException exception) {
        String details = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ApiResponse.error("请求参数不完整：" + details);
    }

    /**
     * 处理缺少请求参数异常。
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ApiResponse<Void> handleMissingParam(MissingServletRequestParameterException exception) {
        return ApiResponse.error("缺少参数：" + exception.getParameterName());
    }
}
