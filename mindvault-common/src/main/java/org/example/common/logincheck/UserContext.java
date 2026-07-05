package org.example.common.logincheck;

public class UserContext {

    // 用户ID线程本地存储
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    //存储用户ID
    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    //获取用户ID
    public static Long getUserId() {
        return USER_ID.get();
    }

    //移除用户ID
    public static void remove(){
        USER_ID.remove();
    }
}