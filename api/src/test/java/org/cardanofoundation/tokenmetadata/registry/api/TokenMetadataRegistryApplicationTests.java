package org.cardanofoundation.tokenmetadata.registry.api;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@JdbcTest
@ActiveProfiles("test")
class TokenMetadataRegistryApplicationTests {

    // Spring Data registers this shared EntityManager under a fixed name. Replacing that
    // definition avoids a lookup of the real entityManagerFactory, which this slice does not start.
    @MockitoBean(name = "jpaSharedEM_entityManagerFactory")
    private EntityManager entityManager;

    @Test
    void contextLoads() {
        assertNotNull(entityManager);
    }
}
