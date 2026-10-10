package market.cosmocats.api.domain;

import java.util.UUID;
import java.util.List;

public class Order {
    public UUID orderId;
    public List<Product> productsList;
    public Float totalAmount;
}
