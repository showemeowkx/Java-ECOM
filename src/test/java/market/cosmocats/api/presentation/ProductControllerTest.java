package market.cosmocats.api.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import market.cosmocats.api.application.dto.PagedResponse;
import market.cosmocats.api.application.dto.ProductReq;
import market.cosmocats.api.application.dto.ProductResp;
import market.cosmocats.api.application.exception.ResourceNotFoundException;
import market.cosmocats.api.application.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final String PRODUCTS_URL = "/api/products";
    private static final String PRODUCT_ID = UUID.randomUUID().toString();
    private static final String CATEGORY_ID = UUID.randomUUID().toString();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    // ---------- positive scenarios ----------

    @ParameterizedTest(name = "{0}")
    @MethodSource("validRequests")
    void createProduct_shouldReturn201ForValidRequest(String scenario, Consumer<ProductReq> modifier) throws Exception {
        ProductReq req = validReq();
        modifier.accept(req);
        when(productService.createProduct(any(ProductReq.class))).thenReturn(respFor(req));

        mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID))
                .andExpect(jsonPath("$.name").value(req.name));

        verify(productService).createProduct(any(ProductReq.class));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validRequests")
    void updateProduct_shouldReturn200ForValidRequest(String scenario, Consumer<ProductReq> modifier) throws Exception {
        ProductReq req = validReq();
        modifier.accept(req);
        when(productService.updateProduct(eq(PRODUCT_ID), any(ProductReq.class))).thenReturn(respFor(req));

        mockMvc.perform(put(PRODUCTS_URL + "/" + PRODUCT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID))
                .andExpect(jsonPath("$.name").value(req.name));

        verify(productService).updateProduct(eq(PRODUCT_ID), any(ProductReq.class));
    }

    static Stream<Arguments> validRequests() {
        return Stream.of(
                scenario("all fields set", req -> { }),
                scenario("description omitted", req -> req.description = null),
                scenario("minimal price 0.01", req -> req.price = new BigDecimal("0.01")),
                scenario("name of 100 chars", req -> req.name = "star" + "a".repeat(96)),
                scenario("cosmic word in upper case", req -> req.name = "GALAXY Cat Food"),
                scenario("cosmic word in Ukrainian", req -> req.name = "Космічний корм")
        );
    }

    // ---------- negative scenarios ----------

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    void createProduct_shouldReturn400ForInvalidRequest(String scenario, Consumer<ProductReq> modifier,
                                                        String expectedError) throws Exception {
        ProductReq req = validReq();
        modifier.accept(req);

        expectValidationError(mockMvc.perform(post(PRODUCTS_URL)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))), PRODUCTS_URL, expectedError);

        verifyNoInteractions(productService);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidRequests")
    void updateProduct_shouldReturn400ForInvalidRequest(String scenario, Consumer<ProductReq> modifier,
                                                        String expectedError) throws Exception {
        ProductReq req = validReq();
        modifier.accept(req);
        String url = PRODUCTS_URL + "/" + PRODUCT_ID;

        expectValidationError(mockMvc.perform(put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))), url, expectedError);

        verifyNoInteractions(productService);
    }

    static Stream<Arguments> invalidRequests() {
        return Stream.of(
                invalid("name missing", req -> req.name = null, "Field 'name' Name is required"),
                invalid("name blank", req -> req.name = "   ", "Field 'name' Name is required"),
                invalid("name too short", req -> req.name = "ab",
                        "Field 'name' Name must be between 3 and 100 chars"),
                invalid("name too long", req -> req.name = "star" + "a".repeat(97),
                        "Field 'name' Name must be between 3 and 100 chars"),
                invalid("name without cosmic word", req -> req.name = "Regular Cat Food",
                        "Field 'name' Product name must contain a cosmic word"),
                invalid("price missing", req -> req.price = null, "Field 'price' Price is required"),
                invalid("price zero", req -> req.price = BigDecimal.ZERO, "Field 'price' Price must be positive"),
                invalid("price negative", req -> req.price = new BigDecimal("-5.00"),
                        "Field 'price' Price must be positive"),
                invalid("price below 0.01", req -> req.price = new BigDecimal("0.009"),
                        "Field 'price' Price must be positive"),
                invalid("categoryId missing", req -> req.categoryId = null,
                        "Field 'categoryId' Category ID is required")
        );
    }

    @Test
    void createProduct_shouldReportAllInvalidFieldsAtOnce() throws Exception {
        mockMvc.perform(post(PRODUCTS_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(allOf(
                        containsString("Field 'name' Name is required"),
                        containsString("Field 'price' Price is required"),
                        containsString("Field 'categoryId' Category ID is required"))));

        verifyNoInteractions(productService);
    }

    // ---------- read & delete scenarios ----------

    @Test
    void getProducts_shouldReturn200WithDefaultPagination() throws Exception {
        when(productService.getAllProducts(0, 10)).thenReturn(pagedResp(0, 10));

        mockMvc.perform(get(PRODUCTS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.limit").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.data[0].id").value(PRODUCT_ID));

        verify(productService).getAllProducts(0, 10);
    }

    @Test
    void getProducts_shouldPassPaginationParamsToService() throws Exception {
        when(productService.getAllProducts(2, 5)).thenReturn(pagedResp(2, 5));

        mockMvc.perform(get(PRODUCTS_URL).param("page", "2").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.limit").value(5));

        verify(productService).getAllProducts(2, 5);
    }

    @Test
    void getProductById_shouldReturn200WhenProductExists() throws Exception {
        when(productService.getProduct(PRODUCT_ID)).thenReturn(respFor(validReq()));

        mockMvc.perform(get(PRODUCTS_URL + "/" + PRODUCT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PRODUCT_ID))
                .andExpect(jsonPath("$.name").value("Star Cat Food"))
                .andExpect(jsonPath("$.categoryId").value(CATEGORY_ID));

        verify(productService).getProduct(PRODUCT_ID);
    }

    @Test
    void getProductById_shouldReturn404WhenProductMissing() throws Exception {
        String url = PRODUCTS_URL + "/" + PRODUCT_ID;
        when(productService.getProduct(PRODUCT_ID))
                .thenThrow(new ResourceNotFoundException("Product not found with id: " + PRODUCT_ID));

        mockMvc.perform(get(url))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource not found"))
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/not-found"))
                .andExpect(jsonPath("$.instance").value(url))
                .andExpect(jsonPath("$.detail").value("Product not found with id: " + PRODUCT_ID));
    }

    @Test
    void deleteProduct_shouldReturn204() throws Exception {
        mockMvc.perform(delete(PRODUCTS_URL + "/" + PRODUCT_ID))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(PRODUCT_ID);
    }

    @Test
    void deleteProduct_shouldReturn404WhenProductMissing() throws Exception {
        doThrow(new ResourceNotFoundException("Product not found with id: " + PRODUCT_ID))
                .when(productService).deleteProduct(PRODUCT_ID);

        mockMvc.perform(delete(PRODUCTS_URL + "/" + PRODUCT_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Resource not found"));
    }

    @Test
    void getProductById_shouldReturn500OnUnexpectedError() throws Exception {
        String url = PRODUCTS_URL + "/" + PRODUCT_ID;
        when(productService.getProduct(PRODUCT_ID)).thenThrow(new IllegalStateException("db is down"));

        mockMvc.perform(get(url))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal Server Error"))
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/internal"))
                .andExpect(jsonPath("$.instance").value(url))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"));
    }

    private static void expectValidationError(ResultActions result, String url, String expectedError)
            throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/validation"))
                .andExpect(jsonPath("$.instance").value(url))
                .andExpect(jsonPath("$.detail").value(containsString(expectedError)));
    }

    private static ProductReq validReq() {
        ProductReq req = new ProductReq();
        req.name = "Star Cat Food";
        req.description = "Tasty food from another galaxy";
        req.price = new BigDecimal("9.99");
        req.categoryId = CATEGORY_ID;
        return req;
    }

    private static ProductResp respFor(ProductReq req) {
        ProductResp resp = new ProductResp();
        resp.id = PRODUCT_ID;
        resp.name = req.name;
        resp.description = req.description;
        resp.price = req.price;
        resp.categoryId = req.categoryId;
        return resp;
    }

    private static PagedResponse pagedResp(int page, int limit) {
        PagedResponse resp = new PagedResponse();
        resp.data = List.of(respFor(validReq()));
        resp.page = page;
        resp.limit = limit;
        resp.totalElements = 1;
        return resp;
    }

    private static Arguments scenario(String name, Consumer<ProductReq> modifier) {
        return Arguments.of(name, modifier);
    }

    private static Arguments invalid(String name, Consumer<ProductReq> modifier, String expectedError) {
        return Arguments.of(name, modifier, expectedError);
    }
}
