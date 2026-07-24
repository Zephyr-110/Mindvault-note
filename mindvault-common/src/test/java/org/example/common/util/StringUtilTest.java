package org.example.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringUtilTest {

    @Test
    @DisplayName("content为null时，返回空字符串")
    void shouldReturnEmptyWhenContentIsNull() {
        String result = StringUtil.truncate(null,10);
        assertEquals("", result);
    }

    @Test
    @DisplayName("content长度小于等于maxLen时，返回content本身，不加省略号")
    void shouldReturnContentWhenContentLengthLessThanOrEqualToMaxLen() {
        String content = "hello world";
        String result = StringUtil.truncate(content,15);
        assertEquals(content, result);
    }

    @Test
    @DisplayName("content长度大于maxLen时，截断后追加省略号")
    void shouldTruncateAndAppendEllipsisWhenContentLengthGreaterThanMaxLen() {
        String content = "hello world";
        String result = StringUtil.truncate(content,5);
        assertEquals("hello...", result);
    }

    @Test
    @DisplayName("正常字符串、maxLen = 0时，返回省略号")
    void shouldReturnEllipsisWhenMaxLenIsZero() {
        String content = "hello world";
        String result = StringUtil.truncate(content,0);
        assertEquals("...", result);
    }
}
