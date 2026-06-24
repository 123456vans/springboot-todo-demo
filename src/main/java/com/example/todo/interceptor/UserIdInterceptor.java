package com.example.todo.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class UserIdInterceptor implements HandlerInterceptor {

    public static final String USER_ID_ATTRIBUTE = "currentUserId";

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws IOException {
        String userIdHeader = request.getHeader("userId");
        if (userIdHeader == null || userIdHeader.isBlank()) {
            writeUnauthorized(response);
            return false;
        }

        try {
            long userId = Long.parseLong(userIdHeader);
            if (userId <= 0) {
                writeUnauthorized(response);
                return false;
            }
            request.setAttribute(USER_ID_ATTRIBUTE, userId);
            return true;
        } catch (NumberFormatException exception) {
            writeUnauthorized(response);
            return false;
        }
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"请求头 userId 缺失或无效\"}");
    }
}
