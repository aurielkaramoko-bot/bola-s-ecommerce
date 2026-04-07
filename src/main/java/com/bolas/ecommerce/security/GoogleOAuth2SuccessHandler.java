package com.bolas.ecommerce.security;

import com.bolas.ecommerce.model.Customer;
import com.bolas.ecommerce.service.CustomerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GoogleOAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final CustomerService customerService;

    public GoogleOAuth2SuccessHandler(@Lazy CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();

        String googleId  = oauthUser.getAttribute("sub");
        String email     = oauthUser.getAttribute("email");
        String firstName = oauthUser.getAttribute("given_name");
        String lastName  = oauthUser.getAttribute("family_name");

        Customer customer = customerService.loginOrCreateGoogle(googleId, email, firstName, lastName);

        // Stocker le customer en session
        HttpSession session = request.getSession();
        session.setAttribute("BOLAS_CUSTOMER", customer);

        response.sendRedirect("/");
    }
}
