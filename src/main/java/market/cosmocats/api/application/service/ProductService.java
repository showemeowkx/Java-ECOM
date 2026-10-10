package market.cosmocats.api.application.service;

import market.cosmocats.api.domain.Product;
import market.cosmocats.api.application.dto.ProductReq;
import market.cosmocats.api.application.dto.ProductResp;
import market.cosmocats.api.application.dto.PagedResponse;
import market.cosmocats.api.application.mapper.ProductMapper;
import market.cosmocats.api.application.repository.ProductRepository;
import market.cosmocats.api.application.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductMapper productMapper;

    public ProductService(ProductRepository productRepository, ProductMapper productMapper) {
        this.productRepository = productRepository;
        this.productMapper = productMapper;
    }

    public ProductResp createProduct(ProductReq req) {
        Product product = productMapper.toDomain(req);
        product.setId(UUID.randomUUID());
        productRepository.save(product);
        return productMapper.toDto(product);
    }

    public PagedResponse getAllProducts(int page, int limit) {
        List<Product> products = productRepository.findAll();
        List<ProductResp> dtos = products.stream()
                .map(productMapper::toDto)
                .collect(Collectors.toList());

        PagedResponse response = new PagedResponse();
        response.data = dtos;
        response.page = page;
        response.limit = limit;
        response.totalElements = products.size();

        return response;
    }

    public ProductResp getProduct(String id) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " not found"));
        return productMapper.toDto(p);
    }

    public ProductResp updateProduct(String id, ProductReq req) {
        Product p = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product with id " + id + " not found"));

        p.setName(req.name);
        p.setDescription(req.description);
        if (req.price != null) {
            p.setPrice(req.price);
        }
        if (req.categoryId != null) {
            p.setCategoryId(UUID.fromString(req.categoryId));
        }

        productRepository.save(p);

        return productMapper.toDto(p);
    }

    public void deleteProduct(String id) {
        productRepository.deleteById(id);
    }
}
