package org.cardanofoundation.tokenmetadata.registry.api.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

class ApplicationLivenessHealthIndicatorTest {

    private final ApplicationLivenessHealthIndicator livenessHealthIndicator = new ApplicationLivenessHealthIndicator();

    @Test
    void shouldReturnUp() {
        Health health = livenessHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

}
