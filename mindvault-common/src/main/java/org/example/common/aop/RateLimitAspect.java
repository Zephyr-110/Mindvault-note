package org.example.common.aop;


import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.example.common.annotation.RateLimit;
import org.example.common.exception.BusinessException;
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
        //拼redisKey
        String redisKey = "rate_limit:" + rateLimit.key() + ":" + ip;
        //原子自增，返回自增后的值
        Long count = redis.opsForValue().increment(redisKey);
        //如果是第一次访问，则设置过期时间为window
        if(count == 1){
            redis.expire(redisKey, Duration.ofSeconds(rateLimit.window()));
        }
        //超限直接拒绝
        if(count > rateLimit.limit()){
            throw new BusinessException(429, "系统繁忙，请稍后再试");
        }
        //没超限就放行
        return joinPoint.proceed();
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