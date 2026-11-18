package market.cosmocats.api;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.liquibase.contexts=test")
@Import(TestcontainersConfiguration.class)
class CosmoCatsApplicationTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void contextLoadsAndAppliesAllMigrationsWithoutSeedData() {
        Integer changesets = jdbcTemplate.queryForObject("SELECT count(*) FROM databasechangelog", Integer.class);
        Integer seeded = jdbcTemplate.queryForObject("SELECT count(*) FROM category", Integer.class);

        assertThat(changesets).isEqualTo(9);
        assertThat(seeded).isZero();
    }
}
