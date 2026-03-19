package org.cardanofoundation.tokenmetadata.registry.api.health;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;

class OnchainSyncHealthIndicatorTest {

    private final OnchainSyncHealthIndicator onchainSyncHealthIndicator = new OnchainSyncHealthIndicator();

    @Test
    void shouldReturnUp() {
        Health health = onchainSyncHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsKey("syncStatus");
    }

}
