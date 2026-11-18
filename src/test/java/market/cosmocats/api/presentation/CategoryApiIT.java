package market.cosmocats.api.presentation;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryApiIT extends AbstractApiIT {

    @Test
    void fullLifecycle_createReadUpdateDelete() throws Exception {
        long id = createCategory("Space Food");

        mockMvc.perform(get("/api/categories/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Space Food"));

        mockMvc.perform(put("/api/categories/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Galaxy Food\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Galaxy Food"));

        assertThat(jdbc.queryForObject("SELECT title FROM category WHERE id = ?", String.class, id))
                .isEqualTo("Galaxy Food");

        mockMvc.perform(delete("/api/categories/" + id)).andExpect(status().isNoContent());
        mockMvc.perform(get("/api/categories/" + id)).andExpect(status().isNotFound());
        assertThat(count("category")).isZero();
    }

    @Test
    void create_shouldTrimTitle() throws Exception {
        postJson("/api/categories", "{\"title\":\"  Space Toys  \"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Space Toys"));
    }

    @Test
    void list_shouldReturnCategoriesSortedByTitle() throws Exception {
        createCategory("Space Toys");
        createCategory("Comet Beds");
        createCategory("Galaxy Food");

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].title").value(contains("Comet Beds", "Galaxy Food", "Space Toys")));
    }

    @Test
    void create_shouldReturn409ForDuplicateTitle() throws Exception {
        createCategory("Space Food");

        postJson("/api/categories", "{\"title\":\"Space Food\"}")
                .andExpect(status().isConflict());

        assertThat(count("category")).isEqualTo(1);
    }

    @Test
    void delete_shouldReturn409AndKeepCategoryWithProducts() throws Exception {
        long id = createCategory("Space Food");
        createProduct("Star Milk", "45.50", id);

        mockMvc.perform(delete("/api/categories/" + id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Category with id " + id + " still has products"));

        assertThat(count("category")).isEqualTo(1);
    }

    @Test
    void products_shouldReturnSummariesOfCategoryCheapestFirst() throws Exception {
        long food = createCategory("Space Food");
        long toys = createCategory("Space Toys");
        createProduct("Star Milk", "45.50", food);
        createProduct("Comet Cookies", "5.00", food);
        createProduct("Star Ball", "1.00", toys);

        mockMvc.perform(get("/api/categories/" + food + "/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Comet Cookies"))
                .andExpect(jsonPath("$[1].name").value("Star Milk"))
                .andExpect(jsonPath("$[0].description").doesNotExist());
    }

    @Test
    void products_shouldReturn404ForUnknownCategory() throws Exception {
        mockMvc.perform(get("/api/categories/999999/products"))
                .andExpect(status().isNotFound());
    }
}
