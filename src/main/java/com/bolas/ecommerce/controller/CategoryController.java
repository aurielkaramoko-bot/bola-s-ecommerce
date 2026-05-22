package com.bolas.ecommerce.controller;

import com.bolas.ecommerce.model.Category;
import com.bolas.ecommerce.model.Product;
import com.bolas.ecommerce.repository.CategoryRepository;
import com.bolas.ecommerce.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class CategoryController {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryController(CategoryRepository categoryRepository,
                               ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/categories")
    @Transactional(readOnly = true)
    public String categories(
            @RequestParam(required = false) Long familyId,
            @RequestParam(required = false) Long subId,
            Model model) {

        model.addAttribute("pageTitle", "Categories - BOLA");

        List<Category> families = categoryRepository.findByParentIdIsNullAndActiveTrueOrderByNameAsc();
        model.addAttribute("families", families);

        Category selectedFamily = null;
        if (familyId != null) {
            selectedFamily = families.stream().filter(f -> f.getId().equals(familyId)).findFirst().orElse(null);
        }
        if (selectedFamily == null && !families.isEmpty()) selectedFamily = families.get(0);
        model.addAttribute("selectedFamily", selectedFamily);

        List<Category> subCategories = List.of();
        if (selectedFamily != null) {
            subCategories = categoryRepository.findByParentIdAndActiveTrueOrderByNameAsc(selectedFamily.getId());
        }
        model.addAttribute("subCategories", subCategories);

        Category selectedSub = null;
        if (subId != null) {
            final Long fSubId = subId;
            selectedSub = subCategories.stream().filter(s -> s.getId().equals(fSubId)).findFirst().orElse(null);
        }
        if (selectedSub == null && !subCategories.isEmpty()) selectedSub = subCategories.get(0);
        model.addAttribute("selectedSub", selectedSub);

        List<Category> leaves = List.of();
        if (selectedSub != null) {
            leaves = categoryRepository.findByParentIdAndActiveTrueOrderByNameAsc(selectedSub.getId());
        }
        model.addAttribute("leaves", leaves);

        List<Product> products = List.of();
        if (selectedSub != null) {
            final Category finalSub = selectedSub;
            final List<Category> finalLeaves = leaves;
            products = productRepository.findAll().stream()
                    .filter(p -> p.isAvailable()
                            && (p.getVendor() == null || p.getVendor().isActive())
                            && p.getCategory() != null
                            && (p.getCategory().getId().equals(finalSub.getId())
                                || finalLeaves.stream().anyMatch(l -> l.getId().equals(p.getCategory().getId()))))
                    .limit(24)
                    .toList();
        }
        model.addAttribute("products", products);

        return "categories";
    }
}
