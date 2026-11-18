package market.cosmocats.api.application.repository;

import market.cosmocats.api.TestcontainersConfiguration;
import market.cosmocats.api.domain.Category;
import market.cosmocats.api.domain.Order;
import market.cosmocats.api.domain.OrderItem;
import market.cosmocats.api.domain.OrderStatus;
import market.cosmocats.api.domain.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.Instant;

@DataJpaTest(properties = "spring.liquibase.contexts=test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
abstract class AbstractRepositoryIT {

    @Autowired
    protected TestEntityManager em;

    protected Category category(String title) {
        return em.persist(new Category(title));
    }

    protected Product product(String name, String price, Category category) {
        Product p = new Product();
        p.setName(name);
        p.setPrice(new BigDecimal(price));
        p.setCategory(category);
        return em.persist(p);
    }

    protected Order order(OrderStatus status, Instant createdAt, Object... productAndQuantity) {
        Order order = new Order();
        order.setStatus(status);
        order.setCreatedAt(createdAt);
        for (int i = 0; i < productAndQuantity.length; i += 2) {
            Product product = (Product) productAndQuantity[i];
            OrderItem item = new OrderItem();
            item.setProduct(product);
            item.setQuantity((Integer) productAndQuantity[i + 1]);
            item.setUnitPrice(product.getPrice());
            order.addItem(item);
        }
        return em.persist(order);
    }

    protected void flushAndClear() {
        em.flush();
        em.clear();
    }
}
