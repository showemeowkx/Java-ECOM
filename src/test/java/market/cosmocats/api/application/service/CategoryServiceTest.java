package market.cosmocats.api.application.service;

import market.cosmocats.api.application.dto.CategoryReq;
import market.cosmocats.api.application.dto.CategoryResp;
import market.cosmocats.api.application.exception.ConflictException;
import market.cosmocats.api.application.exception.ResourceNotFoundException;
import market.cosmocats.api.application.mapper.CategoryMapper;
import market.cosmocats.api.application.repository.CategoryRepository;
import market.cosmocats.api.application.repository.ProductRepository;
import market.cosmocats.api.domain.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import market.cosmocats.api.application.dto.ProductSummaryResp;
import market.cosmocats.api.application.repository.projection.ProductSummary;
import org.springframework.data.domain.Sort;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    private static final Long CATEGORY_ID = 10L;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Spy
    private CategoryMapper categoryMapper = new CategoryMapper();

    @InjectMocks
    private CategoryService categoryService;

    private Category existing;

    @BeforeEach
    void setUp() {
        existing = buildCategory(CATEGORY_ID, "Space Food");
    }

    @Nested
    @DisplayName("createCategory")
    class CreateCategory {

        @Test
        void shouldSaveTrimmedTitleAndReturnDto() {
            when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(inv -> {
                Category c = inv.getArgument(0);
                c.setId(7L);
                return c;
            });

            CategoryResp resp = categoryService.createCategory(req("  Space Toys  "));

            ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
            verify(categoryRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getTitle()).isEqualTo("Space Toys");
            assertThat(resp.id).isEqualTo(7L);
            assertThat(resp.title).isEqualTo("Space Toys");
        }
    }

    @Nested
    @DisplayName("getAllCategories")
    class GetAllCategories {

        @Test
        void shouldReturnCategoriesSortedByTitle() {
            Category other = buildCategory(11L, "Space Toys");
            when(categoryRepository.findAll(Sort.by("title"))).thenReturn(List.of(existing, other));

            List<CategoryResp> result = categoryService.getAllCategories();

            assertThat(result).extracting(r -> r.title).containsExactly("Space Food", "Space Toys");
            assertThat(result).extracting(r -> r.id).containsExactly(CATEGORY_ID, 11L);
        }
    }

    @Nested
    @DisplayName("getCategory")
    class GetCategory {

        @Test
        void shouldReturnDtoWhenCategoryExists() {
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));

            CategoryResp resp = categoryService.getCategory(CATEGORY_ID);

            assertThat(resp.id).isEqualTo(CATEGORY_ID);
            assertThat(resp.title).isEqualTo("Space Food");
        }

        @Test
        void shouldThrowWhenCategoryNotFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.getCategory(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Category with id 99 not found");
        }
    }

    @Nested
    @DisplayName("getCategoryProducts")
    class GetCategoryProducts {

        @Test
        void shouldReturnProductSummariesFromProjection() {
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));
            when(productRepository.findByCategoryIdOrderByPriceAscNameAsc(CATEGORY_ID))
                    .thenReturn(List.of(summary(2L, "Star Toy", "5.00"), summary(1L, "Cosmic Milk", "45.50")));

            List<ProductSummaryResp> result = categoryService.getCategoryProducts(CATEGORY_ID);

            assertThat(result).extracting(r -> r.id).containsExactly(2L, 1L);
            assertThat(result.get(0).name).isEqualTo("Star Toy");
            assertThat(result.get(0).price).isEqualByComparingTo("5.00");
        }

        @Test
        void shouldThrowAndNotQueryProductsWhenCategoryMissing() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.getCategoryProducts(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(productRepository, never()).findByCategoryIdOrderByPriceAscNameAsc(any());
        }

        private static ProductSummary summary(Long id, String name, String price) {
            return new SpelAwareProxyProjectionFactory().createProjection(ProductSummary.class,
                    Map.of("id", id, "name", name, "price", new BigDecimal(price)));
        }
    }

    @Nested
    @DisplayName("updateCategory")
    class UpdateCategory {

        @Test
        void shouldUpdateTitleAndSave() {
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));
            when(categoryRepository.saveAndFlush(existing)).thenReturn(existing);

            CategoryResp resp = categoryService.updateCategory(CATEGORY_ID, req("Galaxy Food"));

            assertThat(existing.getTitle()).isEqualTo("Galaxy Food");
            assertThat(resp.id).isEqualTo(CATEGORY_ID);
            assertThat(resp.title).isEqualTo("Galaxy Food");
        }

        @Test
        void shouldThrowAndNotSaveWhenCategoryNotFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.updateCategory(99L, req("Galaxy Food")))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(categoryRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("deleteCategory")
    class DeleteCategory {

        @Test
        void shouldDeleteAndFlushWhenCategoryHasNoProducts() {
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));
            when(productRepository.existsByCategoryId(CATEGORY_ID)).thenReturn(false);

            categoryService.deleteCategory(CATEGORY_ID);

            verify(categoryRepository).delete(existing);
            verify(categoryRepository).flush();
        }

        @Test
        void shouldThrowConflictAndNotDeleteWhenCategoryHasProducts() {
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(existing));
            when(productRepository.existsByCategoryId(CATEGORY_ID)).thenReturn(true);

            assertThatThrownBy(() -> categoryService.deleteCategory(CATEGORY_ID))
                    .isInstanceOf(ConflictException.class)
                    .hasMessage("Category with id 10 still has products");
            verify(categoryRepository, never()).delete(any());
        }

        @Test
        void shouldThrowWhenCategoryNotFound() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> categoryService.deleteCategory(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(categoryRepository, never()).delete(any());
        }
    }

    private static CategoryReq req(String title) {
        CategoryReq req = new CategoryReq();
        req.title = title;
        return req;
    }

    private static Category buildCategory(Long id, String title) {
        Category c = new Category(title);
        c.setId(id);
        return c;
    }
}
