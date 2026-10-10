package market.cosmocats.api.application.mapper;

import market.cosmocats.api.domain.Product;
import market.cosmocats.api.application.dto.ProductReq;
import market.cosmocats.api.application.dto.ProductResp;
import java.util.UUID;
import java.math.BigDecimal;

public class ProductMapper {
    
    public static Product toDomain(ProductReq req) {
        Product p = new Product();
        p.setId(UUID.randomUUID());
        p.setName(req.name);
        p.setDescription(req.description);
        if (req.price != null) {
            p.setPrice(BigDecimal.valueOf(req.price));
        }
        if (req.categoryId != null) {
            p.setCategoryId(UUID.fromString(req.categoryId));
        }
        return p;
    }

    public static ProductResp toDto(Product product) {
        ProductResp resp = new ProductResp();
        if (product.getId() != null) resp.id = product.getId().toString();
        resp.name = product.getName();
        resp.description = product.getDescription();
        if (product.getPrice() != null) {
            resp.price = product.getPrice().floatValue();
        }
        if (product.getCategoryId() != null) {
            resp.categoryId = product.getCategoryId().toString();
        }
        return resp;
    }
}
