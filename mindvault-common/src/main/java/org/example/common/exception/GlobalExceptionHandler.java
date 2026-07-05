package org.example.common.exception;


import lombok.extern.slf4j.Slf4j;
import org.example.common.result.Result;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常拦截器
 * 所有 Controller 抛出的异常都会在这里被捕获并转成 Result 返回
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 捕获我们自己的 BusinessException
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        // 用异常自带的 code 和 message，不要写死
        return Result.error(e.getCode(), e.getMessage());
    }

    // 捕获其他所有未处理的异常（如空指针、数据库错误）
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("服务器内部错误", e);
        return Result.error(500, "服务器内部错误");
    }

    // 捕获参数校验异常
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return Result.error(400, message);
    }
}