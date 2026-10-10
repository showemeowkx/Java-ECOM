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
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ObjectMapper objectMapper;
    private final File dbFile = new File("database.json");
    private List<Product> mockProducts = new ArrayList<>();

    public ProductService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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
        Product product = ProductMapper.toDomain(req);
        mockProducts.add(product);
        saveToFile();
        return ProductMapper.toDto(product);
    }

    public PagedResponse getAllProducts(int page, int limit) {
        List<ProductResp> dtos = mockProducts.stream()
                .map(ProductMapper::toDto)
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
                .filter(prod -> prod.id.toString().equals(id))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Product not found!")); 
        return ProductMapper.toDto(p);
    }

    public ProductResp updateProduct(String id, ProductReq req) {
        Product p = mockProducts.stream()
                .filter(prod -> prod.id.toString().equals(id))
                .findFirst()
                .orElse(null); 

        if (p != null) {
            p.name = req.name;
            p.desc = req.description;
            p.price = req.price;
            if(req.categoryId != null) {
                p.categoryId = UUID.fromString(req.categoryId);
            }
            saveToFile();
        }
        return ProductMapper.toDto(p);
    }

    public void deleteProduct(String id) {
        mockProducts.removeIf(prod -> prod.id.toString().equals(id));
        saveToFile();
    }
}
