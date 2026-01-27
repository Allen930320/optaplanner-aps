package com.upec.factoryscheduling.common.utils;

import com.upec.factoryscheduling.auth.entity.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 用户上下文工具类，用于在应用的任意地方获取当前登录用户信息
 */
public class UserContext {

    /**
     * 获取当前登录用户
     * @return 当前登录用户，如果未登录则返回null
     */
    public static User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User) {
            return (User) authentication.getPrincipal();
        }
        return null;
    }

    /**
     * 获取当前登录用户ID
     * @return 当前登录用户ID，如果未登录则返回null
     */
    public static Long getCurrentUserId() {
        User user = getCurrentUser();
        return user != null ? user.getId() : null;
    }

    /**
     * 获取当前登录用户名
     * @return 当前登录用户名，如果未登录则返回null
     */
    public static String getCurrentUsername() {
        User user = getCurrentUser();
        return user != null ? user.getUsername() : null;
    }

    /**
     * 获取当前登录用户邮箱
     * @return 当前登录用户邮箱，如果未登录则返回null
     */
    public static String getCurrentUserEmail() {
        User user = getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    /**
     * 获取当前登录用户电话
     * @return 当前登录用户电话，如果未登录则返回null
     */
    public static String getCurrentUserPhone() {
        User user = getCurrentUser();
        return user != null ? user.getPhone() : null;
    }

    /**
     * 判断用户是否已登录
     * @return 是否已登录
     */
    public static boolean isLoggedIn() {
        return getCurrentUser() != null;
    }
}
