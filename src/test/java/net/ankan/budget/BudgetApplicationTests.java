package net.ankan.budget;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.jwt.secret=test_secret_key_for_unit_tests_min_32_chars",
    "app.jwt.expiration-ms=86400000"
})
class BudgetApplicationTests {

    @Test
    void contextLoads() {
    }
}
