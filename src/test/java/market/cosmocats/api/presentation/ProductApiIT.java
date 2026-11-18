package market.cosmocats.api.presentation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductApiIT extends AbstractApiIT {

    private long food;

    @BeforeEach
    void setUp() throws Exception {
        food = createCategory("Space Food");
    }

    @Test
    void create_shouldPersistProductAndReturnIt() throws Exception {
        postJson("/api/products", "{\"name\":\"Star Milk\",\"description\":\"Fresh\",\"price\":45.5,\"categoryId\":" + food + "}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Star Milk"))
                .andExpect(jsonPath("$.categoryId").value(food));

        assertThat(jdbc.queryForObject("SELECT price FROM product WHERE name = 'Star Milk'", String.class))
                .isEqualTo("45.50");
    }

    @Test
    void fullLifecycle_createReadUpdateDelete() throws Exception {
        long toys = createCategory("Space Toys");
        long id = createProduct("Star Ball", "3.50", food);

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Star Ball"))
                .andExpect(jsonPath("$.price").value(3.50));

        mockMvc.perform(put("/api/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Star Ball XL\",\"price\":4,\"categoryId\":" + toys + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Star Ball XL"))
                .andExpect(jsonPath("$.categoryId").value(toys));

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(jsonPath("$.name").value("Star Ball XL"))
                .andExpect(jsonPath("$.categoryId").value(toys));

        mockMvc.perform(delete("/api/products/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/products/" + id)).andExpect(status().isNotFound());
        assertThat(count("product")).isZero();
    }

    @Test
    void list_shouldPageThroughProductsInIdOrder() throws Exception {
        long a = createProduct("Star A", "1", food);
        long b = createProduct("Star B", "2", food);
        long c = createProduct("Star C", "3", food);

        mockMvc.perform(get("/api/products").param("page", "0").param("limit", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value(a))
                .andExpect(jsonPath("$.data[1].id").value(b));

        mockMvc.perform(get("/api/products").param("page", "1").param("limit", "2"))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(c));
    }

    @Test
    void create_shouldReturn409ForDuplicateNameInCategory() throws Exception {
        createProduct("Star Milk", "45.50", food);

        postJson("/api/products", "{\"name\":\"Star Milk\",\"price\":1,\"categoryId\":" + food + "}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));

        assertThat(count("product")).isEqualTo(1);
    }

    @Test
    void create_shouldReturn404AndPersistNothingForUnknownCategory() throws Exception {
        postJson("/api/products", "{\"name\":\"Star Milk\",\"price\":1,\"categoryId\":999999}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Category with id 999999 not found"));

        assertThat(count("product")).isZero();
    }

    @Test
    void create_shouldReturn400AndPersistNothingForInvalidBody() throws Exception {
        postJson("/api/products", "{\"name\":\"Regular Milk\",\"price\":0,\"categoryId\":" + food + "}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"));

        assertThat(count("product")).isZero();
    }

    @Test
    void update_shouldRollBackWhenRenameCollidesWithAnotherProduct() throws Exception {
        createProduct("Star Milk", "45.50", food);
        long yarn = createProduct("Comet Yarn", "10.00", food);

        mockMvc.perform(put("/api/products/" + yarn)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Star Milk\",\"price\":99,\"categoryId\":" + food + "}"))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/products/" + yarn))
                .andExpect(jsonPath("$.name").value("Comet Yarn"))
                .andExpect(jsonPath("$.price").value(10.00));
    }

    @Test
    void delete_shouldReturn409WhenProductIsInAnOrder() throws Exception {
        long milk = createProduct("Star Milk", "45.50", food);
        createOrder("[{\"productId\":" + milk + ",\"quantity\":1}]");

        mockMvc.perform(delete("/api/products/" + milk)).andExpect(status().isConflict());

        assertThat(count("product")).isEqualTo(1);
    }

    @Test
    void get_shouldReturn404ForUnknownId() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.instance").value("/api/products/999999"));
    }
}
