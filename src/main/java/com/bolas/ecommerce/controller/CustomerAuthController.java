package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.model.Customer;
import com.bolas.ecommerce.model.OrderStatus;
import com.bolas.ecommerce.repository.CustomerOrderRepository;
import com.bolas.ecommerce.repository.ProductRepository;
import com.bolas.ecommerce.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.bolas.ecommerce.service.CartService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
public class CustomerAuthController {

    private final CustomerService customerService;
    private final CartService cartService;
    private final CustomerOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    public CustomerAuthController(CustomerService customerService,
                                   CartService cartService,
                                   CustomerOrderRepository orderRepository,
                                   ProductRepository productRepository,
                                   PasswordEncoder passwordEncoder) {
        this.customerService = customerService;
        this.cartService = cartService;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/customer/login")
    public String loginPage(HttpSession session, Model model) {
        if (session.getAttribute("BOLAS_CUSTOMER") != null) return "redirect:/";
        model.addAttribute("pageTitle", "Connexion — BOLA");
        return "customer/login";
    }

    @PostMapping("/customer/login")
    public String loginSubmit(@RequestParam String email,
                              @RequestParam String password,
                              HttpSession session,
                              RedirectAttributes ra) {
        Optional<Customer> customer = customerService.login(email, password);
        if (customer.isEmpty()) {
            ra.addFlashAttribute("flashError", "Email ou mot de passe incorrect.");
            return "redirect:/customer/login";
        }
        session.setAttribute("BOLAS_CUSTOMER", customer.get());
        return "redirect:/";
    }

    @GetMapping("/customer/signup")
    public String signupPage(HttpSession session, Model model) {
        if (session.getAttribute("BOLAS_CUSTOMER") != null) return "redirect:/";
        model.addAttribute("pageTitle", "Créer un compte — BOLA");
        return "customer/signup";
    }

    @PostMapping("/customer/signup")
    public String signupSubmit(@RequestParam String firstName,
                               @RequestParam String lastName,
                               @RequestParam String email,
                               @RequestParam String password,
                               @RequestParam(required = false) String phone,
                               @RequestParam(required = false)
                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate birthDate,
                               HttpSession session,
                               RedirectAttributes ra) {
        if (password == null || password.length() < 6) {
            ra.addFlashAttribute("flashError", "Le mot de passe doit faire au moins 6 caractères.");
            return "redirect:/customer/signup";
        }
        try {
            Customer customer = customerService.register(firstName, lastName, email, password, phone, birthDate);
            session.setAttribute("BOLAS_CUSTOMER", customer);
            return "redirect:/";
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("flashError", e.getMessage());
            return "redirect:/customer/signup";
        }
    }

    @PostMapping("/customer/logout")
    public String logout(HttpSession session) {
        session.removeAttribute("BOLAS_CUSTOMER");
        return "redirect:/";
    }

    /** Appelé après succès OAuth2 Google — stocke le customer en session */
    @GetMapping("/customer/oauth2/success")
    public String oauth2Success(org.springframework.security.core.Authentication authentication,
                                HttpSession session) {
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidcUser) {
            String googleId  = oidcUser.getAttribute("sub");
            String email     = oidcUser.getAttribute("email");
            String firstName = oidcUser.getAttribute("given_name");
            String lastName  = oidcUser.getAttribute("family_name");
            com.bolas.ecommerce.model.Customer customer =
                    customerService.loginOrCreateGoogle(googleId, email, firstName, lastName);
            session.setAttribute("BOLAS_CUSTOMER", customer);
        }
        return "redirect:/";
    }

    // ─── Espace client /client/me ─────────────────────────────────────────────

    @GetMapping("/client/me")
    @Transactional(readOnly = true)
    public String myProfile(HttpSession session, Model model) {
        Customer customer = (Customer) session.getAttribute("BOLAS_CUSTOMER");
        if (customer == null) return "redirect:/customer/login";

        model.addAttribute("pageTitle", "Mon espace — BOLA");
        model.addAttribute("customer", customer);

        // Commandes en cours (par téléphone du client)
        if (customer.getPhone() != null && !customer.getPhone().isBlank()) {
            var activeOrders = orderRepository.findAll().stream()
                    .filter(o -> customer.getPhone().equals(o.getCustomerPhone())
                            && o.getStatus() != OrderStatus.DELIVERED
                            && o.getStatus() != OrderStatus.CANCELLED)
                    .limit(10)
                    .toList();
            model.addAttribute("activeOrders", activeOrders);

            var deliveredOrders = orderRepository.findAll().stream()
                    .filter(o -> customer.getPhone().equals(o.getCustomerPhone())
                            && o.getStatus() == OrderStatus.DELIVERED)
                    .limit(10)
                    .toList();
            model.addAttribute("deliveredOrders", deliveredOrders);
        } else {
            model.addAttribute("activeOrders", List.of());
            model.addAttribute("deliveredOrders", List.of());
        }

        // Produits en tendance
        var trendProducts = productRepository.findAll().stream()
                .filter(p -> p.isAvailable() && p.isCurrentlyTrending())
                .limit(6)
                .toList();
        model.addAttribute("trendProducts", trendProducts);
        model.addAttribute("unreadMessages", 0L); // placeholder

        return "client/me";
    }

    @PostMapping("/client/me/update")
    @Transactional
    public String updateProfile(@RequestParam String firstName,
                                 @RequestParam String lastName,
                                 @RequestParam(required = false) String phone,
                                 @RequestParam(required = false) String displayName,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        Customer customer = (Customer) session.getAttribute("BOLAS_CUSTOMER");
        if (customer == null) return "redirect:/customer/login";

        customer = customerService.findById(customer.getId()).orElse(customer);
        if (firstName != null && !firstName.isBlank()) customer.setFirstName(firstName.trim());
        if (lastName != null && !lastName.isBlank()) customer.setLastName(lastName.trim());
        if (phone != null) customer.setPhone(phone.trim().isEmpty() ? null : phone.trim());
        if (displayName != null && !displayName.isBlank()) {
            String dn = displayName.trim();
            if (dn.length() >= 3 && dn.length() <= 60 && dn.matches("[\\p{L}\\s]+")) {
                customer.setDisplayName(dn);
            }
        }
        customerService.save(customer);
        session.setAttribute("BOLAS_CUSTOMER", customer);

        ra.addFlashAttribute("flashOk", "Profil mis à jour avec succès !");
        return "redirect:/client/me";
    }

    @PostMapping("/client/me/change-password")
    @Transactional
    public String changePassword(@RequestParam String newPassword,
                                  HttpSession session,
                                  RedirectAttributes ra) {
        Customer customer = (Customer) session.getAttribute("BOLAS_CUSTOMER");
        if (customer == null) return "redirect:/customer/login";
        if (newPassword == null || newPassword.length() < 8) {
            ra.addFlashAttribute("flashError", "Le mot de passe doit faire au moins 8 caractères.");
            return "redirect:/client/me";
        }
        customer = customerService.findById(customer.getId()).orElse(customer);
        customer.setPasswordHash(passwordEncoder.encode(newPassword));
        customerService.save(customer);
        ra.addFlashAttribute("flashOk", "Mot de passe modifié !");
        return "redirect:/client/me";
    }

    @PostMapping("/client/me/delete")
    @Transactional
    public String deleteAccount(HttpSession session, RedirectAttributes ra) {
        Customer customer = (Customer) session.getAttribute("BOLAS_CUSTOMER");
        if (customer == null) return "redirect:/customer/login";
        customerService.deleteById(customer.getId());
        session.removeAttribute("BOLAS_CUSTOMER");
        ra.addFlashAttribute("flashOk", "Votre compte a été supprimé.");
        return "redirect:/";
    }
}
