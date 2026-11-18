package market.cosmocats.api.application.repository.projection;

import java.math.BigDecimal;

public interface ProductSummary {

    Long getId();

    String getName();

    BigDecimal getPrice();
}
