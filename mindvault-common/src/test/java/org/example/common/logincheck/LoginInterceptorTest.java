package org.example.common.logincheck;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoginInterceptorTest {

    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate redis;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    private MockedStatic<UserContext> userContextMockedStatic;

    @BeforeEach
    void setUp(){
        userContextMockedStatic = mockStatic(UserContext.class);
    }

    @AfterEach
    void tearDown(){
        userContextMockedStatic.close();
    }

    @InjectMocks
    private LoginInterceptor loginInterceptor;

    @Test
    @DisplayName("OPTIONS请求直接放行")
    public void testPreHandle_OPTIONS() {
        //设置请求方法为OPTIONS
        when(request.getMethod()).thenReturn("OPTIONS");
        //调用拦截器
        boolean result = loginInterceptor.preHandle(request, response, null);
        //如果结果为true，说明OPTIONS请求直接放行
        assertTrue(result);
    }

    @Test
    @DisplayName("无Authorization头，响应401状态码，返回false")
    public void testPreHandle_NoAuthorizationHeader() {
        //设置请求方法为GET
        when(request.getMethod()).thenReturn("GET");
        //设置无Authorization头
        when(request.getHeader("Authorization")).thenReturn(null);
        //调用拦截器
        boolean result = loginInterceptor.preHandle(request, response, null);
        //如果结果为false，说明无Authorization头，响应401状态码
        assertFalse(result);
        verify(response).setStatus(401);
    }

    @Test
    @DisplayName("Authorization不以Bearer开头，响应401状态码，返回false")
    public void testPreHandle_InvalidAuthorizationHeader() {
        //设置请求方法为GET
        when(request.getMethod()).thenReturn("GET");
        //设置Authorization头，不以Bearer开头
        when(request.getHeader("Authorization")).thenReturn("InvalidToken");
        //调用拦截器
        boolean result = loginInterceptor.preHandle(request, response, null);
        //如果结果为false，说明Authorization不以Bearer开头，响应401状态码
        assertFalse(result);
        verify(response).setStatus(401);
    }

    @Test
    @DisplayName("token校验失败->401")
    public void testPreHandle_TokenValidationFailed() {
        //设置请求方法为GET
        when(request.getMethod()).thenReturn("GET");
        //设置Authorization头，以Bearer开头
        when(request.getHeader("Authorization")).thenReturn("Bearer InvalidToken");
        //模拟token校验失败
        when(jwtUtil.validateToken("InvalidToken")).thenReturn(false);
        //调用拦截器
        boolean result = loginInterceptor.preHandle(request, response, null);
        //如果结果为false，说明token校验失败，响应401状态码
        assertFalse(result);
        verify(response).setStatus(401);
    }

    @Test
    @DisplayName("token在黑名单中，返回401")
    public void testPreHandle_TokenInBlacklist() {
        //设置请求方法为GET
        when(request.getMethod()).thenReturn("GET");
        //设置Authorization头，以Bearer开头
        when(request.getHeader("Authorization")).thenReturn("Bearer ValidToken");
        //模拟token校验通过
        when(jwtUtil.validateToken("ValidToken")).thenReturn(true);
        //模拟token在黑名单中
        when(redis.hasKey("blacklist:ValidToken")).thenReturn(true);
        //调用拦截器
        boolean result = loginInterceptor.preHandle(request, response, null);
        //如果结果为false，说明token在黑名单中，响应401状态码
        assertFalse(result);
        verify(response).setStatus(401);
    }

    @Test
    @DisplayName("token自动续期")
    public void testPreHandle_TokenAutoRenewal() {
        //设置请求方法为GET
        when(request.getMethod()).thenReturn("GET");
        //设置Authorization头，以Bearer开头
        when(request.getHeader("Authorization")).thenReturn("Bearer ValidToken");
        //模拟token校验通过
        when(jwtUtil.validateToken("ValidToken")).thenReturn(true);
        //模拟token不在黑名单中
        when(redis.hasKey("blacklist:ValidToken")).thenReturn(false);
        //模拟从token中获取用户ID
        when(jwtUtil.getUserIdFromToken("ValidToken")).thenReturn(10086L);
        //模拟token自动续期
        when(jwtUtil.getRemainingExpiration("ValidToken")).thenReturn(1000L);
        //模拟一个token周期很长
        when(jwtUtil.getTotalExpiration()).thenReturn(3600000L);
        //生成新token
        when(jwtUtil.generateToken(10086L)).thenReturn("NewValidToken");
        //调用拦截器
        boolean result = loginInterceptor.preHandle(request, response, null);
        //如果结果为true，说明token自动续期成功，响应200状态码
        assertTrue(result);
    }

    @Test
    @DisplayName("正常情况，token校验通过")
    public void testPreHandle_NormalCase() {
        //设置请求方法为GET
        when(request.getMethod()).thenReturn("GET");
        //设置Authorization头，以Bearer开头
        when(request.getHeader("Authorization")).thenReturn("Bearer ValidToken");
        //模拟token校验通过
        when(jwtUtil.validateToken("ValidToken")).thenReturn(true);
        //模拟token不在黑名单中
        when(redis.hasKey("blacklist:ValidToken")).thenReturn(false);
        //模拟从token中获取用户ID
        when(jwtUtil.getUserIdFromToken("ValidToken")).thenReturn(10086L);
        //模拟剩余有效时间很长
        when(jwtUtil.getRemainingExpiration("ValidToken")).thenReturn(3600000L);
        //模拟等于剩余，不走续期
        when(jwtUtil.getTotalExpiration()).thenReturn(3600000L);
        //调用拦截器
        boolean result = loginInterceptor.preHandle(request, response, null);
        //如果结果为true，说明正常情况，token校验通过，响应200状态码
        assertTrue(result);
    }
}
