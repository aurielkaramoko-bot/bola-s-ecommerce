package com.bolas.ecommerce.security;

import com.bolas.ecommerce.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class BolasAuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final LoginAttemptService loginAttemptService;
    private final AuditLogService auditLogService;

    public BolasAuthenticationFailureHandler(LoginAttemptService loginAttemptService,
                                             AuditLogService auditLogService) {
        super("/admin/login?error");
        this.loginAttemptService = loginAttemptService;
        this.auditLogService = auditLogService;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String username = request.getParameter("username");
        loginAttemptService.loginFailed(username);
        auditLogService.loginFailure(username, clientIp(request));
        try {
            super.onAuthenticationFailure(request, response, exception);
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/admin/login?error");
        }
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            String[] parts = xff.split(",");
            return parts[parts.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
