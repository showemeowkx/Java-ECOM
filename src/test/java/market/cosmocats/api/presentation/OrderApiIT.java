package market.cosmocats.api.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderApiIT extends AbstractApiIT {

    private long milk;
    private long yarn;

    @BeforeEach
    void setUp() throws Exception {
        long food = createCategory("Space Food");
        milk = createProduct("Star Milk", "45.50", food);
        yarn = createProduct("Comet Yarn", "10.00", food);
    }

    @Test
    void create_shouldPersistOrderWithItemsAndReturnNaturalId() throws Exception {
        postJson("/api/orders", "{\"items\":" + items(milk, 2, yarn, 3) + "}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderNumber").isString())
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].unitPrice").value(45.50))
                .andExpect(jsonPath("$.items[0].lineTotal").value(91.00))
                .andExpect(jsonPath("$.totalAmount").value(121.00));

        assertThat(count("orders")).isEqualTo(1);
        assertThat(count("order_item")).isEqualTo(2);
    }

    @Test
    void get_shouldFindOrderByNaturalId() throws Exception {
        String number = createOrder(items(milk, 1));
        String createdAt = jdbc.queryForObject(
                "SELECT to_char(created_at AT TIME ZONE 'UTC', 'YYYY-MM-DD\"T\"HH24:MI') FROM orders WHERE order_number = ?::uuid",
                String.class, number);

        mockMvc.perform(get("/api/orders/" + number))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value(number))
                .andExpect(jsonPath("$.createdAt").value(startsWith(createdAt)))
                .andExpect(jsonPath("$.items[0].productName").value("Star Milk"));
    }

    @Test
    void unitPrice_shouldStaySnapshottedAfterProductPriceChanges() throws Exception {
        String number = createOrder(items(milk, 2));
        jdbc.update("UPDATE product SET price = 100 WHERE id = ?", milk);

        mockMvc.perform(get("/api/orders/" + number))
                .andExpect(jsonPath("$.items[0].unitPrice").value(45.50))
                .andExpect(jsonPath("$.totalAmount").value(91.00));
    }

    @Test
    void list_shouldReturnAllOrdersNewestFirst() throws Exception {
        String first = createOrder(items(milk, 1));
        String second = createOrder(items(yarn, 1));
        jdbc.update("UPDATE orders SET created_at = created_at - interval '1 hour' WHERE order_number = ?::uuid", first);

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].orderNumber").value(second))
                .andExpect(jsonPath("$[1].orderNumber").value(first));
    }

    @Test
    void create_shouldReturn404AndPersistNothingWhenAnyProductIsMissing() throws Exception {
        postJson("/api/orders", "{\"items\":" + items(milk, 1, 999998L, 1, 999999L, 1) + "}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Products not found: [999998, 999999]"));

        assertThat(count("orders")).isZero();
        assertThat(count("order_item")).isZero();
    }

    @Test
    void status_shouldMoveThroughLifecycleAndLockWhenFinished() throws Exception {
        String number = createOrder(items(milk, 1));

        setStatus(number, "PAID");
        setStatus(number, "SHIPPED");
        setStatus(number, "DELIVERED");

        mockMvc.perform(patch("/api/orders/" + number + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("SELECT status FROM orders WHERE order_number = ?::uuid", String.class, number))
                .isEqualTo("DELIVERED");
    }

    @Test
    void delete_shouldRemoveOrderAndItems() throws Exception {
        String number = createOrder(items(milk, 1, yarn, 1));

        mockMvc.perform(delete("/api/orders/" + number)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/orders/" + number)).andExpect(status().isNotFound());
        assertThat(count("orders")).isZero();
        assertThat(count("order_item")).isZero();
        assertThat(count("product")).isEqualTo(2);
    }

    @Test
    void get_shouldReturn404ForUnknownOrderNumber() throws Exception {
        mockMvc.perform(get("/api/orders/" + UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    private static String items(Object... productAndQuantity) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < productAndQuantity.length; i += 2) {
            if (i > 0) sb.append(',');
            sb.append("{\"productId\":").append(productAndQuantity[i])
                    .append(",\"quantity\":").append(productAndQuantity[i + 1]).append('}');
        }
        return sb.append(']').toString();
    }
}
