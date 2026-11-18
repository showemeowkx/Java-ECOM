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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository,
                          CategoryRepository categoryRepository,
                          ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productMapper = productMapper;
    }

    @Transactional
    public ProductResp createProduct(ProductReq req) {
        Product product = productMapper.toEntity(req, findCategory(req.categoryId));
        return productMapper.toDto(productRepository.saveAndFlush(product));
    }

    public PagedResponse getAllProducts(int page, int limit) {
        Page<Product> result = productRepository.findAll(PageRequest.of(page, limit, Sort.by("id")));

        PagedResponse response = new PagedResponse();
        response.data = result.map(productMapper::toDto).getContent();
        response.page = page;
        response.limit = limit;
        response.totalElements = result.getTotalElements();
        return response;
    }

    public ProductResp getProduct(Long id) {
        return productMapper.toDto(findProduct(id));
    }

    @Transactional
    public ProductResp updateProduct(Long id, ProductReq req) {
        Product product = findProduct(id);
        productMapper.apply(product, req, findCategory(req.categoryId));
        return productMapper.toDto(productRepository.saveAndFlush(product));
    }

    @Transactional
    public void deleteProduct(Long id) {
        productRepository.delete(findProduct(id));
        productRepository.flush();
    }

    private Product findProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " not found"));
    }

    private Category findCategory(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category with id " + id + " not found"));
    }
}
