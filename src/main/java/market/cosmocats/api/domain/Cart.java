package market.cosmocats.api.domain;

import lombok.Data;
import java.util.UUID;
import java.util.List;

@Data
public class Cart {
    private UUID id;
    private List<UUID> productIds;
}
