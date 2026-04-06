package com.bolas.ecommerce.security;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCK_DURATION = Duration.ofMinutes(15);

    private final Clock clock;
    private final LoginAttemptRepository loginAttemptRepository;

    public LoginAttemptService(Clock clock, LoginAttemptRepository loginAttemptRepository) {
        this.clock = clock;
        this.loginAttemptRepository = loginAttemptRepository;
    }

    @Transactional
    public void loginSucceeded(String username) {
        if (username == null) {
            return;
        }
        loginAttemptRepository.deleteByUsername(username.toLowerCase());
    }

    @Transactional
    public void loginFailed(String username) {
        if (username == null) {
            return;
        }
        String key = username.toLowerCase();
        String ip = currentIp();
        Instant now = clock.instant();
        loginAttemptRepository.deleteByWindowEndBefore(now.minus(LOCK_DURATION.multipliedBy(2)));

        Optional<LoginAttempt> opt = loginAttemptRepository.findTopByUsernameAndIpOrderByWindowEndDesc(key, ip);
        LoginAttempt record = opt.orElseGet(LoginAttempt::new);
        record.setUsername(key);
        record.setIp(ip);

        if (opt.isEmpty() || record.getWindowEnd().isBefore(now)) {
            record.setCount(1);
            record.setWindowStart(now);
            record.setWindowEnd(now.plus(LOCK_DURATION));
        } else {
            record.setCount(record.getCount() + 1);
            if (record.getCount() >= MAX_ATTEMPTS) {
                record.setWindowEnd(now.plus(LOCK_DURATION));
            }
        }
        loginAttemptRepository.save(record);
    }

    @Transactional(readOnly = true)
    public boolean isBlocked(String username) {
        if (username == null) {
            return false;
        }
        String key = username.toLowerCase();
        String ip = currentIp();
        Instant now = clock.instant();
        Optional<LoginAttempt> opt = loginAttemptRepository.findTopByUsernameAndIpOrderByWindowEndDesc(key, ip);
        if (opt.isEmpty()) {
            return false;
        }
        LoginAttempt attempt = opt.get();
        return attempt.getCount() >= MAX_ATTEMPTS && attempt.getWindowEnd().isAfter(now);
    }

    private String currentIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return "unknown";
            }
            String xff = attrs.getRequest().getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            return attrs.getRequest().getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
