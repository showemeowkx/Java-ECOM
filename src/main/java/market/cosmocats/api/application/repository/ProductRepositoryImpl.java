package market.cosmocats.api.application.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import market.cosmocats.api.domain.Product;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ProductRepositoryImpl implements ProductRepository {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final File dbFile = new File("database.json");
    private List<Product> mockProducts = new ArrayList<>();

    public ProductRepositoryImpl() {
    }

    @PostConstruct
    public void init() {
        if (dbFile.exists()) {
            try {
                mockProducts = objectMapper.readValue(dbFile, new TypeReference<List<Product>>() {
                });
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

    @Override
    public List<Product> findAll() {
        return new ArrayList<>(mockProducts);
    }

    @Override
    public Optional<Product> findById(String id) {
        return mockProducts.stream()
                .filter(prod -> prod.getId().toString().equals(id))
                .findFirst();
    }

    @Override
    public Product save(Product product) {
        mockProducts.removeIf(p -> p.getId().equals(product.getId()));
        mockProducts.add(product);
        saveToFile();
        return product;
    }

    @Override
    public void deleteById(String id) {
        mockProducts.removeIf(prod -> prod.getId().toString().equals(id));
        saveToFile();
    }
}
