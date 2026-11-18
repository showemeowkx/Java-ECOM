package market.cosmocats.api.application.dto;

import java.math.BigDecimal;

public class OrderItemResp {
    public Long productId;
    public String productName;
    public int quantity;
    public BigDecimal unitPrice;
    public BigDecimal lineTotal;
}
