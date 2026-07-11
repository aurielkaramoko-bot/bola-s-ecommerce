package com.bolas.ecommerce.security;

import com.bolas.ecommerce.model.Customer;
import com.bolas.ecommerce.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(GoogleOAuth2SuccessHandler.class);

    private final CustomerService customerService;

    public GoogleOAuth2SuccessHandler(@Lazy CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        String googleId, email, firstName, lastName;

        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            googleId  = oidcUser.getAttribute("sub");
            email     = oidcUser.getAttribute("email");
            firstName = oidcUser.getAttribute("given_name");
            lastName  = oidcUser.getAttribute("family_name");
        } else {
            OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
            googleId  = oauthUser.getAttribute("sub");
            email     = oauthUser.getAttribute("email");
            firstName = oauthUser.getAttribute("given_name");
            lastName  = oauthUser.getAttribute("family_name");
        }

        log.debug("Google OAuth2 success: googleId={} email={}", googleId, email);

        try {
            Customer customer = customerService.loginOrCreateGoogle(googleId, email, firstName, lastName);
            HttpSession session = request.getSession();
            session.setAttribute("BOLAS_CUSTOMER", customer);
            response.sendRedirect("/");
        } catch (Exception e) {
            log.error("=== GOOGLE SUCCESS ERROR ===", e);
            response.sendRedirect("/customer/login?error");
        }
    }
}