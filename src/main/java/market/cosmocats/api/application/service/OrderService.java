package market.cosmocats.api.application.service;

import market.cosmocats.api.application.dto.OrderItemReq;
import market.cosmocats.api.application.dto.OrderReq;
import market.cosmocats.api.application.dto.OrderResp;
import market.cosmocats.api.application.exception.ConflictException;
import market.cosmocats.api.application.exception.ResourceNotFoundException;
import market.cosmocats.api.application.mapper.OrderMapper;
import market.cosmocats.api.application.repository.OrderRepository;
import market.cosmocats.api.application.repository.ProductRepository;
import market.cosmocats.api.domain.Order;
import market.cosmocats.api.domain.OrderItem;
import market.cosmocats.api.domain.OrderStatus;
import market.cosmocats.api.domain.Product;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final Set<OrderStatus> FINAL_STATUSES = EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED);

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderMapper orderMapper;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository,
                        OrderMapper orderMapper) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.orderMapper = orderMapper;
    }

    @Transactional
    public OrderResp createOrder(OrderReq req) {
        Map<Long, Product> products = findProducts(req.items);

        Order order = new Order();
        for (OrderItemReq itemReq : req.items) {
            Product product = products.get(itemReq.productId);
            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(itemReq.quantity);
            item.setUnitPrice(product.getPrice());
            order.addItem(item);
        }

        return orderMapper.toDto(orderRepository.saveAndFlush(order));
    }

    public List<OrderResp> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(orderMapper::toDto)
                .toList();
    }

    public OrderResp getOrder(UUID orderNumber) {
        return orderMapper.toDto(findOrder(orderNumber));
    }

    @Transactional
    public OrderResp updateStatus(UUID orderNumber, OrderStatus status) {
        Order order = findOrder(orderNumber);
        if (FINAL_STATUSES.contains(order.getStatus())) {
            throw new ConflictException("Order " + orderNumber + " is already " + order.getStatus()
                    + " and cannot change status");
        }
        order.setStatus(status);
        return orderMapper.toDto(orderRepository.saveAndFlush(order));
    }

    @Transactional
    public void deleteOrder(UUID orderNumber) {
        orderRepository.delete(findOrder(orderNumber));
        orderRepository.flush();
    }

    private Map<Long, Product> findProducts(List<OrderItemReq> items) {
        Set<Long> ids = items.stream().map(i -> i.productId).collect(Collectors.toSet());
        Map<Long, Product> found = productRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        List<Long> missing = ids.stream().filter(id -> !found.containsKey(id)).sorted().toList();
        if (!missing.isEmpty()) {
            throw new ResourceNotFoundException("Products not found: " + missing);
        }
        return found;
    }

    private Order findOrder(UUID orderNumber) {
        return orderRepository.findByNaturalId(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderNumber + " not found"));
    }
}
