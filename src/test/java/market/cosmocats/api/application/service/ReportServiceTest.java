package market.cosmocats.api.application.service;

import market.cosmocats.api.application.dto.ProductSalesReportResp;
import market.cosmocats.api.application.exception.BadRequestException;
import market.cosmocats.api.application.repository.ProductRepository;
import market.cosmocats.api.application.repository.projection.ProductSalesReport;
import market.cosmocats.api.domain.OrderStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final Instant FROM = Instant.parse("2026-11-01T00:00:00Z");
    private static final Instant TO = Instant.parse("2026-12-01T00:00:00Z");

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    void shouldQueryOnlyPurchasedOrdersAndMapProjection() {
        when(productRepository.findTopSellingProducts(ReportService.PURCHASED_STATUSES, FROM, TO, Limit.of(5)))
                .thenReturn(List.of(row(301L, "Cosmic Milk", 6L, "273.00"), row(251L, "Star Yarn", 5L, "499.95")));

        List<ProductSalesReportResp> result = reportService.getTopSellingProducts(FROM, TO, 5);

        assertThat(result).extracting(r -> r.productId).containsExactly(301L, 251L);
        ProductSalesReportResp first = result.get(0);
        assertThat(first.productName).isEqualTo("Cosmic Milk");
        assertThat(first.categoryTitle).isEqualTo("Space Food");
        assertThat(first.totalQuantity).isEqualTo(6L);
        assertThat(first.totalRevenue).isEqualByComparingTo("273.00");
    }

    @Test
    void purchasedStatusesShouldExcludeNewAndCancelled() {
        assertThat(ReportService.PURCHASED_STATUSES)
                .containsExactlyInAnyOrder(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED);
    }

    @Test
    void shouldReturnEmptyListWhenNothingSold() {
        when(productRepository.findTopSellingProducts(any(), any(), any(), any())).thenReturn(List.of());

        assertThat(reportService.getTopSellingProducts(FROM, TO, 10)).isEmpty();
        verify(productRepository).findTopSellingProducts(ReportService.PURCHASED_STATUSES, FROM, TO, Limit.of(10));
    }

    @Test
    void shouldRejectWindowWhereFromIsNotBeforeTo() {
        assertThatThrownBy(() -> reportService.getTopSellingProducts(TO, FROM, 10))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("'from' must be before 'to'");
        assertThatThrownBy(() -> reportService.getTopSellingProducts(FROM, FROM, 10))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(productRepository);
    }

    private static ProductSalesReport row(Long id, String name, Long quantity, String revenue) {
        return new SpelAwareProxyProjectionFactory().createProjection(ProductSalesReport.class, Map.of(
                "productId", id,
                "productName", name,
                "categoryTitle", "Space Food",
                "totalQuantity", quantity,
                "totalRevenue", new BigDecimal(revenue)));
    }
}
