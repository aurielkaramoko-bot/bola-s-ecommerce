package com.bolas.ecommerce.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/admin/login")
    public String login(Model model) {
        model.addAttribute("pageTitle", "Connexion — Bola's");
        return "login";
    }

    @GetMapping("/admin/login-error")
    public String loginError() {
        return "redirect:/admin/login?error";
    }
}
