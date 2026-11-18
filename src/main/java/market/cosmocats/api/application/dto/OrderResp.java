package market.cosmocats.api.application.dto;

import market.cosmocats.api.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class OrderResp {
    public UUID orderNumber;
    public OrderStatus status;
    public Instant createdAt;
    public List<OrderItemResp> items;
    public BigDecimal totalAmount;
}
