package org.example.common;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordUtil {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /** 加密 */
    public static String encode(String rawPassword) {//返回密文
        return encoder.encode(rawPassword);
    }

    /** 验证 */
    public static boolean matches(String rawPassword, String encodedPassword) {//返回true/false
        return encoder.matches(rawPassword, encodedPassword);
    }
}