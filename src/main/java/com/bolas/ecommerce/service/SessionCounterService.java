package com.bolas.ecommerce.service;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Compte les visiteurs réels actifs (page vue dans les 5 dernières minutes)
 * et les vendeurs connectés.
 */
@Service
public class SessionCounterService implements HttpSessionListener {

    private static final String VENDOR_KEY = "BOLAS_VENDOR";
    private static final long ACTIVE_WINDOW_MS = 5 * 60 * 1000L; // 5 minutes

    /** sessionId → dernière activité */
    private final Map<String, Instant> recentActivity = new ConcurrentHashMap<>();

    /** IDs de sessions des vendeurs connectés */
    private final Set<String> vendorSessions = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public void sessionCreated(HttpSessionEvent se) {}

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        recentActivity.remove(se.getSession().getId());
        vendorSessions.remove(se.getSession().getId());
    }

    /** À appeler depuis un filtre ou intercepteur à chaque requête de page */
    public void recordActivity(String sessionId) {
        recentActivity.put(sessionId, Instant.now());
    }

    public void markVendorSession(HttpSession session) {
        if (session != null && session.getAttribute(VENDOR_KEY) != null) {
            vendorSessions.add(session.getId());
        }
    }

    public void unmarkVendorSession(HttpSession session) {
        if (session != null) {
            vendorSessions.remove(session.getId());
        }
    }

    /** Visiteurs ayant eu une activité dans les 5 dernières minutes */
    public int getActiveVisitors() {
        Instant cutoff = Instant.now().minusMillis(ACTIVE_WINDOW_MS);
        return (int) recentActivity.values().stream()
                .filter(t -> t.isAfter(cutoff)).count();
    }

    public int getConnectedVendors() {
        return vendorSessions.size();
    }
}
