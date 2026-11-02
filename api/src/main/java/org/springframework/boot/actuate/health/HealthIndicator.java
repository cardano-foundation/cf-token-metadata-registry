package org.springframework.boot.actuate.health;

/**
 * Binary-compatibility stand-in for the Spring Boot 3 {@code HealthIndicator}.
 * yaci-store 2.0.7 implements this interface. Spring Boot 4 registers contributors of type
 * {@code org.springframework.boot.health.contributor.HealthIndicator} instead.
 */
@FunctionalInterface
public interface HealthIndicator extends HealthContributor {

    default Health getHealth(boolean includeDetails) {
        Health health = health();
        return includeDetails ? health : health.withoutDetails();
    }

    Health health();
}
