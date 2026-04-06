package com.bolas.ecommerce.service;

import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    private static final String SESSION_KEY = "BOLAS_CART";

    public record CartLine(Product product, int quantity) {
        public long lineTotalCfa() {
            return product.getEffectivePriceCfa() * (long) quantity;
        }
    }

    private final ProductRepository productRepository;

    public CartService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @SuppressWarnings("unchecked")
    private Map<Long, Integer> cartMap(HttpSession session) {
        Object obj = session.getAttribute(SESSION_KEY);
        if (obj instanceof Map<?, ?>) {
            return (Map<Long, Integer>) obj;
        }
        Map<Long, Integer> created = new LinkedHashMap<>();
        session.setAttribute(SESSION_KEY, created);
        return created;
    }

    public void add(HttpSession session, long productId, int quantity) {
        if (quantity < 1) {
            return;
        }
        Map<Long, Integer> cart = cartMap(session);
        int existing = cart.getOrDefault(productId, 0);
        cart.put(productId, existing + quantity);
    }

    public void setQuantity(HttpSession session, long productId, int quantity) {
        Map<Long, Integer> cart = cartMap(session);
        if (quantity <= 0) {
            cart.remove(productId);
        } else {
            cart.put(productId, quantity);
        }
    }

    public void remove(HttpSession session, long productId) {
        cartMap(session).remove(productId);
    }

    public void clear(HttpSession session) {
        cartMap(session).clear();
    }

    public int totalItems(HttpSession session) {
        int total = 0;
        for (int q : cartMap(session).values()) {
            total += Math.max(q, 0);
        }
        return total;
    }

    public List<CartLine> lines(HttpSession session) {
        Map<Long, Integer> cart = cartMap(session);
        List<CartLine> out = new ArrayList<>();
        for (var entry : cart.entrySet()) {
            Long productId = entry.getKey();
            Integer qty = entry.getValue();
            if (productId == null || qty == null || qty <= 0) {
                continue;
            }
            Product p = productRepository.findById(productId).orElse(null);
            if (p == null || !p.isAvailable()) {
                continue;
            }
            out.add(new CartLine(p, qty));
        }
        return out;
    }

    public long totalAmountCfa(HttpSession session) {
        long total = 0L;
        for (CartLine line : lines(session)) {
            total += line.lineTotalCfa();
        }
        return total;
    }
}
