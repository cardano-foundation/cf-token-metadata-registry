package org.cardanofoundation.tokenmetadata.registry.api.health;

import org.cardanofoundation.tokenmetadata.registry.model.enums.SyncStatusEnum;
import org.cardanofoundation.tokenmetadata.registry.service.SyncStatus;
import org.cardanofoundation.tokenmetadata.registry.service.TokenMetadataSyncService;
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
class OffchainSyncHealthIndicatorTest {

    @Mock
    private TokenMetadataSyncService tokenMetadataSyncService;

    @InjectMocks
    private OffchainSyncHealthIndicator offchainSyncHealthIndicator;

    @Test
    void syncDone_shouldReturnUp() {
        when(tokenMetadataSyncService.getSyncStatus())
                .thenReturn(SyncStatus.builder().isInitialSyncDone(true).syncStatus(SyncStatusEnum.SYNC_DONE).build());

        Health health = offchainSyncHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Sync done");
    }

    @Test
    void syncInExtraJob_shouldReturnUp() {
        when(tokenMetadataSyncService.getSyncStatus())
                .thenReturn(SyncStatus.builder().isInitialSyncDone(true).syncStatus(SyncStatusEnum.SYNC_IN_EXTRA_JOB).build());

        Health health = offchainSyncHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.UP);
    }

    @Test
    void syncInProgress_shouldReturnOutOfService() {
        when(tokenMetadataSyncService.getSyncStatus())
                .thenReturn(SyncStatus.builder().isInitialSyncDone(false).syncStatus(SyncStatusEnum.SYNC_IN_PROGRESS).build());

        Health health = offchainSyncHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Sync in progress");
    }

    @Test
    void syncNotStarted_shouldReturnOutOfService() {
        when(tokenMetadataSyncService.getSyncStatus())
                .thenReturn(SyncStatus.builder().isInitialSyncDone(false).syncStatus(SyncStatusEnum.SYNC_NOT_STARTED).build());

        Health health = offchainSyncHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.OUT_OF_SERVICE);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Sync not started");
    }

    @Test
    void syncError_shouldReturnDown() {
        when(tokenMetadataSyncService.getSyncStatus())
                .thenReturn(SyncStatus.builder().isInitialSyncDone(false).syncStatus(SyncStatusEnum.SYNC_ERROR).build());

        Health health = offchainSyncHealthIndicator.health();

        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsEntry("syncStatus", "Error while syncing");
    }

}
