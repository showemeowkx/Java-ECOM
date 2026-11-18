package market.cosmocats.api.application.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import market.cosmocats.api.domain.Order;
import org.hibernate.Session;

import java.util.Optional;
import java.util.UUID;

class OrderNaturalIdRepositoryImpl implements OrderNaturalIdRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Optional<Order> findByNaturalId(UUID orderNumber) {
        return entityManager.unwrap(Session.class)
                .bySimpleNaturalId(Order.class)
                .loadOptional(orderNumber);
    }
}
