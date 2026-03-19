package org.cardanofoundation.tokenmetadata.registry.api.health;

import com.bloxbean.cardano.yaci.store.common.domain.HealthStatus;
import com.bloxbean.cardano.yaci.store.core.service.HealthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApplicationLivenessHealthIndicatorTest {

    @Mock
    private HealthService healthService;

    @InjectMocks
    private ApplicationLivenessHealthIndicator livenessHealthIndicator;

    @Test
    void healthyIndexer_shouldReturnUp() {
        when(healthService.getHealthStatus()).thenReturn(HealthStatus.builder()
                .isConnectionAlive(true)
                .isReceivingBlocks(true)
                .isError(false)
                .isScheduleToStop(false)
                .build());

        Health health = livenessHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("indexerStatus", "Healthy");
    }

    @Test
    void connectionLost_shouldReturnDown() {
        when(healthService.getHealthStatus()).thenReturn(HealthStatus.builder()
                .isConnectionAlive(false)
                .isReceivingBlocks(false)
                .isError(false)
                .isScheduleToStop(false)
                .build());

        Health health = livenessHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("connectionAlive", false);
    }

    @Test
    void syncError_shouldReturnDown() {
        when(healthService.getHealthStatus()).thenReturn(HealthStatus.builder()
                .isConnectionAlive(true)
                .isReceivingBlocks(true)
                .isError(true)
                .isScheduleToStop(false)
                .build());

        Health health = livenessHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("error", true);
    }

    @Test
    void scheduledToStop_shouldReturnUp() {
        when(healthService.getHealthStatus()).thenReturn(HealthStatus.builder()
                .isConnectionAlive(false)
                .isReceivingBlocks(false)
                .isError(false)
                .isScheduleToStop(true)
                .build());

        Health health = livenessHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("indexerStatus", "Scheduled to stop");
    }

}
