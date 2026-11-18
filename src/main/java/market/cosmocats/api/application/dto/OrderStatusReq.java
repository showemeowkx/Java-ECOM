package market.cosmocats.api.application.dto;

import jakarta.validation.constraints.NotNull;
import market.cosmocats.api.domain.OrderStatus;

public class OrderStatusReq {

    @NotNull(message = "Status is required")
    public OrderStatus status;
}
