package market.cosmocats.api.presentation;

import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportApiIT extends AbstractApiIT {

    @Test
    void topProducts_shouldRankPurchasedProductsOfLast30Days() throws Exception {
        long food = createCategory("Space Food");
        long milk = createProduct("Star Milk", "45.50", food);
        long yarn = createProduct("Comet Yarn", "10.00", food);

        setStatus(createOrder("[{\"productId\":" + milk + ",\"quantity\":2},{\"productId\":" + yarn + ",\"quantity\":1}]"), "PAID");
        setStatus(createOrder("[{\"productId\":" + milk + ",\"quantity\":3}]"), "PAID");
        createOrder("[{\"productId\":" + yarn + ",\"quantity\":50}]");
        String old = createOrder("[{\"productId\":" + yarn + ",\"quantity\":70}]");
        setStatus(old, "PAID");
        jdbc.update("UPDATE orders SET created_at = now() - interval '45 days' WHERE order_number = ?::uuid", old);

        mockMvc.perform(get("/api/reports/top-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].productName").value("Star Milk"))
                .andExpect(jsonPath("$[0].categoryTitle").value("Space Food"))
                .andExpect(jsonPath("$[0].totalQuantity").value(5))
                .andExpect(jsonPath("$[0].totalRevenue").value(227.50))
                .andExpect(jsonPath("$[1].productName").value("Comet Yarn"))
                .andExpect(jsonPath("$[1].totalQuantity").value(1));
    }

    @Test
    void topProducts_shouldIncludeOlderOrdersWhenWindowIsWidened() throws Exception {
        long food = createCategory("Space Food");
        long yarn = createProduct("Comet Yarn", "10.00", food);
        String old = createOrder("[{\"productId\":" + yarn + ",\"quantity\":70}]");
        setStatus(old, "DELIVERED");
        jdbc.update("UPDATE orders SET created_at = TIMESTAMPTZ '2025-06-15 12:00:00+00' WHERE order_number = ?::uuid", old);

        mockMvc.perform(get("/api/reports/top-products")
                        .param("from", "2025-06-01T00:00:00Z")
                        .param("to", "2025-07-01T00:00:00Z")
                        .param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].totalQuantity").value(70));
    }

    @Test
    void topProducts_shouldReturnEmptyListWhenNothingPurchased() throws Exception {
        mockMvc.perform(get("/api/reports/top-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
