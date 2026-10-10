package market.cosmocats.api.application.mapper;

import market.cosmocats.api.domain.Product;
import market.cosmocats.api.application.dto.ProductReq;
import market.cosmocats.api.application.dto.ProductResp;
import java.util.UUID;

public class ProductMapper {
    
    public static Product toDomain(ProductReq req) {
        Product p = new Product();
        p.id = UUID.randomUUID();
        p.name = req.name;
        p.desc = req.description;
        p.price = req.price;
        if (req.categoryId != null) {
            p.categoryId = UUID.fromString(req.categoryId);
        }
        return p;
    }

    public static ProductResp toDto(Product product) {
        ProductResp resp = new ProductResp();
        if (product.id != null) resp.id = product.id.toString();
        resp.name = product.name;
        resp.description = product.desc;
        resp.price = product.price;
        if (product.categoryId != null) {
            resp.categoryId = product.categoryId.toString();
        }
        return resp;
    }
}
