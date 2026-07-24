package org.example.common.logincheck;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试
 * 不依赖 Spring 容器，直接 new 出来测
 */
class JwtUtilTest {

    // 测试用的密钥，长度必须 ≥ 32 字节
    private static final String TEST_SECRET = "this-is-a-very-long-secret-for-testing!!";
    private static final long TEST_EXPIRATION = 3600000L; // 1小时

    private JwtUtil jwtUtil;

    /**
     * @BeforeEach：每个 @Test 执行前都会跑一遍这个方法
     * 保证每次测试都是干净的 JwtUtil 实例
     */
    @BeforeEach
    void setUp() {
        //用我们的假数据初始化 JwtUtil 实例
        jwtUtil = new JwtUtil(TEST_SECRET, TEST_EXPIRATION);
    }

    @Test
    @DisplayName("生成 token 后能正确解析出 userId")
    void shouldGenerateAndParseToken() {
        // 1. 生成 token
        String token = jwtUtil.generateToken(10086L);

        // 2. 解析 token 拿到 userId
        Long userId = jwtUtil.getUserIdFromToken(token);

        // 3. 断言：解析出来的 userId 应该和生成时一致
        assertEquals(10086L, userId);
    }

    @Test
    @DisplayName("合法 token 校验通过，被篡改的 token 校验失败")
    void shouldValidateToken() {
        String token = jwtUtil.generateToken(1L);

        // 合法 token 应该通过
        assertTrue(jwtUtil.validateToken(token));

        // 篡改后的 token 不应该通过
        String tampered = token.substring(0, token.length() - 3) + "xxx";
        assertFalse(jwtUtil.validateToken(tampered));
    }

    @Test
    @DisplayName("获取总过期时间应该返回配置的过期时间")
    void shouldGetTotalExpiration() {
        assertEquals(TEST_EXPIRATION, jwtUtil.getTotalExpiration());
    }

    @Test
    @DisplayName("构造方法密钥太短抛异常")
    void shouldThrowExceptionWhenSecretTooShort() {
        // 密钥长度必须 ≥ 32 字节
        assertThrows(IllegalStateException.class, () -> new JwtUtil("zhegemiyaofeichangduan", TEST_EXPIRATION));
    }

    @Test
    @DisplayName("密钥为 null 时构造失败")
    void shouldThrowWhenSecretIsNull() {
        assertThrows(IllegalStateException.class, () -> new JwtUtil(null, TEST_EXPIRATION));
    }

    @Test
    @DisplayName("正常 token 获取剩余过期时间应大于 0")
    void shouldReturnPositiveRemainingExpiration() {
        String token = jwtUtil.generateToken(1L);
        long remaining = jwtUtil.getRemainingExpiration(token);
        assertTrue(remaining > 0);
    }

    @Test
    @DisplayName("乱码 token 获取剩余过期时间应返回 0")
    void shouldReturnZeroForGarbledToken() {
        long remaining = jwtUtil.getRemainingExpiration("this-is-not-a-valid-token");
        assertEquals(0, remaining);
    }

    @Test
    @DisplayName("解析乱码 token 应抛异常")
    void shouldThrowWhenParsingGarbledToken() {
        assertThrows(Exception.class, () -> jwtUtil.getUserIdFromToken("garbage-token"));
    }
}