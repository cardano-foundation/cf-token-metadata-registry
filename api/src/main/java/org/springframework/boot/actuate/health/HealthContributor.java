package org.springframework.boot.actuate.health;

/**
 * Binary-compatibility stand-in for the Spring Boot 3 health contributor type.
 * yaci-store 2.0.7 implements this interface. Spring Boot 4 moved the real type to
 * {@code org.springframework.boot.health.contributor.HealthContributor}.
 */
public interface HealthContributor {
}
