package com.bolas.ecommerce.repository;

import com.bolas.ecommerce.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** Recherche par slug unique */
    Optional<Category> findBySlug(String slug);

    /** Recherche par nom exact (pour idempotence dans DataInitializer) */
    Optional<Category> findByName(String name);

    /** Toutes les catégories racines (grandes familles, niveau 1) */
    List<Category> findByParentIdIsNullAndActiveTrueOrderByNameAsc();

    /** Toutes les catégories racines actives ou non (pour admin) */
    List<Category> findByParentIdIsNullOrderByNameAsc();

    /** Sous-catégories d'une catégorie parente */
    List<Category> findByParentIdAndActiveTrueOrderByNameAsc(Long parentId);

    /** Toutes les feuilles (niveau 3) actives — pour le formulaire ajout produit */
    @Query("SELECT c FROM Category c WHERE c.parentId IS NOT NULL AND c.active = true ORDER BY c.name")
    List<Category> findAllActiveLeaves();

    /** Toutes les catégories d'un arbre (pour la page /categories) */
    List<Category> findAllByOrderByParentIdAscNameAsc();

    /** Supprimer toutes les catégories (pour remplacement lors de l'init) */
    @Modifying
    @Query("DELETE FROM Category c")
    void deleteAllCategories();

    /** Existe déjà une catégorie avec ce slug ? */
    boolean existsBySlug(String slug);

    /** Nombre de catégories racines */
    long countByParentIdIsNull();
}
