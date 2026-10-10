package market.cosmocats.api.presentation;

import market.cosmocats.api.application.dto.PagedResponse;
import market.cosmocats.api.application.dto.ProductReq;
import market.cosmocats.api.application.dto.ProductResp;
import market.cosmocats.api.application.service.ProductService;
import org.springframework.web.bind.annotation.*;

@RestController
public class ProductController {

    @org.springframework.beans.factory.annotation.Autowired
    private ProductService productService;

    @GetMapping("/api/products")
    public PagedResponse getProducts(
            @RequestParam(defaultValue = "1") int page_number,
            @RequestParam(defaultValue = "10") int limit) {
        return productService.getAllProducts(page_number, limit);
    }

    @PostMapping("/api/products")
    public ProductResp createProduct(@RequestBody ProductReq req) {
        return productService.createProduct(req);
    }

    @GetMapping("/api/products/{id}")
    public ProductResp getProductById(@PathVariable String id) {
        return productService.getProduct(id);
    }

    @PutMapping("/api/products/{id}")
    public ProductResp updateProduct(@PathVariable String id, @RequestBody ProductReq req) {
        return productService.updateProduct(id, req);
    }

    @DeleteMapping("/api/products/{id}")
    public String deleteProduct(@PathVariable String id) {
        productService.deleteProduct(id);
        return "Deleted"; 
    }
}
