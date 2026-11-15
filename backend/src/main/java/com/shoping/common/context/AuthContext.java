package com.shoping.common.context;

/**
 * 当前请求的登录上下文（由拦截器写入，请求结束清理）
 * 用于区分管理员端（role=1）与普通用户端（role=0）
 */
public final class AuthContext {

    /** 角色：顾客 */
    public static final int ROLE_CUSTOMER = 0;

    /** 角色：管理员 */
    public static final int ROLE_ADMIN = 1;

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> ROLE = new ThreadLocal<>();
    private static final ThreadLocal<String> USERNAME = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(Long userId, Integer role, String username) {
        USER_ID.set(userId);
        ROLE.set(role);
        USERNAME.set(username);
    }

    public static Long userId() {
        return USER_ID.get();
    }

    public static Integer role() {
        return ROLE.get();
    }

    public static String username() {
        return USERNAME.get();
    }

    public static boolean isAdmin() {
        Integer role = ROLE.get();
        return role != null && role == ROLE_ADMIN;
    }

    /** 当前登录用户 ID，未登录抛异常（兜底，正常已被拦截器拦下） */
    public static Long requireUserId() {
        Long userId = USER_ID.get();
        if (userId == null) {
            throw new IllegalStateException("未登录");
        }
        return userId;
    }

    public static void clear() {
        USER_ID.remove();
        ROLE.remove();
        USERNAME.remove();
    }
}
