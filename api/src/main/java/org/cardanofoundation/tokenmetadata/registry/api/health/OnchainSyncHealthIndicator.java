package org.cardanofoundation.tokenmetadata.registry.api.health;

import com.bloxbean.cardano.yaci.store.common.domain.HealthStatus;
import com.bloxbean.cardano.yaci.store.common.domain.SyncStatus;
import com.bloxbean.cardano.yaci.store.core.service.HealthService;
import com.bloxbean.cardano.yaci.store.core.service.SyncStatusService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for on-chain token sync via Yaci indexer.
 * Covers CIP-68, CIP-113, and future on-chain token standards.
 * Uses Yaci Store's HealthService for connection status and
 * SyncStatusService to verify the indexer is near the chain tip.
 */
@Component
@RequiredArgsConstructor
public class OnchainSyncHealthIndicator implements HealthIndicator {

    private final HealthService healthService;
    private final SyncStatusService syncStatusService;

    @Override
    public Health health() {
        HealthStatus status;
        try {
            status = healthService.getHealthStatus();
        } catch (NullPointerException e) {
            // Workaround for yaci-store bug: BlockRangeSync.isRunning() throws NPE
            // when blockFetcher is null (node not connected yet).
            // See: BlockRangeSync.java:99 in yaci-helper.
            // TODO: remove once fixed upstream in yaci-store/yaci-helper
            return Health.unknown()
                    .withDetail("syncStatus", "Block fetcher not initialized")
                    .build();
        }

        Health.Builder builder = new Health.Builder()
                .withDetail("connectionAlive", status.isConnectionAlive())
                .withDetail("receivingBlocks", status.isReceivingBlocks())
                .withDetail("error", status.isError())
                .withDetail("timeSinceLastBlockMs", status.getTimeSinceLastBlock());

        if (status.isScheduleToStop()) {
            return builder.outOfService()
                    .withDetail("syncStatus", "Scheduled to stop")
                    .build();
        }

        if (status.isError() || !status.isConnectionAlive()) {
            return builder.down()
                    .withDetail("syncStatus", "Connection lost or sync error")
                    .build();
        }

        if (!status.isReceivingBlocks()) {
            return builder.outOfService()
                    .withDetail("syncStatus", "Not receiving blocks")
                    .build();
        }

        SyncStatus syncStatus = syncStatusService.getSyncStatus();
        builder.withDetail("syncPercentage", String.format("%.2f%%", syncStatus.syncPercentage()));

        if (!syncStatus.synced()) {
            return builder.outOfService()
                    .withDetail("syncStatus", "Syncing")
                    .build();
        }

        return builder.up()
                .withDetail("syncStatus", "Synced")
                .build();
    }

}
