package market.cosmocats.api.domain;

import java.util.UUID;
import java.util.List;

public class Cart {
    private UUID id;
    private List<UUID> productIds;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public List<UUID> getProductIds() { return productIds; }
    public void setProductIds(List<UUID> productIds) { this.productIds = productIds; }
}
