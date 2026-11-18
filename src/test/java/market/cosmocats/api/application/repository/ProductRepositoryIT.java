package market.cosmocats.api.application.repository;

import market.cosmocats.api.application.repository.projection.ProductSummary;
import market.cosmocats.api.domain.Category;
import market.cosmocats.api.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class ProductRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    private ProductRepository productRepository;

    private Category food;
    private Category toys;

    @BeforeEach
    void setUp() {
        food = category("Space Food");
        toys = category("Space Toys");
    }

    @Test
    void save_shouldPersistProductWithCategory() {
        Product p = new Product();
        p.setName("Star Milk");
        p.setDescription("From the Milky Way");
        p.setPrice(new BigDecimal("45.50"));
        p.setCategory(food);

        Long id = productRepository.saveAndFlush(p).getId();
        flushAndClear();

        Product found = productRepository.findById(id).orElseThrow();
        assertThat(found.getName()).isEqualTo("Star Milk");
        assertThat(found.getDescription()).isEqualTo("From the Milky Way");
        assertThat(found.getPrice()).isEqualByComparingTo("45.50");
        assertThat(found.getCategory().getId()).isEqualTo(food.getId());
    }

    @Test
    void update_shouldChangeFieldsAndCategory() {
        Long id = product("Star Milk", "45.50", food).getId();
        flushAndClear();

        Product p = productRepository.findById(id).orElseThrow();
        p.setPrice(new BigDecimal("50.00"));
        p.setCategory(em.find(Category.class, toys.getId()));
        productRepository.saveAndFlush(p);
        flushAndClear();

        Product updated = productRepository.findById(id).orElseThrow();
        assertThat(updated.getPrice()).isEqualByComparingTo("50.00");
        assertThat(updated.getCategory().getId()).isEqualTo(toys.getId());
    }

    @Test
    void delete_shouldRemoveProduct() {
        Long id = product("Star Milk", "45.50", food).getId();
        flushAndClear();

        productRepository.deleteById(id);
        flushAndClear();

        assertThat(productRepository.findById(id)).isEmpty();
    }

    @Test
    void findAll_shouldReturnRequestedPageSortedById() {
        Product a = product("Star A", "1.00", food);
        Product b = product("Star B", "2.00", food);
        Product c = product("Star C", "3.00", toys);
        flushAndClear();

        Page<Product> page = productRepository.findAll(PageRequest.of(1, 2, Sort.by("id")));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.getContent()).extracting(Product::getId).containsExactly(c.getId());
        assertThat(List.of(a.getId(), b.getId())).allSatisfy(id -> assertThat(id).isLessThan(c.getId()));
    }

    @Test
    void shouldRejectDuplicateNameInSameCategory() {
        product("Star Milk", "45.50", food);
        em.flush();

        assertThatThrownBy(() -> productRepository.saveAndFlush(newProduct("Star Milk", "10.00", food)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_product_category_name");
    }

    @Test
    void shouldAllowSameNameInDifferentCategories() {
        product("Star Milk", "45.50", food);
        em.flush();

        Product other = productRepository.saveAndFlush(newProduct("Star Milk", "10.00", toys));

        assertThat(other.getId()).isNotNull();
    }

    @Test
    void shouldRejectNonPositivePrice() {
        assertThatThrownBy(() -> productRepository.saveAndFlush(newProduct("Star Milk", "0.00", food)))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("chk_product_price_positive");
    }

    @Test
    void existsByCategoryId_shouldReflectWhetherCategoryHasProducts() {
        product("Star Milk", "45.50", food);
        flushAndClear();

        assertThat(productRepository.existsByCategoryId(food.getId())).isTrue();
        assertThat(productRepository.existsByCategoryId(toys.getId())).isFalse();
    }

    @Test
    void findByCategoryId_shouldReturnSummariesOfThatCategoryCheapestFirst() {
        product("Star Milk", "45.50", food);
        product("Comet Cookies", "5.00", food);
        product("Astro Snack", "5.00", food);
        product("Star Ball", "1.00", toys);
        flushAndClear();

        List<ProductSummary> result = productRepository.findByCategoryIdOrderByPriceAscNameAsc(food.getId());

        assertThat(result)
                .extracting(ProductSummary::getName, s -> s.getPrice().toPlainString())
                .containsExactly(
                        tuple("Astro Snack", "5.00"),
                        tuple("Comet Cookies", "5.00"),
                        tuple("Star Milk", "45.50"));
        assertThat(result.get(0).getId()).isNotNull();
    }

    private static Product newProduct(String name, String price, Category category) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.setCategory(category);
        return p;
    }
}
