package market.cosmocats.api.application.repository;

import market.cosmocats.api.domain.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void save_shouldAssignIdFromSequenceAndPersist() {
        Category saved = categoryRepository.saveAndFlush(new Category("Space Food"));
        flushAndClear();

        assertThat(saved.getId()).isPositive();
        assertThat(categoryRepository.findById(saved.getId()))
                .get()
                .extracting(Category::getTitle)
                .isEqualTo("Space Food");
    }

    @Test
    void findAll_shouldSortByTitle() {
        category("Space Toys");
        category("Galaxy Food");
        category("Comet Beds");
        flushAndClear();

        assertThat(categoryRepository.findAll(Sort.by("title")))
                .extracting(Category::getTitle)
                .containsExactly("Comet Beds", "Galaxy Food", "Space Toys");
    }

    @Test
    void update_shouldChangeTitle() {
        Long id = category("Space Food").getId();
        flushAndClear();

        Category category = categoryRepository.findById(id).orElseThrow();
        category.setTitle("Galaxy Food");
        categoryRepository.saveAndFlush(category);
        flushAndClear();

        assertThat(categoryRepository.findById(id).orElseThrow().getTitle()).isEqualTo("Galaxy Food");
    }

    @Test
    void delete_shouldRemoveCategory() {
        Long id = category("Space Food").getId();
        flushAndClear();

        categoryRepository.deleteById(id);
        flushAndClear();

        assertThat(categoryRepository.findById(id)).isEmpty();
    }

    @Test
    void save_shouldRejectDuplicateTitle() {
        category("Space Food");
        em.flush();

        assertThatThrownBy(() -> categoryRepository.saveAndFlush(new Category("Space Food")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_category_title");
    }

    @Test
    void delete_shouldBeBlockedByForeignKeyWhenCategoryHasProducts() {
        Category category = category("Space Food");
        product("Star Milk", "4.50", category);
        flushAndClear();

        assertThatThrownBy(() -> {
            categoryRepository.deleteById(category.getId());
            categoryRepository.flush();
        }).isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_product_category");
    }
}
