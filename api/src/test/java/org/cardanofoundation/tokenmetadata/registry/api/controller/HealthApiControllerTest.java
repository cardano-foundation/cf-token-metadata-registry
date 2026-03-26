package org.cardanofoundation.tokenmetadata.registry.api.controller;

import org.cardanofoundation.tokenmetadata.registry.api.health.OffchainSyncHealthIndicator;
import org.cardanofoundation.tokenmetadata.registry.api.health.OnchainReadinessHealthIndicator;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.HealthResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HealthApiControllerTest {

    @Mock
    private OffchainSyncHealthIndicator offchainSyncHealthIndicator;

    @Mock
    private OnchainReadinessHealthIndicator onchainSyncHealthIndicator;

    @InjectMocks
    private HealthApiController healthApiController;

    @Test
    void bothUp_shouldReturnSyncedTrue() {
        when(offchainSyncHealthIndicator.health())
                .thenReturn(Health.up().withDetail("syncStatus", "Sync done").build());
        when(onchainSyncHealthIndicator.health())
                .thenReturn(Health.up().withDetail("syncStatus", "Healthy").build());

        ResponseEntity<HealthResponse> response = healthApiController.getHealthStatus();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSynced()).isTrue();
        assertThat(response.getBody().getSyncStatus()).isEqualTo("offchain: Sync done, onchain: Healthy");
    }

    @Test
    void offchainDown_shouldReturnSyncedFalse() {
        when(offchainSyncHealthIndicator.health())
                .thenReturn(Health.down().withDetail("syncStatus", "Error while syncing").build());
        when(onchainSyncHealthIndicator.health())
                .thenReturn(Health.up().withDetail("syncStatus", "Healthy").build());

        ResponseEntity<HealthResponse> response = healthApiController.getHealthStatus();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSynced()).isFalse();
    }

    @Test
    void onchainDown_shouldReturnSyncedFalse() {
        when(offchainSyncHealthIndicator.health())
                .thenReturn(Health.up().withDetail("syncStatus", "Sync done").build());
        when(onchainSyncHealthIndicator.health())
                .thenReturn(Health.down().withDetail("syncStatus", "Indexer unreachable").build());

        ResponseEntity<HealthResponse> response = healthApiController.getHealthStatus();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSynced()).isFalse();
    }

    @Test
    void bothDown_shouldReturnSyncedFalse() {
        when(offchainSyncHealthIndicator.health())
                .thenReturn(Health.down().withDetail("syncStatus", "Error while syncing").build());
        when(onchainSyncHealthIndicator.health())
                .thenReturn(Health.down().withDetail("syncStatus", "Indexer unreachable").build());

        ResponseEntity<HealthResponse> response = healthApiController.getHealthStatus();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSynced()).isFalse();
    }

    @Test
    void offchainOutOfService_shouldReturnSyncedFalse() {
        when(offchainSyncHealthIndicator.health())
                .thenReturn(Health.outOfService().withDetail("syncStatus", "Sync in progress").build());
        when(onchainSyncHealthIndicator.health())
                .thenReturn(Health.up().withDetail("syncStatus", "Healthy").build());

        ResponseEntity<HealthResponse> response = healthApiController.getHealthStatus();

        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSynced()).isFalse();
    }

}
