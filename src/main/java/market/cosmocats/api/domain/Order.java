package market.cosmocats.api.domain;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.List;

@Data
public class Order {
    private UUID orderId;
    private List<Product> productsList;
    private BigDecimal totalAmount;
}
