package com.memento.util;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

/**
 * 安全工具类，用于获取当前登录用户
 * 暂时通过请求头获取，未来可轻松切换为 Spring Security
 */
public class SecurityUtils {

    private static final String USER_ID_HEADER = "X-User-Id";

    public static Long getCurrentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String userIdStr = request.getHeader(USER_ID_HEADER);
            if (userIdStr != null && !userIdStr.isEmpty()) {
                try {
                    return Long.parseLong(userIdStr);
                } catch (NumberFormatException e) {
                    // 忽略错误格式
                }
            }
        }
        
        // 默认返回 1L，保持兼容性
        return 1L;
    }
}
