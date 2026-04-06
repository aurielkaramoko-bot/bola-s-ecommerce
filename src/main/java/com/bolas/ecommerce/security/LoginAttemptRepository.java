package com.bolas.ecommerce.security;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    Optional<LoginAttempt> findTopByUsernameAndIpOrderByWindowEndDesc(String username, String ip);

    void deleteByWindowEndBefore(Instant now);

    void deleteByUsername(String username);
}

