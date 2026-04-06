package com.bolas.ecommerce.security;

import com.bolas.ecommerce.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class BolasAuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuditLogService auditLogService;

    public BolasAuthenticationSuccessHandler(AuditLogService auditLogService) {
        super("/admin/dashboard");
        this.auditLogService = auditLogService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        auditLogService.loginSuccess(authentication.getName(), clientIp(request));
        try {
            super.onAuthenticationSuccess(request, response, authentication);
        } catch (Exception e) {
            response.sendRedirect(request.getContextPath() + "/admin/dashboard");
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
