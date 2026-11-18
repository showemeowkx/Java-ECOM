package market.cosmocats.api.application.mapper;

import market.cosmocats.api.application.dto.ProductReq;
import market.cosmocats.api.application.dto.ProductResp;
import market.cosmocats.api.domain.Category;
import market.cosmocats.api.domain.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public Product toEntity(ProductReq req, Category category) {
        Product p = new Product();
        apply(p, req, category);
        return p;
    }

    public void apply(Product product, ProductReq req, Category category) {
        product.setName(req.name);
        product.setDescription(req.description);
        product.setPrice(req.price);
        product.setCategory(category);
    }

    public ProductResp toDto(Product product) {
        ProductResp resp = new ProductResp();
        resp.id = product.getId();
        resp.name = product.getName();
        resp.description = product.getDescription();
        resp.price = product.getPrice();
        if (product.getCategory() != null) {
            resp.categoryId = product.getCategory().getId();
        }
        return resp;
    }
}
