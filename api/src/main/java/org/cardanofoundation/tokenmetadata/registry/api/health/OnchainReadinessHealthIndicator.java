package org.cardanofoundation.tokenmetadata.registry.api.health;

import com.bloxbean.cardano.yaci.store.common.domain.HealthStatus;
import com.bloxbean.cardano.yaci.store.core.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.cardanofoundation.tokenmetadata.registry.api.service.OnchainSyncStatusService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Readiness health indicator for on-chain sync — checks that the indexer is fully synced
 * to the chain tip (100%). Used by Kubernetes readiness probe to gate traffic routing.
 * A pod that is still catching up will not receive requests.
 */
@Component
@ConditionalOnBean(HealthService.class)
@RequiredArgsConstructor
public class OnchainReadinessHealthIndicator implements HealthIndicator {

    private static final String DETAIL_SYNC_STATUS = "syncStatus";

    private final HealthService healthService;
    private final OnchainSyncStatusService syncStatusService;

    @Override
    public Health health() {
        HealthStatus status;
        try {
            status = healthService.getHealthStatus();
        } catch (NullPointerException _) {
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
