package com.bolas.ecommerce.service;

import com.bolas.ecommerce.dto.VendorStatsDto;
import com.bolas.ecommerce.model.OrderStatus;
import com.bolas.ecommerce.model.VendorUser;
import com.bolas.ecommerce.repository.CustomerOrderRepository;
import com.bolas.ecommerce.repository.OrderLineRepository;
import com.bolas.ecommerce.repository.ProductRepository;
import com.bolas.ecommerce.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Calcule les statistiques vendeur selon son plan :
 *  - PRO_LOCAL : CA du mois, nb commandes, top 5 produits
 *  - PREMIUM   : + graphiques sur 6 mois, note moyenne, nb avis
 */
@Service
public class VendorStatsService {

    private static final ZoneId ZONE = ZoneId.of("Africa/Abidjan");

    private final CustomerOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderLineRepository orderLineRepository;
    private final ReviewRepository reviewRepository;

    public VendorStatsService(CustomerOrderRepository orderRepository,
                              ProductRepository productRepository,
                              OrderLineRepository orderLineRepository,
                              ReviewRepository reviewRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderLineRepository = orderLineRepository;
        this.reviewRepository = reviewRepository;
    }

    /**
     * Stats basiques pour PRO_LOCAL (et PREMIUM qui les inclut).
     */
    @Transactional(readOnly = true)
    public VendorStatsDto getBasicStats(VendorUser vendor) {
        VendorStatsDto dto = new VendorStatsDto();

        YearMonth thisMonth = YearMonth.now(ZONE);
        Instant startOfMonth = thisMonth.atDay(1).atStartOfDay(ZONE).toInstant();
        Instant endOfMonth = thisMonth.atEndOfMonth().atTime(23, 59, 59).atZone(ZONE).toInstant();

        // CA du mois
        dto.setMonthlyRevenue(orderRepository.sumRevenueByVendorBetween(vendor, startOfMonth, endOfMonth));

        // Nb commandes du mois
        var monthOrders = orderRepository.findByVendorOrderByCreatedAtDesc(vendor).stream()
                .filter(o -> !o.getCreatedAt().isBefore(startOfMonth) && !o.getCreatedAt().isAfter(endOfMonth))
                .count();
        dto.setMonthlyOrders(monthOrders);

        // Total commandes
        dto.setTotalOrders(orderRepository.countByVendor(vendor));

        // Total produits
        dto.setTotalProducts(productRepository.countByVendor(vendor));

        // Top 5 produits (par nb de lignes de commande)
        var products = productRepository.findByVendor(vendor);
        List<VendorStatsDto.TopProduct> topProducts = products.stream()
                .map(p -> {
                    long count = orderLineRepository.countByProduct_Id(p.getId());
                    long revenue = count * p.getEffectivePriceCfa();
                    return new VendorStatsDto.TopProduct(p.getName(), count, revenue);
                })
                .sorted((a, b) -> Long.compare(b.getOrderCount(), a.getOrderCount()))
                .limit(5)
                .collect(Collectors.toList());
        dto.setTopProducts(topProducts);

        return dto;
    }

    /**
     * Stats avancées pour PREMIUM (inclut les stats basiques).
     */
    @Transactional(readOnly = true)
    public VendorStatsDto getAdvancedStats(VendorUser vendor) {
        VendorStatsDto dto = getBasicStats(vendor);

        // Graphiques sur 6 mois
        List<String> labels = new ArrayList<>();
        List<Long> revenues = new ArrayList<>();
        List<Long> orderCounts = new ArrayList<>();

        for (int i = 5; i >= 0; i--) {
            YearMonth ym = YearMonth.now(ZONE).minusMonths(i);
            Instant start = ym.atDay(1).atStartOfDay(ZONE).toInstant();
            Instant end = ym.atEndOfMonth().atTime(23, 59, 59).atZone(ZONE).toInstant();

            labels.add(ym.getMonth().getDisplayName(TextStyle.SHORT, Locale.FRENCH) + " " + ym.getYear());
            revenues.add(orderRepository.sumRevenueByVendorBetween(vendor, start, end));

            long count = orderRepository.findByVendorOrderByCreatedAtDesc(vendor).stream()
                    .filter(o -> !o.getCreatedAt().isBefore(start) && !o.getCreatedAt().isAfter(end))
                    .count();
            orderCounts.add(count);
        }

        dto.setMonthLabels(labels);
        dto.setMonthRevenues(revenues);
        dto.setMonthOrderCounts(orderCounts);

        // Note et avis
        dto.setAverageRating(reviewRepository.averageRatingByVendor(vendor));
        dto.setReviewCount(reviewRepository.countApprovedByVendor(vendor));

        return dto;
    }
}
