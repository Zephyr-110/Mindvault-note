package org.example.common.logincheck;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class LoginInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redis;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler){
        //预检请求直接放行
        if("OPTIONS".equalsIgnoreCase(request.getMethod())){
            return true;
        }

        String token = request.getHeader("Authorization");
        //判断token是否存在以及是否以Bearer开头，若不然则返回401
        if(token == null || !token.startsWith("Bearer ")){
            response.setStatus(401);
            return false;
        }
        //掐头去尾，只保留token部分，掐掉的就是Bearer
        token = token.substring(7);
        if(!jwtUtil.validateToken(token)){
            response.setStatus(401);
            return false;
        }
        //判断token是否在黑名单中，若在则返回401
        try {
            if(Boolean.TRUE.equals(redis.hasKey("blacklist:" + token))){
                response.setStatus(401);
                return false;
            }
        } catch (Exception e) {
            log.error("Redis不可用，黑名单校验降级放行", e);
        }
        //从token中获取用户ID
        Long userId = jwtUtil.getUserIdFromToken(token);
        //设置用户ID
        UserContext.setUserId(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex){
        //请求处理完成后，清空用户信息
        UserContext.remove();
    }
}