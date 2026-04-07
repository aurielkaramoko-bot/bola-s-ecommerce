package com.bolas.ecommerce.security;

import com.bolas.ecommerce.model.VendorUser;
import com.bolas.ecommerce.repository.VendorUserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VendorUserDetailsService implements UserDetailsService {

    private final VendorUserRepository vendorUserRepository;

    public VendorUserDetailsService(VendorUserRepository vendorUserRepository) {
        this.vendorUserRepository = vendorUserRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        VendorUser v = vendorUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Vendeur introuvable : " + username));
        if (!v.isActive()) throw new UsernameNotFoundException("Compte vendeur désactivé.");
        return new User(v.getUsername(), v.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_VENDOR")));
    }
}
