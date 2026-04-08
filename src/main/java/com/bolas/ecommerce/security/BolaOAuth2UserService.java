package com.bolas.ecommerce.security;

import com.bolas.ecommerce.model.Customer;
import com.bolas.ecommerce.service.CustomerService;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class BolaOAuth2UserService extends OidcUserService {

    private final CustomerService customerService;

    public BolaOAuth2UserService(@Lazy CustomerService customerService) {
        this.customerService = customerService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        return super.loadUser(userRequest);
    }
}
