package com.bolas.ecommerce.security;

import com.bolas.ecommerce.service.SessionCounterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Enregistre l'activité de chaque visiteur (pages HTML uniquement, pas les assets).
 * Permet de compter les vrais visiteurs actifs dans les 5 dernières minutes.
 */
@Component
public class ActivityTrackingFilter extends OncePerRequestFilter {

    private final SessionCounterService sessionCounter;

    public ActivityTrackingFilter(SessionCounterService sessionCounter) {
        this.sessionCounter = sessionCounter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String uri = request.getRequestURI();
        // Ne tracker que les pages HTML, pas les assets statiques ni les API
        if (!uri.startsWith("/css/") && !uri.startsWith("/js/") && !uri.startsWith("/images/")
                && !uri.startsWith("/uploads/") && !uri.startsWith("/api/")
                && !uri.contains(".") ) {
            var session = request.getSession(false);
            if (session != null) {
                sessionCounter.recordActivity(session.getId());
            }
        }
        chain.doFilter(request, response);
    }
}
