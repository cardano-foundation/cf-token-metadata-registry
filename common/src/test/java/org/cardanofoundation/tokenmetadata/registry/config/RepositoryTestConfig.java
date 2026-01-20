package org.cardanofoundation.tokenmetadata.registry.config;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Reusable test configuration for JPA repository integration tests.
 * This configuration enables JPA repositories and entity scanning for the test context.
 */
@SpringBootApplication
@EnableJpaRepositories(basePackages = "org.cardanofoundation.tokenmetadata.registry.repository")
@EntityScan(basePackages = "org.cardanofoundation.tokenmetadata.registry.entity")
public class RepositoryTestConfig {
}
