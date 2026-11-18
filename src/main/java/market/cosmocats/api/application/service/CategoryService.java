package market.cosmocats.api.application.service;

import market.cosmocats.api.application.dto.CategoryReq;
import market.cosmocats.api.application.dto.CategoryResp;
import market.cosmocats.api.application.dto.ProductSummaryResp;
import market.cosmocats.api.application.repository.projection.ProductSummary;
import market.cosmocats.api.application.exception.ConflictException;
import market.cosmocats.api.application.exception.ResourceNotFoundException;
import market.cosmocats.api.application.mapper.CategoryMapper;
import market.cosmocats.api.application.repository.CategoryRepository;
import market.cosmocats.api.application.repository.ProductRepository;
import market.cosmocats.api.domain.Category;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CategoryMapper categoryMapper;

    public CategoryService(CategoryRepository categoryRepository,
                           ProductRepository productRepository,
                           CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.categoryMapper = categoryMapper;
    }

    @Transactional
    public CategoryResp createCategory(CategoryReq req) {
        Category saved = categoryRepository.saveAndFlush(categoryMapper.toEntity(req));
        return categoryMapper.toDto(saved);
    }

    public List<CategoryResp> getAllCategories() {
        return categoryRepository.findAll(Sort.by("title")).stream()
                .map(categoryMapper::toDto)
                .toList();
    }

    public CategoryResp getCategory(Long id) {
        return categoryMapper.toDto(findCategory(id));
    }

    public List<ProductSummaryResp> getCategoryProducts(Long id) {
        findCategory(id);
        return productRepository.findByCategoryIdOrderByPriceAscNameAsc(id).stream()
                .map(CategoryService::toDto)
                .toList();
    }

    @Transactional
    public CategoryResp updateCategory(Long id, CategoryReq req) {
        Category category = findCategory(id);
        categoryMapper.apply(category, req);
        return categoryMapper.toDto(categoryRepository.saveAndFlush(category));
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategory(id);
        if (productRepository.existsByCategoryId(id)) {
            throw new ConflictException("Category with id " + id + " still has products");
        }
        categoryRepository.delete(category);
        categoryRepository.flush();
    }

    private static ProductSummaryResp toDto(ProductSummary summary) {
        ProductSummaryResp resp = new ProductSummaryResp();
        resp.id = summary.getId();
        resp.name = summary.getName();
        resp.price = summary.getPrice();
        return resp;
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category with id " + id + " not found"));
    }
}
