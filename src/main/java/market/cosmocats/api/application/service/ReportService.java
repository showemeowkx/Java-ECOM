package market.cosmocats.api.application.service;

import market.cosmocats.api.application.dto.ProductSalesReportResp;
import market.cosmocats.api.application.exception.BadRequestException;
import market.cosmocats.api.application.repository.ProductRepository;
import market.cosmocats.api.application.repository.projection.ProductSalesReport;
import market.cosmocats.api.domain.OrderStatus;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ReportService {

    static final Set<OrderStatus> PURCHASED_STATUSES =
            EnumSet.of(OrderStatus.PAID, OrderStatus.SHIPPED, OrderStatus.DELIVERED);

    private final ProductRepository productRepository;

    public ReportService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductSalesReportResp> getTopSellingProducts(Instant from, Instant to, int limit) {
        if (!from.isBefore(to)) {
            throw new BadRequestException("'from' must be before 'to'");
        }
        return productRepository.findTopSellingProducts(PURCHASED_STATUSES, from, to, Limit.of(limit)).stream()
                .map(ReportService::toDto)
                .toList();
    }

    private static ProductSalesReportResp toDto(ProductSalesReport row) {
        ProductSalesReportResp resp = new ProductSalesReportResp();
        resp.productId = row.getProductId();
        resp.productName = row.getProductName();
        resp.categoryTitle = row.getCategoryTitle();
        resp.totalQuantity = row.getTotalQuantity();
        resp.totalRevenue = row.getTotalRevenue();
        return resp;
    }
}
