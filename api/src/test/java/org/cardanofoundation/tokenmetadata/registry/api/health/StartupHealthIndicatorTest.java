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
class StartupHealthIndicatorTest {

    @Mock
    private HealthService healthService;

    @InjectMocks
    private StartupHealthIndicator startupHealthIndicator;

    @Test
    void connectedAndReceivingBlocks_shouldReturnUp() {
        when(healthService.getHealthStatus()).thenReturn(HealthStatus.builder()
                .isConnectionAlive(true)
                .isReceivingBlocks(true)
                .isError(false)
                .isScheduleToStop(false)
                .timeSinceLastBlock(1000)
                .build());

        Health health = startupHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("connectionAlive", true);
        assertThat(health.getDetails()).containsEntry("receivingBlocks", true);
    }

    @Test
    void connectionNotAlive_shouldReturnDown() {
        when(healthService.getHealthStatus()).thenReturn(HealthStatus.builder()
                .isConnectionAlive(false)
                .isReceivingBlocks(false)
                .isError(false)
                .isScheduleToStop(false)
                .timeSinceLastBlock(60000)
                .build());

        Health health = startupHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("reason", "Yaci Store connection not alive");
    }

    @Test
    void notReceivingBlocks_shouldReturnDown() {
        when(healthService.getHealthStatus()).thenReturn(HealthStatus.builder()
                .isConnectionAlive(true)
                .isReceivingBlocks(false)
                .isError(false)
                .isScheduleToStop(false)
                .timeSinceLastBlock(120000)
                .build());

        Health health = startupHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("reason", "Not receiving blocks from node");
    }

    @Test
    void scheduledToStop_shouldReturnDown() {
        when(healthService.getHealthStatus()).thenReturn(HealthStatus.builder()
                .isConnectionAlive(true)
                .isReceivingBlocks(true)
                .isError(false)
                .isScheduleToStop(true)
                .timeSinceLastBlock(1000)
                .build());

        Health health = startupHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("reason", "Sync scheduled to stop");
    }

    @Test
    void blockFetcherNotInitialized_shouldReturnDown() {
        when(healthService.getHealthStatus()).thenThrow(new NullPointerException("blockFetcher is null"));

        Health health = startupHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("reason", "Block fetcher not initialized");
    }

}
