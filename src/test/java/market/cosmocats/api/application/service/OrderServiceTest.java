package market.cosmocats.api.application.service;

import market.cosmocats.api.application.dto.OrderItemReq;
import market.cosmocats.api.application.dto.OrderReq;
import market.cosmocats.api.application.dto.OrderResp;
import market.cosmocats.api.application.exception.ConflictException;
import market.cosmocats.api.application.exception.ResourceNotFoundException;
import market.cosmocats.api.application.mapper.OrderMapper;
import market.cosmocats.api.application.repository.OrderRepository;
import market.cosmocats.api.application.repository.ProductRepository;
import market.cosmocats.api.domain.Category;
import market.cosmocats.api.domain.Order;
import market.cosmocats.api.domain.OrderItem;
import market.cosmocats.api.domain.OrderStatus;
import market.cosmocats.api.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final UUID ORDER_NUMBER = UUID.randomUUID();

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Spy
    private OrderMapper orderMapper = new OrderMapper();

    @InjectMocks
    private OrderService orderService;

    private Product milk;
    private Product toy;

    @BeforeEach
    void setUp() {
        Category category = new Category("Space Food");
        category.setId(10L);
        milk = buildProduct(1L, "Cosmic Milk", "45.50", category);
        toy = buildProduct(2L, "Star Toy", "10.00", category);
    }

    @Nested
    @DisplayName("createOrder")
    class CreateOrder {

        @Test
        void shouldCreateItemsWithCurrentPricesAndReturnTotal() {
            when(productRepository.findAllById(anyCollection())).thenReturn(List.of(milk, toy));
            when(orderRepository.saveAndFlush(any(Order.class))).thenAnswer(inv -> {
                Order o = inv.getArgument(0);
                o.setOrderNumber(ORDER_NUMBER);
                o.setStatus(OrderStatus.NEW);
                return o;
            });

            OrderResp resp = orderService.createOrder(req(item(1L, 2), item(2L, 3)));

            ArgumentCaptor<Order> captor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).saveAndFlush(captor.capture());
            Order saved = captor.getValue();
            assertThat(saved.getItems()).hasSize(2);
            assertThat(saved.getItems()).allSatisfy(i -> assertThat(i.getOrder()).isSameAs(saved));
            assertThat(saved.getItems().get(0).getUnitPrice()).isEqualByComparingTo("45.50");

            assertThat(resp.orderNumber).isEqualTo(ORDER_NUMBER);
            assertThat(resp.status).isEqualTo(OrderStatus.NEW);
            assertThat(resp.items).extracting(i -> i.productName).containsExactly("Cosmic Milk", "Star Toy");
            assertThat(resp.items.get(0).lineTotal).isEqualByComparingTo("91.00");
            assertThat(resp.totalAmount).isEqualByComparingTo("121.00");
        }

        @Test
        void shouldThrowWithAllMissingIdsAndNotSave() {
            when(productRepository.findAllById(anyCollection())).thenReturn(List.of(milk));

            assertThatThrownBy(() -> orderService.createOrder(req(item(1L, 1), item(99L, 1), item(98L, 1))))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Products not found: [98, 99]");
            verify(orderRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("getAllOrders / getOrder")
    class ReadOrders {

        @Test
        void shouldReturnAllOrdersNewestFirst() {
            Order older = buildOrder(UUID.randomUUID(), OrderStatus.PAID);
            Order newer = buildOrder(ORDER_NUMBER, OrderStatus.NEW);
            when(orderRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(newer, older));

            List<OrderResp> result = orderService.getAllOrders();

            assertThat(result).extracting(r -> r.orderNumber).containsExactly(ORDER_NUMBER, older.getOrderNumber());
        }

        @Test
        void shouldFindOrderByNaturalId() {
            when(orderRepository.findByNaturalId(ORDER_NUMBER)).thenReturn(Optional.of(buildOrder(ORDER_NUMBER, OrderStatus.NEW)));

            OrderResp resp = orderService.getOrder(ORDER_NUMBER);

            assertThat(resp.orderNumber).isEqualTo(ORDER_NUMBER);
            assertThat(resp.items).hasSize(1);
            assertThat(resp.totalAmount).isEqualByComparingTo("45.50");
        }

        @Test
        void shouldThrowWhenOrderNotFound() {
            when(orderRepository.findByNaturalId(ORDER_NUMBER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.getOrder(ORDER_NUMBER))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Order " + ORDER_NUMBER + " not found");
        }
    }

    @Nested
    @DisplayName("updateStatus")
    class UpdateStatus {

        @Test
        void shouldChangeStatusOfActiveOrder() {
            Order order = buildOrder(ORDER_NUMBER, OrderStatus.NEW);
            when(orderRepository.findByNaturalId(ORDER_NUMBER)).thenReturn(Optional.of(order));
            when(orderRepository.saveAndFlush(order)).thenReturn(order);

            OrderResp resp = orderService.updateStatus(ORDER_NUMBER, OrderStatus.PAID);

            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
            assertThat(resp.status).isEqualTo(OrderStatus.PAID);
        }

        @ParameterizedTest
        @EnumSource(value = OrderStatus.class, names = {"DELIVERED", "CANCELLED"})
        void shouldRejectChangeOfFinishedOrder(OrderStatus finalStatus) {
            Order order = buildOrder(ORDER_NUMBER, finalStatus);
            when(orderRepository.findByNaturalId(ORDER_NUMBER)).thenReturn(Optional.of(order));

            assertThatThrownBy(() -> orderService.updateStatus(ORDER_NUMBER, OrderStatus.PAID))
                    .isInstanceOf(ConflictException.class)
                    .hasMessageContaining("is already " + finalStatus);
            assertThat(order.getStatus()).isEqualTo(finalStatus);
            verify(orderRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("deleteOrder")
    class DeleteOrder {

        @Test
        void shouldDeleteAndFlush() {
            Order order = buildOrder(ORDER_NUMBER, OrderStatus.NEW);
            when(orderRepository.findByNaturalId(ORDER_NUMBER)).thenReturn(Optional.of(order));

            orderService.deleteOrder(ORDER_NUMBER);

            verify(orderRepository).delete(order);
            verify(orderRepository).flush();
        }

        @Test
        void shouldThrowAndNotDeleteWhenMissing() {
            when(orderRepository.findByNaturalId(ORDER_NUMBER)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> orderService.deleteOrder(ORDER_NUMBER))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(orderRepository, never()).delete(any());
        }
    }

    private static OrderReq req(OrderItemReq... items) {
        OrderReq req = new OrderReq();
        req.items = List.of(items);
        return req;
    }

    private static OrderItemReq item(Long productId, int quantity) {
        OrderItemReq item = new OrderItemReq();
        item.productId = productId;
        item.quantity = quantity;
        return item;
    }

    private static Product buildProduct(Long id, String name, String price, Category category) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.setCategory(category);
        return p;
    }

    private Order buildOrder(UUID orderNumber, OrderStatus status) {
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setStatus(status);
        order.setCreatedAt(Instant.now());
        OrderItem item = new OrderItem();
        item.setProduct(milk);
        item.setQuantity(1);
        item.setUnitPrice(milk.getPrice());
        order.addItem(item);
        return order;
    }
}
