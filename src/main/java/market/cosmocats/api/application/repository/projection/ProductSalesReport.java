package market.cosmocats.api.application.repository.projection;

import java.math.BigDecimal;

public interface ProductSalesReport {

    Long getProductId();

    String getProductName();

    String getCategoryTitle();

    Long getTotalQuantity();

    BigDecimal getTotalRevenue();
}
