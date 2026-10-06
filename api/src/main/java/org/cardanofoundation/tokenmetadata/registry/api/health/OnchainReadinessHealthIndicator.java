package org.cardanofoundation.tokenmetadata.registry.api.health;

import com.bloxbean.cardano.yaci.store.common.domain.HealthStatus;
import com.bloxbean.cardano.yaci.store.common.domain.SyncStatus;
import com.bloxbean.cardano.yaci.store.core.service.HealthService;
import com.bloxbean.cardano.yaci.store.core.service.SyncStatusService;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

/**
 * Readiness health indicator for on-chain sync — checks that the indexer is fully synced
 * to the chain tip (100%). Uses Yaci Store's upstream {@link SyncStatusService} for sync
 * status instead of a local back-port. Used by Kubernetes readiness probe to gate traffic
 * routing. A pod that is still catching up will not receive requests.
 *
 * <p>Only registered when Yaci Store's {@link HealthService} bean is present. In read-only mode
 * ({@code store.read-only-mode=true}), Yaci Store does not start its sync infrastructure and
 * does not register {@code HealthService}, so this indicator is skipped entirely.</p>
 */
@Component
@ConditionalOnBean(HealthService.class)
@RequiredArgsConstructor
public class OnchainReadinessHealthIndicator implements HealthIndicator {

    private static final String DETAIL_SYNC_STATUS = "syncStatus";

    private final HealthService healthService;
    private final SyncStatusService syncStatusService;

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

        SyncStatus syncStatus = syncStatusService.getSyncStatus();
        builder.withDetail("syncPercentage", String.format(Locale.US, "%.2f%%", syncStatus.syncPercentage()));

        if (!syncStatus.synced()) {
            return builder.outOfService()
                    .withDetail(DETAIL_SYNC_STATUS, "Syncing")
                    .build();
        }

        return builder.up()
                .withDetail(DETAIL_SYNC_STATUS, "Synced")
                .build();
    }

}
