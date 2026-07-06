package org.example.common.config;

import lombok.RequiredArgsConstructor;
import org.example.common.logincheck.JwtUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/user", "/topic", "/queue");
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .setHandshakeHandler(new JwtHandshakeHandler())
                .addInterceptors(new JwtHandshakeInterceptor(jwtUtil));
    }

    /**
     * 握手成功后，从 attributes 取出 userId 封装为 Principal
     * Spring STOMP 用 Principal 标识当前连接的用户
     */
    private static class JwtHandshakeHandler extends DefaultHandshakeHandler {
        @Override
        protected Principal determineUser(ServerHttpRequest request, WebSocketHandler wsHandler,
                                           Map<String, Object> attributes) {
            Long userId = (Long) attributes.get("userId");
            return userId != null ? () -> String.valueOf(userId) : null;
        }
    }

    /**
     * 握手拦截器：从 Sec-WebSocket-Protocol 提取 token 并校验
     * token 放在请求头而不是 URL，避免被服务器日志记录
     */
    @RequiredArgsConstructor
    private static class JwtHandshakeInterceptor implements HandshakeInterceptor {

        private final JwtUtil jwtUtil;

        @Override
        public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                        WebSocketHandler wsHandler, Map<String, Object> attributes) {
            // 从 Sec-WebSocket-Protocol 请求头提取 token
            List<String> protocols = request.getHeaders().get("Sec-WebSocket-Protocol");
            if (protocols == null || protocols.isEmpty()) {
                return false;
            }
            // 格式：v10.stomp, jwt.xxx... 我们找 jwt. 开头的就是 token
            String token = null;
            for (String protocol : protocols) {
                if (protocol.startsWith("jwt.")) {
                    token = protocol.substring(4);
                    break;
                }
            }
            if (token == null || token.isBlank()) {
                return false;
            }
            try {
                Long userId = jwtUtil.getUserIdFromToken(token);
                attributes.put("userId", userId);
                return true;
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Exception exception) {
        }
    }
}