package market.cosmocats.api.application.repository;

import market.cosmocats.api.domain.Order;

import java.util.Optional;
import java.util.UUID;

public interface OrderNaturalIdRepository {

    Optional<Order> findByNaturalId(UUID orderNumber);
}
