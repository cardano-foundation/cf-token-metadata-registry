package org.cardanofoundation.tokenmetadata.registry.api;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@JdbcTest
@ActiveProfiles("test")
@SuppressWarnings("java:S5738") // @MockBean deprecated — @MockitoBean cannot replace it here (EntityManager needs early registration)
class TokenMetadataRegistryApplicationTests {

    @MockBean
    private EntityManager entityManager;

    @Test
    void contextLoads() {
        assertNotNull(entityManager);
    }
}
