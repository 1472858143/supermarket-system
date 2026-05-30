package com.supermarket.inventory.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.inventory.auth.service.JwtTokenService;
import com.supermarket.inventory.common.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtTokenService jwtTokenService;
    private final ObjectMapper objectMapper;

    public AuthInterceptor(JwtTokenService jwtTokenService, ObjectMapper objectMapper) {
        this.jwtTokenService = jwtTokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 所有业务接口统一通过 Bearer Token 认证，避免 Controller 分散处理登录态。
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeError(response, 401, "未登录或认证失败");
            return false;
        }

        CurrentUser currentUser;
        try {
            currentUser = jwtTokenService.parseToken(authorization.substring("Bearer ".length()));
        } catch (RuntimeException exception) {
            writeError(response, 401, exception.getMessage());
            return false;
        }
        CurrentUserContext.set(currentUser);

        // 角色权限以后端注解为准，前端隐藏菜单和按钮只负责改善用户体验。
        RequireRoles requireRoles = resolveRequireRoles(handler);
        if (requireRoles != null && Arrays.stream(requireRoles.value()).noneMatch(currentUser::hasRole)) {
            writeError(response, 403, "无权限访问该资源");
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束后清理线程上下文，防止线程复用时串用上一个用户信息。
        CurrentUserContext.clear();
    }

    private RequireRoles resolveRequireRoles(Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return null;
        }
        RequireRoles methodAnnotation = handlerMethod.getMethodAnnotation(RequireRoles.class);
        if (methodAnnotation != null) {
            return methodAnnotation;
        }
        return handlerMethod.getBeanType().getAnnotation(RequireRoles.class);
    }

    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        response.setStatus(code);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(code, message)));
    }
}
