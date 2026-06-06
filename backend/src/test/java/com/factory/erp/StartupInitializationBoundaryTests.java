package com.factory.erp;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:sqlite:file:startup-boundary-test?mode=memory&cache=shared",
        "spring.sql.init.mode=always",
        "spring.sql.init.schema-locations=classpath:db/startup-boundary-schema.sql",
        "spring.sql.init.data-locations=classpath:db/startup-boundary-data.sql"
})
class StartupInitializationBoundaryTests {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void startupLoadsExistingDatabaseWithoutResettingUsersOrSeedingInventory() {
        Integer customUserCount = jdbcTemplate.queryForObject(
                "select count(*) from users where username = ?",
                Integer.class,
                "existing"
        );
        Integer defaultUserCount = jdbcTemplate.queryForObject(
                "select count(*) from users where username in ('admin', 'warehouse', 'sales')",
                Integer.class
        );
        Integer productCount = jdbcTemplate.queryForObject("select count(*) from products", Integer.class);
        Integer inventoryCount = jdbcTemplate.queryForObject("select count(*) from inventory", Integer.class);

        assertThat(customUserCount).isEqualTo(1);
        assertThat(defaultUserCount).isZero();
        assertThat(productCount).isZero();
        assertThat(inventoryCount).isZero();
    }
}
