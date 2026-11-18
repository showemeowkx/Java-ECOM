package market.cosmocats.api.presentation;

import com.jayway.jsonpath.JsonPath;
import market.cosmocats.api.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.liquibase.contexts=test")
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
abstract class AbstractApiIT {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void cleanDatabase() {
        jdbc.execute("TRUNCATE order_item, orders, product, category");
    }

    protected ResultActions postJson(String url, String body) throws Exception {
        return mockMvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected long createCategory(String title) throws Exception {
        return idOf(postJson("/api/categories", "{\"title\":\"" + title + "\"}"), "$.id");
    }

    protected long createProduct(String name, String price, long categoryId) throws Exception {
        return idOf(postJson("/api/products",
                "{\"name\":\"" + name + "\",\"price\":" + price + ",\"categoryId\":" + categoryId + "}"), "$.id");
    }

    protected String createOrder(String itemsJson) throws Exception {
        String body = postJson("/api/orders", "{\"items\":" + itemsJson + "}")
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.orderNumber");
    }

    protected void setStatus(String orderNumber, String status) throws Exception {
        mockMvc.perform(patch("/api/orders/" + orderNumber + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk());
    }

    protected int count(String table) {
        return jdbc.queryForObject("SELECT count(*) FROM " + table, Integer.class);
    }

    private static long idOf(ResultActions result, String path) throws Exception {
        String body = result.andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, path)).longValue();
    }
}
