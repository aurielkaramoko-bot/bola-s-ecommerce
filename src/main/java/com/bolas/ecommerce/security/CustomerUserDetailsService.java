package com.bolas.ecommerce.security;

import com.bolas.ecommerce.model.Customer;
import com.bolas.ecommerce.repository.CustomerRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomerUserDetailsService implements UserDetailsService {

    private final CustomerRepository customerRepository;

    public CustomerUserDetailsService(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Customer c = customerRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new UsernameNotFoundException("Client introuvable : " + email));
        return new User(c.getEmail(), c.getPasswordHash() != null ? c.getPasswordHash() : "",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
    }
}
