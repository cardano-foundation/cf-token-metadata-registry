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
class OnchainLivenessHealthIndicatorTest {

    @Mock
    private HealthService healthService;

    @InjectMocks
    private OnchainLivenessHealthIndicator indicator;

    private HealthStatus healthyStatus() {
        return HealthStatus.builder()
                .isConnectionAlive(true)
                .isReceivingBlocks(true)
                .isError(false)
                .isScheduleToStop(false)
                .timeSinceLastBlock(1000)
                .build();
    }

    @Test
    void connectedAndReceiving_shouldReturnUp() {
        when(healthService.getHealthStatus()).thenReturn(healthyStatus());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Connected");
    }

    @Test
    void connectionLost_shouldReturnDown() {
        when(healthService.getHealthStatus()).thenReturn(HealthStatus.builder()
                .isConnectionAlive(false)
                .isReceivingBlocks(false)
                .isError(false)
                .isScheduleToStop(false)
                .timeSinceLastBlock(60000)
                .build());

        Health health = indicator.health();

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
                .timeSinceLastBlock(1000)
                .build());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("error", true);
    }

    @Test
    void notReceivingBlocks_shouldReturnOutOfService() {
        when(healthService.getHealthStatus()).thenReturn(HealthStatus.builder()
                .isConnectionAlive(true)
                .isReceivingBlocks(false)
                .isError(false)
                .isScheduleToStop(false)
                .timeSinceLastBlock(120000)
                .build());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Not receiving blocks");
    }

    @Test
    void blockFetcherNotInitialized_shouldReturnUnknown() {
        when(healthService.getHealthStatus()).thenThrow(new NullPointerException("blockFetcher is null"));

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UNKNOWN);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Block fetcher not initialized");
    }

    @Test
    void scheduledToStop_shouldReturnOutOfService() {
        when(healthService.getHealthStatus()).thenReturn(HealthStatus.builder()
                .isConnectionAlive(true)
                .isReceivingBlocks(true)
                .isError(false)
                .isScheduleToStop(true)
                .timeSinceLastBlock(1000)
                .build());

        Health health = indicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Scheduled to stop");
    }

}
