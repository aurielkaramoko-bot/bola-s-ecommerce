package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.dto.ProductFilterDto;
import com.bolas.ecommerce.model.Product;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * JPA Specification builder pour les filtres de recherche produits avancés.
 * Combine dynamiquement : mot-clé, catégorie, prix, ville, pays, promo.
 *
 * Note : supporte les produits sans vendeur (BOLA direct) via LEFT JOIN.
 */
public class ProductSpecification {

    /** Construit la Specification combinée depuis le DTO de filtres */
    public static Specification<Product> of(ProductFilterDto f) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Toujours : produits disponibles uniquement
            predicates.add(cb.isTrue(root.get("available")));

            // Vendeur actif OU produit sans vendeur (BOLA direct)
            Join<?, ?> vendorJoin = root.join("vendor", JoinType.LEFT);
            predicates.add(cb.or(
                cb.isNull(root.get("vendor")),
                cb.isTrue(vendorJoin.get("active"))
            ));

            // Mot-clé (nom OU description)
            if (f.getQ() != null && !f.getQ().isBlank()) {
                String like = "%" + f.getQ().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(root.get("name")), like),
                    cb.like(cb.lower(root.get("description")), like)
                ));
            }

            // Catégorie
            if (f.getCategoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), f.getCategoryId()));
            }

            // Fourchette de prix (sur prix effectif : promo si active, sinon normal)
            if (f.getPriceMin() != null) {
                predicates.add(cb.or(
                    cb.and(cb.isNull(root.get("promoPriceCfa")),
                           cb.greaterThanOrEqualTo(root.get("priceCfa"), f.getPriceMin())),
                    cb.and(cb.isNotNull(root.get("promoPriceCfa")),
                           cb.greaterThanOrEqualTo(root.get("promoPriceCfa"), f.getPriceMin()))
                ));
            }
            if (f.getPriceMax() != null) {
                predicates.add(cb.or(
                    cb.and(cb.isNull(root.get("promoPriceCfa")),
                           cb.lessThanOrEqualTo(root.get("priceCfa"), f.getPriceMax())),
                    cb.and(cb.isNotNull(root.get("promoPriceCfa")),
                           cb.lessThanOrEqualTo(root.get("promoPriceCfa"), f.getPriceMax()))
                ));
            }

            // Ville du vendeur (recherche dans shopAddress)
            if (f.getCity() != null && !f.getCity().isBlank()) {
                predicates.add(cb.like(
                    cb.lower(vendorJoin.get("shopAddress")),
                    "%" + f.getCity().toLowerCase() + "%"
                ));
            }

            // Pays du vendeur (recherche dans shopAddress ou country)
            if (f.getCountry() != null && !f.getCountry().isBlank()) {
                predicates.add(cb.like(
                    cb.lower(vendorJoin.get("shopAddress")),
                    "%" + f.getCountry().toLowerCase() + "%"
                ));
            }

            // Promo uniquement
            if (f.isPromoOnly()) {
                predicates.add(cb.isNotNull(root.get("promoPriceCfa")));
            }

            // Déduplication (LEFT JOIN peut créer des doublons)
            query.distinct(true);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
