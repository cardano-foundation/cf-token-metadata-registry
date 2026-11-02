package org.springframework.boot.autoconfigure.jooq;

import org.jooq.impl.DefaultConfiguration;

/**
 * Binary-compatibility stand-in for Spring Boot 3's jOOQ customizer.
 * yaci-store 2.0.7 exposes a bean of this type. Spring Boot 4 moved the interface to
 * {@code org.springframework.boot.jooq.autoconfigure.DefaultConfigurationCustomizer}.
 */
@FunctionalInterface
public interface DefaultConfigurationCustomizer {

    void customize(DefaultConfiguration configuration);
}
