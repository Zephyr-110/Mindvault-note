package org.example.common.aop;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.common.annotation.RateLimit;
import org.example.common.exception.BusinessException;
import org.example.common.logincheck.UserContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

@Slf4j
@Component
@Aspect
@RequiredArgsConstructor
public class RateLimitAspect {

    private final StringRedisTemplate redis;
    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint joinPoint, RateLimit rateLimit) throws Throwable {
        //拿到该请求的HttpServletRequest对象
        ServletRequestAttributes attributes =
                (ServletRequestAttributes)RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        //获取客户端真实IP
        String ip = getClientIp(request);
        
        //IP维度限流：防止单机多账号刷接口
        String ipKey = "rate_limit:ip:" + rateLimit.key() + ":" + ip;
        boolean ipOverLimit = isOverLimit(ipKey, rateLimit);
        
        //用户维度限流：防止同一账号多IP刷接口，只对已登录用户生效
        boolean userOverLimit = false;
        Long userId = UserContext.getUserId();
        if (userId != null) {
            String userKey = "rate_limit:user:" + rateLimit.key() + ":" + userId;
            userOverLimit = isOverLimit(userKey, rateLimit);
        }
        
        //任意一个维度超限，直接拒绝
        if (ipOverLimit || userOverLimit) {
            throw new BusinessException(429, "系统繁忙，请稍后再试");
        }
        
        //两个维度都没超限，放行
        return joinPoint.proceed();
    }
    
    //检查指定key是否超限，incr + 设过期 + 判断
    private boolean isOverLimit(String key, RateLimit rateLimit) {
        Long count = redis.opsForValue().increment(key);
        //第一次访问设置过期时间
        if (count == 1) {
            redis.expire(key, Duration.ofSeconds(rateLimit.window()));
        }
        return count > rateLimit.limit();
    }

    private String getClientIp(HttpServletRequest request) {
        //1.先看 X-Forwarded-For（经过代理时的真实 IP）
        String ip = request.getHeader("X-Forwarded-For");
        //2.再看 X-Real-IP（Nginx 常用）
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        //3.兜底用直连 IP
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        //X-Forwarded-For可能是一串"客户端,代理1,代理2",取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}