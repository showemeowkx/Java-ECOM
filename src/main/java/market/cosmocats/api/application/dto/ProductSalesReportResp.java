package market.cosmocats.api.application.dto;

import java.math.BigDecimal;

public class ProductSalesReportResp {
    public Long productId;
    public String productName;
    public String categoryTitle;
    public long totalQuantity;
    public BigDecimal totalRevenue;
}
