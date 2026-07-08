package org.example.common.logincheck;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

//启动时创建实例
@Component
public class JwtUtil {

    //密钥
    private final SecretKey secretKey;
    //过期时间
    private final long expiration;

    //构造方法
    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        //先校验是不是没拿到密钥
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT secret 未配置，请设置环境变量 JWT_SECRET 或在 application-local.yaml 中配置 jwt.secret");
        }
        //再校验密钥长度是否足够
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "JWT secret 长度不足，当前 " + keyBytes.length + " 字节，至少需要 32 字节（256 bits）");
        }
        //初始化密钥
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.expiration = expiration;
    }
    //获取总过期时间，给拦截器续期用
    public long getTotalExpiration() {
        return expiration;
    }
    //生成JWT令牌
    public String generateToken(Long userId){
        Date now = new Date();
        //过期时间
        Date expiryDate = new Date(now.getTime() + expiration);
        //通过一系列方法生成JWT令牌
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }
    //解析JWT令牌
    public Long getUserIdFromToken(String token){
        //从JWT令牌中提取用户ID
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.parseLong(claims.getSubject());
    }
    //获取JWT令牌的剩余过期时间
    public long getRemainingExpiration(String token){
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            long remaining = claims.getExpiration().getTime() - System.currentTimeMillis();
            return Math.max(remaining, 0);
        } catch (Exception e) {
            return 0;
        }
    }
    //验证JWT令牌，主要给拦截器用的
    public boolean validateToken(String token){
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}