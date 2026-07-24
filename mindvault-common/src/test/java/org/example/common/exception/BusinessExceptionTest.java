package org.example.common.exception;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BusinessExceptionTest {

    @Test
    @DisplayName("400,参数错误")
    public void isException() {
        BusinessException businessException = new BusinessException(400, "参数错误");
        Assertions.assertEquals(400, businessException.getCode());
        Assertions.assertEquals("参数错误", businessException.getMessage());
    }

    @Test
    @DisplayName("自定义异常是否运行时异常的子类")
    public void isRuntimeException() {
        BusinessException businessException = new BusinessException(400, "参数错误");
        Assertions.assertInstanceOf(RuntimeException.class, businessException);
    }
}
