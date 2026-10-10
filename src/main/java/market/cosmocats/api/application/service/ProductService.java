package market.cosmocats.api.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import market.cosmocats.api.domain.Product;
import market.cosmocats.api.application.dto.ProductReq;
import market.cosmocats.api.application.dto.ProductResp;
import market.cosmocats.api.application.dto.PagedResponse;
import market.cosmocats.api.application.mapper.ProductMapper;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.math.BigDecimal;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ObjectMapper objectMapper;
    private final ProductMapper productMapper;
    private final File dbFile = new File("database.json");
    private List<Product> mockProducts = new ArrayList<>();

    public ProductService(ObjectMapper objectMapper, ProductMapper productMapper) {
        this.objectMapper = objectMapper;
        this.productMapper = productMapper;
    }

    @PostConstruct
    public void init() {
        if (dbFile.exists()) {
            try {
                mockProducts = objectMapper.readValue(dbFile, new TypeReference<List<Product>>() {});
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveToFile() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(dbFile, mockProducts);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ProductResp createProduct(ProductReq req) {
        Product product = productMapper.toDomain(req);
        product.setId(UUID.randomUUID());
        mockProducts.add(product);
        saveToFile();
        return productMapper.toDto(product);
    }

    public PagedResponse getAllProducts(int page, int limit) {
        List<ProductResp> dtos = mockProducts.stream()
                .map(productMapper::toDto)
                .collect(Collectors.toList());

        PagedResponse response = new PagedResponse();
        response.data = dtos; 
        response.page = page;
        response.limit = limit;
        response.totalElements = String.valueOf(mockProducts.size());
        
        return response;
    }

    public ProductResp getProduct(String id) {
        Product p = mockProducts.stream()
                .filter(prod -> prod.getId().toString().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Product not found!")); 
        return productMapper.toDto(p);
    }

    public ProductResp updateProduct(String id, ProductReq req) {
        Product p = mockProducts.stream()
                .filter(prod -> prod.getId().toString().equals(id))
                .findFirst()
                .orElse(null); 

        if (p != null) {
            p.setName(req.name);
            p.setDescription(req.description);
            if (req.price != null) {
                p.setPrice(BigDecimal.valueOf(req.price));
            }
            if(req.categoryId != null) {
                p.setCategoryId(UUID.fromString(req.categoryId));
            }
            saveToFile();
        }
        return productMapper.toDto(p);
    }

    public void deleteProduct(String id) {
        mockProducts.removeIf(prod -> prod.getId().toString().equals(id));
        saveToFile();
    }
}
