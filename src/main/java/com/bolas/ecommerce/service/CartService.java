package com.bolas.ecommerce.service;

import com.bolas.ecommerce.model.CartItem;
import com.bolas.ecommerce.model.Customer;
import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.repository.CartItemRepository;
import com.bolas.ecommerce.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CartService {

    private static final String SESSION_KEY = "BOLAS_CART";
    private static final String CUSTOMER_KEY = "BOLAS_CUSTOMER";

    public record CartLine(Product product, int quantity) {
        public long lineTotalCfa() {
            return product.getEffectivePriceCfa() * (long) quantity;
        }
    }

    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;

    public CartService(ProductRepository productRepository, CartItemRepository cartItemRepository) {
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
    }

    // --- LOGIQUE D'AJOUT ---
    @Transactional
    public void add(HttpSession session, long productId, int quantity) {
        if (quantity < 1) return;

        Customer customer = (Customer) session.getAttribute(CUSTOMER_KEY);
        
        if (customer != null) {
            // MODE BDD : Client connecté
            Product product = productRepository.findById(productId).orElse(null);
            if (product != null) {
                CartItem item = cartItemRepository.findByCustomerAndProductId(customer, productId)
                        .orElse(new CartItem(customer, product, 0));
                item.setQuantity(item.getQuantity() + quantity);
                cartItemRepository.save(item);
            }
        } else {
            // MODE SESSION : Client anonyme
            Map<Long, Integer> cart = cartMap(session);
            cart.put(productId, cart.getOrDefault(productId, 0) + quantity);
        }
    }

    // --- RÉCUPÉRATION DES LIGNES ---
    public List<CartLine> lines(HttpSession session) {
        Customer customer = (Customer) session.getAttribute(CUSTOMER_KEY);
        List<CartLine> out = new ArrayList<>();

        if (customer != null) {
            // Récupère depuis la BDD
            List<CartItem> items = cartItemRepository.findByCustomer(customer);
            for (CartItem item : items) {
                if (item.getProduct().isAvailable()) {
                    out.add(new CartLine(item.getProduct(), item.getQuantity()));
                }
            }
        } else {
            // Récupère depuis la Session
            Map<Long, Integer> cart = cartMap(session);
            for (var entry : cart.entrySet()) {
                productRepository.findById(entry.getKey()).ifPresent(p -> {
                    if (p.isAvailable()) out.add(new CartLine(p, entry.getValue()));
                });
            }
        }
        return out;
    }

    // --- SUPPRESSION / NETTOYAGE ---
    @Transactional
    public void remove(HttpSession session, long productId) {
        Customer customer = (Customer) session.getAttribute(CUSTOMER_KEY);
        if (customer != null) {
            cartItemRepository.findByCustomerAndProductId(customer, productId)
                .ifPresent(cartItemRepository::delete);
        } else {
            cartMap(session).remove(productId);
        }
    }

    @Transactional
    public void clear(HttpSession session) {
        Customer customer = (Customer) session.getAttribute(CUSTOMER_KEY);
        if (customer != null) {
            cartItemRepository.deleteByCustomer(customer);
        } else {
            cartMap(session).clear();
        }
    }

    // --- MÉTHODES UTILITAIRES ---
    @SuppressWarnings("unchecked")
    private Map<Long, Integer> cartMap(HttpSession session) {
        Object obj = session.getAttribute(SESSION_KEY);
        if (obj instanceof Map<?, ?>) return (Map<Long, Integer>) obj;
        Map<Long, Integer> created = new LinkedHashMap<>();
        session.setAttribute(SESSION_KEY, created);
        return created;
    }

    public long totalAmountCfa(HttpSession session) {
        return lines(session).stream().mapToLong(CartLine::lineTotalCfa).sum();
    }

    public int totalItems(HttpSession session) {
        return lines(session).stream().mapToInt(CartLine::quantity).sum();
    }

    @Transactional
    public void setQuantity(HttpSession session, long productId, int quantity) {
        Customer customer = (Customer) session.getAttribute(CUSTOMER_KEY);
        if (customer != null) {
            if (quantity <= 0) {
                remove(session, productId);
            } else {
                cartItemRepository.findByCustomerAndProductId(customer, productId).ifPresent(item -> {
                    item.setQuantity(quantity);
                    cartItemRepository.save(item);
                });
            }
        } else {
            if (quantity <= 0) cartMap(session).remove(productId);
            else cartMap(session).put(productId, quantity);
        }
    }
    @Transactional
public void mergeCart(HttpSession session, Customer customer) {
    // 1. Récupérer le panier temporaire de la session
    Map<Long, Integer> sessionCart = cartMap(session);
    
    if (sessionCart.isEmpty()) return;

    // 2. Transférer chaque produit vers la base de données
    for (var entry : sessionCart.entrySet()) {
        long productId = entry.getKey();
        int quantity = entry.getValue();
        
        productRepository.findById(productId).ifPresent(product -> {
            CartItem item = cartItemRepository.findByCustomerAndProductId(customer, productId)
                    .orElse(new CartItem(customer, product, 0));
            
            // On ajoute la quantité de la session à celle déjà en BDD (si elle existe)
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        });
    }

    // 3. Vider la session après la fusion
    sessionCart.clear();
}
}