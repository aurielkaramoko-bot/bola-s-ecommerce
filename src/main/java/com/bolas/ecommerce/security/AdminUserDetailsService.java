package com.bolas.ecommerce.security;

import com.bolas.ecommerce.repository.AdminUserRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import org.springframework.context.annotation.Primary;

@Service
@Primary
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserRepository adminUserRepository;
    private final LoginAttemptService loginAttemptService;

    public AdminUserDetailsService(AdminUserRepository adminUserRepository,
                                   LoginAttemptService loginAttemptService) {
        this.adminUserRepository = adminUserRepository;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = adminUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur inconnu"));

        boolean locked = loginAttemptService.isBlocked(username);
        return User.withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles("ADMIN")
                .accountLocked(locked)
                .build();
    }
}
