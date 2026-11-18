package market.cosmocats.api.application.dto;

import java.math.BigDecimal;

public class ProductResp {
    public Long id;
    public String name;
    public String description;
    public BigDecimal price;
    public Long categoryId;
}
