package market.cosmocats.api.application.service;

import market.cosmocats.api.application.dto.PagedResponse;
import market.cosmocats.api.application.dto.ProductReq;
import market.cosmocats.api.application.dto.ProductResp;
import market.cosmocats.api.application.exception.ResourceNotFoundException;
import market.cosmocats.api.application.mapper.ProductMapper;
import market.cosmocats.api.application.repository.ProductRepository;
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

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Spy
    private ProductMapper productMapper = new ProductMapper();

    @InjectMocks
    private ProductService productService;

    private UUID productId;
    private UUID categoryId;
    private Product existingProduct;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        categoryId = UUID.randomUUID();
        existingProduct = buildProduct(productId, "Star Cat Food", "Tasty", new BigDecimal("9.99"), categoryId);
    }

    @Nested
    @DisplayName("createProduct")
    class CreateProduct {

        @Test
        void shouldAssignIdSaveAndReturnDto() {
            ProductReq req = buildReq("Galaxy Toy", "Fluffy", new BigDecimal("19.50"), categoryId.toString());

            ProductResp resp = productService.createProduct(req);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(productRepository).save(captor.capture());
            Product saved = captor.getValue();

            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getName()).isEqualTo("Galaxy Toy");
            assertThat(saved.getDescription()).isEqualTo("Fluffy");
            assertThat(saved.getPrice()).isEqualByComparingTo("19.50");
            assertThat(saved.getCategoryId()).isEqualTo(categoryId);

            assertThat(resp.id).isEqualTo(saved.getId().toString());
            assertThat(resp.name).isEqualTo("Galaxy Toy");
            assertThat(resp.description).isEqualTo("Fluffy");
            assertThat(resp.price).isEqualByComparingTo("19.50");
            assertThat(resp.categoryId).isEqualTo(categoryId.toString());
        }

        @Test
        void shouldGenerateUniqueIdsForEachProduct() {
            ProductReq req = buildReq("Comet Ball", null, new BigDecimal("1.00"), categoryId.toString());

            ProductResp first = productService.createProduct(req);
            ProductResp second = productService.createProduct(req);

            assertThat(first.id).isNotEqualTo(second.id);
        }
    }

    @Nested
    @DisplayName("getAllProducts")
    class GetAllProducts {

        @Test
        void shouldReturnAllProductsWithPagingInfo() {
            Product other = buildProduct(UUID.randomUUID(), "Comet Ball", null, new BigDecimal("2.00"), categoryId);
            when(productRepository.findAll()).thenReturn(List.of(existingProduct, other));

            PagedResponse response = productService.getAllProducts(1, 10);

            assertThat(response.page).isEqualTo(1);
            assertThat(response.limit).isEqualTo(10);
            assertThat(response.totalElements).isEqualTo(2);
            assertThat(response.data).hasSize(2);
            assertThat(response.data).extracting(r -> r.name).containsExactly("Star Cat Food", "Comet Ball");
            assertThat(response.data.get(0).id).isEqualTo(productId.toString());
        }

        @Test
        void shouldReturnEmptyPageWhenNoProducts() {
            when(productRepository.findAll()).thenReturn(Collections.emptyList());

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
            when(productRepository.findById(productId.toString())).thenReturn(Optional.of(existingProduct));

            ProductResp resp = productService.getProduct(productId.toString());

            assertThat(resp.id).isEqualTo(productId.toString());
            assertThat(resp.name).isEqualTo("Star Cat Food");
            assertThat(resp.description).isEqualTo("Tasty");
            assertThat(resp.price).isEqualByComparingTo("9.99");
            assertThat(resp.categoryId).isEqualTo(categoryId.toString());
        }

        @Test
        void shouldThrowWhenProductNotFound() {
            String missingId = UUID.randomUUID().toString();
            when(productRepository.findById(missingId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> productService.getProduct(missingId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Product with id " + missingId + " not found");
        }
    }

    @Nested
    @DisplayName("updateProduct")
    class UpdateProduct {

        @Test
        void shouldUpdateAllFieldsAndSave() {
            UUID newCategoryId = UUID.randomUUID();
            when(productRepository.findById(productId.toString())).thenReturn(Optional.of(existingProduct));
            ProductReq req = buildReq("Galaxy Food", "Even tastier", new BigDecimal("12.00"), newCategoryId.toString());

            ProductResp resp = productService.updateProduct(productId.toString(), req);

            verify(productRepository).save(existingProduct);
            assertThat(existingProduct.getId()).isEqualTo(productId);
            assertThat(existingProduct.getName()).isEqualTo("Galaxy Food");
            assertThat(existingProduct.getDescription()).isEqualTo("Even tastier");
            assertThat(existingProduct.getPrice()).isEqualByComparingTo("12.00");
            assertThat(existingProduct.getCategoryId()).isEqualTo(newCategoryId);

            assertThat(resp.id).isEqualTo(productId.toString());
            assertThat(resp.name).isEqualTo("Galaxy Food");
            assertThat(resp.categoryId).isEqualTo(newCategoryId.toString());
        }

        @Test
        void shouldThrowAndNotSaveWhenProductNotFound() {
            String missingId = UUID.randomUUID().toString();
            when(productRepository.findById(missingId)).thenReturn(Optional.empty());
            ProductReq req = buildReq("Comet", null, BigDecimal.ONE, categoryId.toString());

            assertThatThrownBy(() -> productService.updateProduct(missingId, req))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(missingId);
            verify(productRepository, never()).save(any());
        }

        @Test
        void shouldRejectInvalidCategoryId() {
            when(productRepository.findById(productId.toString())).thenReturn(Optional.of(existingProduct));
            ProductReq req = buildReq("Star", null, BigDecimal.ONE, "not-a-uuid");

            assertThatThrownBy(() -> productService.updateProduct(productId.toString(), req))
                    .isInstanceOf(IllegalArgumentException.class);
            verify(productRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteProduct")
    class DeleteProduct {

        @Test
        void shouldDelegateToRepository() {
            productService.deleteProduct(productId.toString());

            verify(productRepository).deleteById(productId.toString());
        }
    }

    private static ProductReq buildReq(String name, String description, BigDecimal price, String categoryId) {
        ProductReq req = new ProductReq();
        req.name = name;
        req.description = description;
        req.price = price;
        req.categoryId = categoryId;
        return req;
    }

    private static Product buildProduct(UUID id, String name, String description, BigDecimal price, UUID categoryId) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setDescription(description);
        p.setPrice(price);
        p.setCategoryId(categoryId);
        return p;
    }
}
