package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.model.Customer;
import com.bolas.ecommerce.service.CustomerService;
import jakarta.servlet.http.HttpSession;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.bolas.ecommerce.service.CartService;

import java.time.LocalDate;
import java.util.Optional;

@Controller
public class CustomerAuthController {

    private final CustomerService customerService;

    private final CartService cartService; 

    
    public CustomerAuthController(CustomerService customerService, CartService cartService) {
        this.customerService = customerService;
        this.cartService = cartService;
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
}
