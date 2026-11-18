package market.cosmocats.api.application.repository;

import market.cosmocats.api.domain.Category;
import market.cosmocats.api.domain.Order;
import market.cosmocats.api.domain.OrderItem;
import market.cosmocats.api.domain.OrderStatus;
import market.cosmocats.api.domain.Product;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class OrderRepositoryIT extends AbstractRepositoryIT {

    private static final Instant NOW = Instant.parse("2026-11-18T12:00:00Z");

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    private Product milk;
    private Product yarn;

    @BeforeEach
    void setUp() {
        Category category = category("Space Food");
        milk = product("Star Milk", "45.50", category);
        yarn = product("Comet Yarn", "10.00", category);
    }

    @Test
    void save_shouldGenerateNaturalIdDefaultsAndCascadeItems() {
        Order order = new Order();
        OrderItem item = new OrderItem();
        item.setProduct(milk);
        item.setQuantity(2);
        item.setUnitPrice(milk.getPrice());
        order.addItem(item);

        Order saved = orderRepository.saveAndFlush(order);
        flushAndClear();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getOrderNumber()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(countItems(saved.getId())).isEqualTo(1);
    }

    @Test
    void findByNaturalId_shouldLoadOrderWithItemsFromDatabase() {
        UUID number = order(OrderStatus.PAID, NOW, milk, 2, yarn, 3).getOrderNumber();
        flushAndClear();

        Order found = orderRepository.findByNaturalId(number).orElseThrow();

        assertThat(found.getOrderNumber()).isEqualTo(number);
        assertThat(found.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(found.getCreatedAt()).isEqualTo(NOW);
        assertThat(found.getItems())
                .extracting(i -> i.getProduct().getName(), OrderItem::getQuantity)
                .containsExactlyInAnyOrder(
                        tuple("Star Milk", 2),
                        tuple("Comet Yarn", 3));
        assertThat(found.getTotalAmount()).isEqualByComparingTo("121.00");
    }

    @Test
    void findByNaturalId_shouldReturnEmptyForUnknownOrderNumber() {
        order(OrderStatus.NEW, NOW, milk, 1);
        flushAndClear();

        assertThat(orderRepository.findByNaturalId(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findAllByOrderByCreatedAtDesc_shouldReturnNewestFirstWithItemsFetched() {
        UUID older = order(OrderStatus.PAID, NOW.minusSeconds(3600), milk, 1).getOrderNumber();
        UUID newer = order(OrderStatus.NEW, NOW, yarn, 2).getOrderNumber();
        flushAndClear();

        List<Order> orders = orderRepository.findAllByOrderByCreatedAtDesc();
        em.clear();

        assertThat(orders).extracting(Order::getOrderNumber).containsExactly(newer, older);
        assertThat(orders).allSatisfy(o -> {
            assertThat(Hibernate.isInitialized(o.getItems())).isTrue();
            assertThat(Hibernate.isInitialized(o.getItems().get(0).getProduct())).isTrue();
        });
        assertThat(orders.get(0).getItems().get(0).getProduct().getName()).isEqualTo("Comet Yarn");
    }

    @Test
    void updateStatus_shouldPersist() {
        Order order = order(OrderStatus.NEW, NOW, milk, 1);
        flushAndClear();

        Order loaded = orderRepository.findByNaturalId(order.getOrderNumber()).orElseThrow();
        loaded.setStatus(OrderStatus.SHIPPED);
        orderRepository.saveAndFlush(loaded);
        flushAndClear();

        assertThat(orderRepository.findByNaturalId(order.getOrderNumber()).orElseThrow().getStatus())
                .isEqualTo(OrderStatus.SHIPPED);
    }

    @Test
    void delete_shouldRemoveOrderAndItsItems() {
        Order order = order(OrderStatus.NEW, NOW, milk, 1, yarn, 1);
        Long id = order.getId();
        flushAndClear();

        orderRepository.delete(orderRepository.findByNaturalId(order.getOrderNumber()).orElseThrow());
        orderRepository.flush();
        em.clear();

        assertThat(orderRepository.findById(id)).isEmpty();
        assertThat(countItems(id)).isZero();
    }

    @Test
    void save_shouldRejectDuplicateOrderNumber() {
        UUID number = order(OrderStatus.NEW, NOW, milk, 1).getOrderNumber();
        flushAndClear();

        Order duplicate = new Order();
        duplicate.setOrderNumber(number);

        assertThatThrownBy(() -> orderRepository.saveAndFlush(duplicate))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_orders_order_number");
    }

    @Test
    void save_shouldRejectNonPositiveQuantity() {
        Order order = new Order();
        OrderItem item = new OrderItem();
        item.setProduct(milk);
        item.setQuantity(0);
        item.setUnitPrice(milk.getPrice());
        order.addItem(item);

        assertThatThrownBy(() -> orderRepository.saveAndFlush(order))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_order_item_quantity_positive");
    }

    @Test
    void deletingOrderedProduct_shouldBeBlockedByForeignKey() {
        order(OrderStatus.PAID, NOW, milk, 1);
        flushAndClear();

        assertThatThrownBy(() -> {
            productRepository.deleteById(milk.getId());
            productRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_order_item_product");
    }

    private long countItems(Long orderId) {
        return em.getEntityManager()
                .createQuery("SELECT count(oi) FROM OrderItem oi WHERE oi.order.id = :id", Long.class)
                .setParameter("id", orderId)
                .getSingleResult();
    }
}
