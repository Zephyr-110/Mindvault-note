package org.example.common.exception;

/**
 * 自定义业务异常
 * 当 Service 层遇到逻辑错误（如用户名重复、余额不足）时抛出
 */
public class BusinessException extends RuntimeException {
    private int code;  // 异常对应的状态码，方便返回给前端

    public BusinessException(int code, String message) {
        super(message);          // 把 message 传给父类 RuntimeException
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}

