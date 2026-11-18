package market.cosmocats.api.presentation;

import jakarta.validation.Valid;
import market.cosmocats.api.application.dto.OrderReq;
import market.cosmocats.api.application.dto.OrderResp;
import market.cosmocats.api.application.dto.OrderStatusReq;
import market.cosmocats.api.application.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderResp>> getOrders() {
        return ResponseEntity.ok(orderService.getAllOrders());
    }

    @PostMapping
    public ResponseEntity<OrderResp> createOrder(@Valid @RequestBody OrderReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(req));
    }

    @GetMapping("/{orderNumber}")
    public ResponseEntity<OrderResp> getOrder(@PathVariable UUID orderNumber) {
        return ResponseEntity.ok(orderService.getOrder(orderNumber));
    }

    @PatchMapping("/{orderNumber}/status")
    public ResponseEntity<OrderResp> updateStatus(@PathVariable UUID orderNumber,
                                                  @Valid @RequestBody OrderStatusReq req) {
        return ResponseEntity.ok(orderService.updateStatus(orderNumber, req.status));
    }

    @DeleteMapping("/{orderNumber}")
    public ResponseEntity<Void> deleteOrder(@PathVariable UUID orderNumber) {
        orderService.deleteOrder(orderNumber);
        return ResponseEntity.noContent().build();
    }
}
