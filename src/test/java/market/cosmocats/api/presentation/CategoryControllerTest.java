package market.cosmocats.api.presentation;

import com.fasterxml.jackson.databind.ObjectMapper;
import market.cosmocats.api.application.dto.CategoryReq;
import market.cosmocats.api.application.dto.CategoryResp;
import market.cosmocats.api.application.dto.ProductSummaryResp;
import market.cosmocats.api.application.exception.ConflictException;
import market.cosmocats.api.application.exception.ResourceNotFoundException;
import market.cosmocats.api.application.service.CategoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

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

@WebMvcTest(CategoryController.class)
class CategoryControllerTest {

    private static final String CATEGORIES_URL = "/api/categories";
    private static final Long CATEGORY_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CategoryService categoryService;

    @Test
    void getCategories_shouldReturn200WithList() throws Exception {
        when(categoryService.getAllCategories()).thenReturn(List.of(resp(CATEGORY_ID, "Space Food"), resp(11L, "Space Toys")));

        mockMvc.perform(get(CATEGORIES_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(CATEGORY_ID))
                .andExpect(jsonPath("$[1].title").value("Space Toys"));
    }

    @Test
    void getCategoryById_shouldReturn200WhenExists() throws Exception {
        when(categoryService.getCategory(CATEGORY_ID)).thenReturn(resp(CATEGORY_ID, "Space Food"));

        mockMvc.perform(get(CATEGORIES_URL + "/" + CATEGORY_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CATEGORY_ID))
                .andExpect(jsonPath("$.title").value("Space Food"));
    }

    @Test
    void getCategoryById_shouldReturn404WhenMissing() throws Exception {
        when(categoryService.getCategory(99L)).thenThrow(new ResourceNotFoundException("Category with id 99 not found"));

        mockMvc.perform(get(CATEGORIES_URL + "/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Category with id 99 not found"));
    }

    @Test
    void getCategoryProducts_shouldReturn200WithSummaries() throws Exception {
        ProductSummaryResp summary = new ProductSummaryResp();
        summary.id = 1L;
        summary.name = "Star Toy";
        summary.price = new BigDecimal("5.00");
        when(categoryService.getCategoryProducts(CATEGORY_ID)).thenReturn(List.of(summary));

        mockMvc.perform(get(CATEGORIES_URL + "/" + CATEGORY_ID + "/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Star Toy"))
                .andExpect(jsonPath("$[0].price").value(5.00))
                .andExpect(jsonPath("$[0].description").doesNotExist());
    }

    @Test
    void getCategoryProducts_shouldReturn404WhenCategoryMissing() throws Exception {
        when(categoryService.getCategoryProducts(99L))
                .thenThrow(new ResourceNotFoundException("Category with id 99 not found"));

        mockMvc.perform(get(CATEGORIES_URL + "/99/products"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCategory_shouldReturn201ForValidRequest() throws Exception {
        when(categoryService.createCategory(any(CategoryReq.class))).thenReturn(resp(CATEGORY_ID, "Space Food"));

        mockMvc.perform(post(CATEGORIES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Space Food")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CATEGORY_ID))
                .andExpect(jsonPath("$.title").value("Space Food"));
    }

    @ParameterizedTest
    @MethodSource("invalidTitles")
    void createCategory_shouldReturn400ForInvalidTitle(String title, String expectedError) throws Exception {
        mockMvc.perform(post(CATEGORIES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(title)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.detail").value(containsString(expectedError)));

        verifyNoInteractions(categoryService);
    }

    static Stream<Arguments> invalidTitles() {
        return Stream.of(
                Arguments.of(null, "Field 'title' Title is required"),
                Arguments.of("   ", "Field 'title' Title is required"),
                Arguments.of("a".repeat(101), "Field 'title' Title must be at most 100 chars")
        );
    }

    @Test
    void createCategory_shouldReturn409ForDuplicateTitle() throws Exception {
        when(categoryService.createCategory(any(CategoryReq.class)))
                .thenThrow(new DataIntegrityViolationException("uq_category_title"));

        mockMvc.perform(post(CATEGORIES_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Space Food")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    void updateCategory_shouldReturn200ForValidRequest() throws Exception {
        when(categoryService.updateCategory(eq(CATEGORY_ID), any(CategoryReq.class)))
                .thenReturn(resp(CATEGORY_ID, "Galaxy Food"));

        mockMvc.perform(put(CATEGORIES_URL + "/" + CATEGORY_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Galaxy Food")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Galaxy Food"));
    }

    @Test
    void deleteCategory_shouldReturn204() throws Exception {
        mockMvc.perform(delete(CATEGORIES_URL + "/" + CATEGORY_ID))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory(CATEGORY_ID);
    }

    @Test
    void deleteCategory_shouldReturn409WhenCategoryHasProducts() throws Exception {
        doThrow(new ConflictException("Category with id 10 still has products"))
                .when(categoryService).deleteCategory(CATEGORY_ID);

        mockMvc.perform(delete(CATEGORIES_URL + "/" + CATEGORY_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.type").value("https://cosmo-cats.market/errors/conflict"))
                .andExpect(jsonPath("$.detail").value("Category with id 10 still has products"))
                .andExpect(jsonPath("$.instance").value(CATEGORIES_URL + "/" + CATEGORY_ID));
    }

    private String json(String title) throws Exception {
        CategoryReq req = new CategoryReq();
        req.title = title;
        return objectMapper.writeValueAsString(req);
    }

    private static CategoryResp resp(Long id, String title) {
        CategoryResp resp = new CategoryResp();
        resp.id = id;
        resp.title = title;
        return resp;
    }
}
