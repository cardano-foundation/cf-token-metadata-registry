package org.cardanofoundation.tokenmetadata.registry.api;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@JdbcTest
@ActiveProfiles("test")
class TokenMetadataRegistryApplicationTests {

    @MockBean
    private EntityManager entityManager;
  
    @Test
    void contextLoads() {
    }
}
