package org.cardanofoundation.tokenmetadata.registry.api.health;

import lombok.RequiredArgsConstructor;
import org.cardanofoundation.tokenmetadata.registry.service.SyncStatus;
import org.cardanofoundation.tokenmetadata.registry.service.TokenMetadataSyncService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OffchainSyncHealthIndicator implements HealthIndicator {

    private final TokenMetadataSyncService tokenMetadataSyncService;

    @Override
    public Health health() {
        SyncStatus syncStatus = tokenMetadataSyncService.getSyncStatus();
        String statusText = syncStatus.getStatus().toString();

        return switch (syncStatus.getStatus()) {
            case SYNC_DONE, SYNC_IN_EXTRA_JOB -> Health.up()
                    .withDetail("syncStatus", statusText)
                    .build();
            case SYNC_IN_PROGRESS, SYNC_NOT_STARTED -> Health.outOfService()
                    .withDetail("syncStatus", statusText)
                    .build();
            case SYNC_ERROR -> Health.down()
                    .withDetail("syncStatus", statusText)
                    .build();
        };
    }

}
