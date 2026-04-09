package com.bolas.ecommerce.service;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Compte en temps réel :
 *  - Le nombre de sessions actives (visiteurs + acheteurs + vendeurs)
 *  - Le nombre de vendeurs connectés (session contenant BOLAS_VENDOR)
 *
 * Implémente HttpSessionListener pour détecter création/destruction de session.
 */
@Service
public class SessionCounterService implements HttpSessionListener {

    private static final String VENDOR_KEY = "BOLAS_VENDOR";

    /** Sessions HTTP actuellement actives */
    private final AtomicInteger activeSessions = new AtomicInteger(0);

    /** IDs de sessions des vendeurs connectés */
    private final Set<String> vendorSessions = Collections.newSetFromMap(new ConcurrentHashMap<>());

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        activeSessions.incrementAndGet();
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        activeSessions.decrementAndGet();
        vendorSessions.remove(se.getSession().getId());
    }

    /**
     * À appeler dans les contrôleurs vendeur lors du login.
     * Enregistre l'ID de session comme "vendeur connecté".
     */
    public void markVendorSession(HttpSession session) {
        if (session != null && session.getAttribute(VENDOR_KEY) != null) {
            vendorSessions.add(session.getId());
        }
    }

    /**
     * À appeler lors du logout vendeur.
     */
    public void unmarkVendorSession(HttpSession session) {
        if (session != null) {
            vendorSessions.remove(session.getId());
        }
    }

    public int getActiveVisitors() {
        return activeSessions.get();
    }

    public int getConnectedVendors() {
        return vendorSessions.size();
    }
}
