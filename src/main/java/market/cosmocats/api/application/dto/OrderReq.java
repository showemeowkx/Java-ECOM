package market.cosmocats.api.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class OrderReq {

    @NotEmpty(message = "Order must contain at least one item")
    public List<@Valid OrderItemReq> items;
}
