package market.cosmocats.api.application.service;

import market.cosmocats.api.application.dto.PagedResponse;
import market.cosmocats.api.application.dto.ProductReq;
import market.cosmocats.api.application.dto.ProductResp;
import market.cosmocats.api.application.exception.ResourceNotFoundException;
import market.cosmocats.api.application.mapper.ProductMapper;
import market.cosmocats.api.application.repository.CategoryRepository;
import market.cosmocats.api.application.repository.ProductRepository;
import market.cosmocats.api.domain.Category;
import market.cosmocats.api.domain.Product;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Long PRODUCT_ID = 1L;
    private static final Long CATEGORY_ID = 10L;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Spy
    private ProductMapper productMapper = new ProductMapper();

    @InjectMocks
    private ProductService productService;

    private Category category;
    private Product existingProduct;

    @BeforeEach
    void setUp() {
        category = buildCategory(CATEGORY_ID, "Space Food");
        existingProduct = buildProduct(PRODUCT_ID, "Star Cat Food", "Tasty", new BigDecimal("9.99"), category);
    }

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        void shouldSaveProductWithCategoryAndReturnDto() {
            when(categoryRepository.findById(CATEGORY_ID)).thenReturn(Optional.of(category));
            when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(inv -> {
                Product p = inv.getArgument(0);
                p.setId(42L);
                return p;
            });
            ProductReq req = buildReq("Galaxy Toy", "Fluffy", new BigDecimal("19.50"), CATEGORY_ID);

            ProductResp resp = productService.createProduct(req);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).saveAndFlush(captor.capture());
            Product saved = captor.getValue();
            assertThat(saved.getName()).isEqualTo("Galaxy Toy");
            assertThat(saved.getDescription()).isEqualTo("Fluffy");
            assertThat(saved.getPrice()).isEqualByComparingTo("19.50");
            assertThat(saved.getCategory()).isSameAs(category);

            assertThat(resp.id).isEqualTo(42L);
            assertThat(resp.name).isEqualTo("Galaxy Toy");
            assertThat(resp.description).isEqualTo("Fluffy");
            assertThat(resp.price).isEqualByComparingTo("19.50");
            assertThat(resp.categoryId).isEqualTo(CATEGORY_ID);
        }

        @Test
        void shouldThrowAndNotSaveWhenCategoryMissing() {
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
            ProductReq req = buildReq("Galaxy Toy", null, BigDecimal.ONE, 99L);

            assertThatThrownBy(() -> productService.createProduct(req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Category with id 99 not found");
            verify(productRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("getAllProducts")
    class GetAllProducts {

        @Test
        void shouldRequestSortedPageAndMapResult() {
            Product other = buildProduct(2L, "Comet Ball", null, new BigDecimal("2.00"), category);
            Pageable expected = PageRequest.of(1, 2, Sort.by("id"));
            Page<Product> page = new PageImpl<>(List.of(existingProduct, other), expected, 5);
            when(productRepository.findAll(expected)).thenReturn(page);

            PagedResponse response = productService.getAllProducts(1, 2);

            assertThat(response.page).isEqualTo(1);
            assertThat(response.limit).isEqualTo(2);
            assertThat(response.totalElements).isEqualTo(5);
            assertThat(response.data).extracting(r -> r.name).containsExactly("Star Cat Food", "Comet Ball");
            assertThat(response.data.get(0).id).isEqualTo(PRODUCT_ID);
            assertThat(response.data.get(0).categoryId).isEqualTo(CATEGORY_ID);
        }

        @Test
        void shouldReturnEmptyPageWhenNoProducts() {
            when(productRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

            PagedResponse response = productService.getAllProducts(0, 20);

            assertThat(response.data).isEmpty();
            assertThat(response.totalElements).isZero();
            assertThat(response.page).isZero();
            assertThat(response.limit).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("getProduct")
    class GetProduct {

        @Test
        void shouldReturnDtoWhenProductExists() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));

            ProductResp resp = productService.getProduct(PRODUCT_ID);

            assertThat(resp.id).isEqualTo(PRODUCT_ID);
            assertThat(resp.name).isEqualTo("Star Cat Food");
            assertThat(resp.description).isEqualTo("Tasty");
            assertThat(resp.price).isEqualByComparingTo("9.99");
            assertThat(resp.categoryId).isEqualTo(CATEGORY_ID);
        }

        @Test
        void shouldThrowWhenProductNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProduct(99L))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product with id 99 not found");
        }
    }

    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct {

        @Test
        void shouldUpdateAllFieldsAndSave() {
            Category newCategory = buildCategory(20L, "Space Toys");
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));
            when(categoryRepository.findById(20L)).thenReturn(Optional.of(newCategory));
            when(productRepository.saveAndFlush(existingProduct)).thenReturn(existingProduct);
            ProductReq req = buildReq("Galaxy Food", "Even tastier", new BigDecimal("12.00"), 20L);

            ProductResp resp = productService.updateProduct(PRODUCT_ID, req);

            verify(productRepository).saveAndFlush(existingProduct);
            assertThat(existingProduct.getId()).isEqualTo(PRODUCT_ID);
            assertThat(existingProduct.getName()).isEqualTo("Galaxy Food");
            assertThat(existingProduct.getDescription()).isEqualTo("Even tastier");
            assertThat(existingProduct.getPrice()).isEqualByComparingTo("12.00");
            assertThat(existingProduct.getCategory()).isSameAs(newCategory);

            assertThat(resp.id).isEqualTo(PRODUCT_ID);
            assertThat(resp.name).isEqualTo("Galaxy Food");
            assertThat(resp.categoryId).isEqualTo(20L);
        }

        @Test
        void shouldThrowAndNotSaveWhenProductNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());
            ProductReq req = buildReq("Comet", null, BigDecimal.ONE, CATEGORY_ID);

            assertThatThrownBy(() -> productService.updateProduct(99L, req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product with id 99 not found");
            verify(productRepository, never()).saveAndFlush(any());
        }

        @Test
        void shouldThrowAndNotSaveWhenCategoryMissing() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());
            ProductReq req = buildReq("Star", null, BigDecimal.ONE, 99L);

            assertThatThrownBy(() -> productService.updateProduct(PRODUCT_ID, req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Category with id 99 not found");
            verify(productRepository, never()).saveAndFlush(any());
        }
    }

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        void shouldDeleteAndFlushWhenProductExists() {
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(existingProduct));

            productService.deleteProduct(PRODUCT_ID);

            verify(productRepository).delete(existingProduct);
            verify(productRepository).flush();
        }

        @Test
        void shouldThrowAndNotDeleteWhenProductNotFound() {
            when(productRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.deleteProduct(99L))
                    .isInstanceOf(ResourceNotFoundException.class);
            verify(productRepository, never()).delete(any());
        }
    }

    private static ProductReq buildReq(String name, String description, BigDecimal price, Long categoryId) {
        ProductReq req = new ProductReq();
        req.name = name;
        req.description = description;
        req.price = price;
        req.categoryId = categoryId;
        return req;
    }

    private static Category buildCategory(Long id, String title) {
        Category c = new Category(title);
        c.setId(id);
        return c;
    }

    private static Product buildProduct(Long id, String name, String description, BigDecimal price, Category category) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setDescription(description);
        p.setPrice(price);
        p.setCategory(category);
        return p;
    }
}
