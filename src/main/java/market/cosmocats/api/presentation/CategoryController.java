package market.cosmocats.api.presentation;

import jakarta.validation.Valid;
import market.cosmocats.api.application.dto.CategoryReq;
import market.cosmocats.api.application.dto.CategoryResp;
import market.cosmocats.api.application.dto.ProductSummaryResp;
import market.cosmocats.api.application.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<List<CategoryResp>> getCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    @PostMapping
    public ResponseEntity<CategoryResp> createCategory(@Valid @RequestBody CategoryReq req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryService.createCategory(req));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResp> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategory(id));
    }

    @GetMapping("/{id}/products")
    public ResponseEntity<List<ProductSummaryResp>> getCategoryProducts(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryProducts(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResp> updateCategory(@PathVariable Long id, @Valid @RequestBody CategoryReq req) {
        return ResponseEntity.ok(categoryService.updateCategory(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
