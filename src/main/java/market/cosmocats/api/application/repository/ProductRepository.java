package market.cosmocats.api.application.repository;

import market.cosmocats.api.application.repository.projection.ProductSalesReport;
import market.cosmocats.api.application.repository.projection.ProductSummary;
import market.cosmocats.api.domain.OrderStatus;
import market.cosmocats.api.domain.Product;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsByCategoryId(Long categoryId);

    List<ProductSummary> findByCategoryIdOrderByPriceAscNameAsc(Long categoryId);

    @Query("""
            SELECT p.id AS productId,
                   p.name AS productName,
                   c.title AS categoryTitle,
                   SUM(oi.quantity) AS totalQuantity,
                   SUM(oi.unitPrice * oi.quantity) AS totalRevenue
            FROM OrderItem oi
            JOIN oi.order o
            JOIN oi.product p
            JOIN p.category c
            WHERE o.status IN :statuses
              AND o.createdAt >= :from
              AND o.createdAt < :to
            GROUP BY p.id, p.name, c.title
            ORDER BY SUM(oi.quantity) DESC, p.name ASC
            """)
    List<ProductSalesReport> findTopSellingProducts(@Param("statuses") Collection<OrderStatus> statuses,
                                                    @Param("from") Instant from,
                                                    @Param("to") Instant to,
                                                    Limit limit);
}
