package com.hcz.nexusbackend.util;

import java.util.Map;

public class SecurityUtils {

    private static final ThreadLocal<Map<String, Object>> USER_CONTEXT = new ThreadLocal<>();

    public static void set(Map<String, Object> user) {
        USER_CONTEXT.set(user);
    }

    public static Map<String, Object> get() {
        return USER_CONTEXT.get();
    }

    public static Long getUserId() {
        Map<String, Object> user = get();
        if (user == null || user.get("userId") == null) {
            return null;
        }
        return Long.valueOf(user.get("userId").toString());
    }

    public static String getGlobalRole() {
        Map<String, Object> user = get();
        if (user == null) {
            return null;
        }
        return (String) user.get("globalRole");
    }

    public static void clear() {
        USER_CONTEXT.remove();
    }
}
