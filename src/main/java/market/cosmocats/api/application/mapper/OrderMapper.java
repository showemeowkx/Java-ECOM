package market.cosmocats.api.application.mapper;

import market.cosmocats.api.application.dto.OrderItemResp;
import market.cosmocats.api.application.dto.OrderResp;
import market.cosmocats.api.domain.Order;
import market.cosmocats.api.domain.OrderItem;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResp toDto(Order order) {
        OrderResp resp = new OrderResp();
        resp.orderNumber = order.getOrderNumber();
        resp.status = order.getStatus();
        resp.createdAt = order.getCreatedAt();
        resp.items = order.getItems().stream().map(this::toDto).toList();
        resp.totalAmount = order.getTotalAmount();
        return resp;
    }

    private OrderItemResp toDto(OrderItem item) {
        OrderItemResp resp = new OrderItemResp();
        resp.productId = item.getProduct().getId();
        resp.productName = item.getProduct().getName();
        resp.quantity = item.getQuantity();
        resp.unitPrice = item.getUnitPrice();
        resp.lineTotal = item.getLineTotal();
        return resp;
    }
}
