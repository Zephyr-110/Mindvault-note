package org.example.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PasswordUtilTest {


    @Test
    @DisplayName("encode后返回的不是明文")
    void shouldEncode() {
        String rawPassword = "123456";
        String encodedPassword = PasswordUtil.encode(rawPassword);
        assertNotEquals(rawPassword, encodedPassword);
    }

    @Test
    @DisplayName("encode同一个密码两次，结果不一样，检验盐值是否随机")
    void shouldGenerateDifferentEncodedPassword() {
        String rawPassword = "123456";
        String encodedPassword1 = PasswordUtil.encode(rawPassword);
        String encodedPassword2 = PasswordUtil.encode(rawPassword);
        assertNotEquals(encodedPassword1, encodedPassword2);
    }

    @Test
    @DisplayName("matches返回true")
    void shouldMatch() {
        String rawPassword = "123456";
        String encodedPassword = PasswordUtil.encode(rawPassword);
        assertTrue(PasswordUtil.matches(rawPassword, encodedPassword));
    }

    @Test
    @DisplayName("matches返回false")
    void shouldNotMatch() {
        String rawPassword = "123456";
        String encodedPassword = PasswordUtil.encode(rawPassword);
        assertFalse(PasswordUtil.matches("654321", encodedPassword));
    }
}
