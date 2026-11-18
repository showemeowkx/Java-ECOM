package market.cosmocats.api.application.repository;

import market.cosmocats.api.application.repository.projection.ProductSalesReport;
import market.cosmocats.api.domain.Category;
import market.cosmocats.api.domain.OrderStatus;
import market.cosmocats.api.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Limit;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class ProductSalesReportIT extends AbstractRepositoryIT {

    @Autowired
    private ProductRepository productRepository;


    private final Set<OrderStatus> purchased = EnumSet.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED);
    private final Instant from = Instant.parse("2026-11-01T00:00:00Z");
    private final Instant to = Instant.parse("2026-12-01T00:00:00Z");
    private final Instant inWindow = Instant.parse("2026-11-15T10:00:00Z");

    private Product milk;
    private Product yarn;
    private Product ball;

    @BeforeEach
    void setUp() {
        Category food = category("Space Food");
        Category toys = category("Space Toys");
        milk = product("Star Milk", "45.50", food);
        yarn = product("Comet Yarn", "99.99", toys);
        ball = product("Astro Ball", "5.00", toys);

        order(OrderStatus.PAID, inWindow, milk, 2, yarn, 5);
        order(OrderStatus.SHIPPED, inWindow, milk, 4, ball, 1);
        order(OrderStatus.DELIVERED, inWindow, ball, 4);
        order(OrderStatus.NEW, inWindow, ball, 50);
        order(OrderStatus.CANCELLED, inWindow, yarn, 100);
        order(OrderStatus.PAID, Instant.parse("2026-10-31T23:59:59Z"), milk, 70);
        order(OrderStatus.PAID, to, yarn, 80);
        flushAndClear();
    }

    @Test
    void shouldAggregatePurchasedItemsInWindowOrderedByQuantity() {
        List<ProductSalesReport> report = productRepository.findTopSellingProducts(purchased, from, to, Limit.of(10));

        assertThat(report)
                .extracting(ProductSalesReport::getProductName,
                        ProductSalesReport::getCategoryTitle,
                        ProductSalesReport::getTotalQuantity)
                .containsExactly(
                        tuple("Star Milk", "Space Food", 6L),
                        tuple("Astro Ball", "Space Toys", 5L),
                        tuple("Comet Yarn", "Space Toys", 5L));
        assertThat(report.get(0).getProductId()).isEqualTo(milk.getId());
        assertThat(report.get(0).getTotalRevenue()).isEqualByComparingTo("273.00");
        assertThat(report.get(2).getTotalRevenue()).isEqualByComparingTo("499.95");
    }

    @Test
    void shouldBreakQuantityTiesByProductName() {
        List<ProductSalesReport> report = productRepository.findTopSellingProducts(purchased, from, to, Limit.of(10));

        assertThat(report.get(1).getProductName()).isEqualTo("Astro Ball");
        assertThat(report.get(2).getProductName()).isEqualTo("Comet Yarn");
    }

    @Test
    void shouldApplyLimit() {
        List<ProductSalesReport> report = productRepository.findTopSellingProducts(purchased, from, to, Limit.of(1));

        assertThat(report).extracting(ProductSalesReport::getProductName).containsExactly("Star Milk");
    }

    @Test
    void shouldRespectStatusFilter() {
        List<ProductSalesReport> report = productRepository.findTopSellingProducts(
                EnumSet.of(OrderStatus.NEW), from, to, Limit.of(10));

        assertThat(report)
                .extracting(ProductSalesReport::getProductName, ProductSalesReport::getTotalQuantity)
                .containsExactly(tuple("Astro Ball", 50L));
    }

    @Test
    void shouldIncludeFromAndExcludeTo() {
        List<ProductSalesReport> report = productRepository.findTopSellingProducts(
                purchased, Instant.parse("2026-10-31T23:59:59Z"), Instant.parse("2026-11-01T00:00:00Z"), Limit.of(10));

        assertThat(report)
                .extracting(ProductSalesReport::getProductName, ProductSalesReport::getTotalQuantity)
                .containsExactly(tuple("Star Milk", 70L));
    }

    @Test
    void shouldReturnEmptyWhenNothingSoldInWindow() {
        assertThat(productRepository.findTopSellingProducts(
                purchased, Instant.parse("2020-01-01T00:00:00Z"), Instant.parse("2021-01-01T00:00:00Z"), Limit.of(10)))
                .isEmpty();
    }
}
