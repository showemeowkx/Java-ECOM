package market.cosmocats.api.domain;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

public class Order {
    private UUID orderId;
    private List<Product> productsList;
    private BigDecimal totalAmount;

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public List<Product> getProductsList() { return productsList; }
    public void setProductsList(List<Product> productsList) { this.productsList = productsList; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
