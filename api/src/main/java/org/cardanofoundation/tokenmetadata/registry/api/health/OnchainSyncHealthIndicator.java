package org.cardanofoundation.tokenmetadata.registry.api.health;

import com.bloxbean.cardano.yaci.store.common.domain.HealthStatus;
import com.bloxbean.cardano.yaci.store.core.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.cardanofoundation.tokenmetadata.registry.api.service.OnchainSyncStatusService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for on-chain token sync via Yaci indexer.
 * Covers CIP-68, CIP-113, and future on-chain token standards.
 * Uses Yaci Store's HealthService for connection status and
 * OnchainSyncStatusService to verify the indexer is near the chain tip.
 */
@Component
@RequiredArgsConstructor
public class OnchainSyncHealthIndicator implements HealthIndicator {

    private static final String DETAIL_SYNC_STATUS = "syncStatus";

    private final HealthService healthService;
    private final OnchainSyncStatusService syncStatusService;

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
                    .withDetail(DETAIL_SYNC_STATUS, "Block fetcher not initialized")
                    .build();
        }

        Health.Builder builder = new Health.Builder()
                .withDetail("connectionAlive", status.isConnectionAlive())
                .withDetail("receivingBlocks", status.isReceivingBlocks())
                .withDetail("error", status.isError())
                .withDetail("timeSinceLastBlockMs", status.getTimeSinceLastBlock());

        if (status.isScheduleToStop()) {
            return builder.outOfService()
                    .withDetail(DETAIL_SYNC_STATUS, "Scheduled to stop")
                    .build();
        }

        if (status.isError() || !status.isConnectionAlive()) {
            return builder.down()
                    .withDetail(DETAIL_SYNC_STATUS, "Connection lost or sync error")
                    .build();
        }

        if (!status.isReceivingBlocks()) {
            return builder.outOfService()
                    .withDetail(DETAIL_SYNC_STATUS, "Not receiving blocks")
                    .build();
        }

        double syncPercentage = syncStatusService.getSyncPercentage();
        builder.withDetail("syncPercentage", String.format(java.util.Locale.US, "%.2f%%", syncPercentage));

        if (!syncStatusService.isSynced()) {
            return builder.outOfService()
                    .withDetail(DETAIL_SYNC_STATUS, "Syncing")
                    .build();
        }

        return builder.up()
                .withDetail(DETAIL_SYNC_STATUS, "Synced")
                .build();
    }

}
