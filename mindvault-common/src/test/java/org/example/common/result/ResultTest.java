package org.example.common.result;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class ResultTest {

    @Test
    @DisplayName("成功时data不为空")
    public void testSuccessWithData() {
        Result<String> result = Result.success("hello world");
        Assertions.assertEquals("hello world", result.getData());
    }

    @Test
    @DisplayName("成功时data为空")
    public void testSuccessWithoutData() {
        Result<String> result = Result.success();
        Assertions.assertNull(result.getData());
    }

    @Test
    @DisplayName("400,参数错误")
    public void testParamError() {
        Result<String> result = Result.error(400,"参数错误");
        Assertions.assertEquals("参数错误", result.getMessage());
        Assertions.assertEquals(400, result.getCode());
    }

    @Test
    @DisplayName("500,系统繁忙，请稍后再试")
    public void testSystemError() {
        Result<String> result = Result.error(500,"系统繁忙，请稍后再试");
        Assertions.assertEquals("系统繁忙，请稍后再试", result.getMessage());
        Assertions.assertEquals(500, result.getCode());
    }
}
