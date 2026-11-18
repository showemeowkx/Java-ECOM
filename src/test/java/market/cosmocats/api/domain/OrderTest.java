package market.cosmocats.api.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderTest {

    @Test
    void onCreate_shouldFillNaturalIdTimestampAndStatusWhenMissing() {
        Order order = new Order();

        order.onCreate();

        assertThat(order.getOrderNumber()).isNotNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.NEW);
        assertThat(order.getCreatedAt()).isNotNull();
        assertThat(order.getCreatedAt()).isEqualTo(order.getCreatedAt().truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    void onCreate_shouldKeepValuesThatAreAlreadySet() {
        UUID number = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Order order = new Order();
        order.setOrderNumber(number);
        order.setCreatedAt(createdAt);
        order.setStatus(OrderStatus.PAID);

        order.onCreate();

        assertThat(order.getOrderNumber()).isEqualTo(number);
        assertThat(order.getCreatedAt()).isEqualTo(createdAt);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void addItem_shouldLinkItemBackToOrder() {
        Order order = new Order();
        OrderItem item = item("2.50", 4);

        order.addItem(item);

        assertThat(order.getItems()).containsExactly(item);
        assertThat(item.getOrder()).isSameAs(order);
    }

    @Test
    void getTotalAmount_shouldSumLineTotals() {
        Order order = new Order();
        order.addItem(item("2.50", 4));
        order.addItem(item("45.50", 2));

        assertThat(order.getTotalAmount()).isEqualByComparingTo("101.00");
    }

    @Test
    void getTotalAmount_shouldBeZeroForEmptyOrder() {
        assertThat(new Order().getTotalAmount()).isEqualByComparingTo("0");
    }

    private static OrderItem item(String unitPrice, int quantity) {
        OrderItem item = new OrderItem();
        item.setUnitPrice(new BigDecimal(unitPrice));
        item.setQuantity(quantity);
        return item;
    }
}
