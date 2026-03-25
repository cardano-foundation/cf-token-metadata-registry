package org.cardanofoundation.tokenmetadata.registry.api.health;

import com.bloxbean.cardano.yaci.store.common.domain.HealthStatus;
import com.bloxbean.cardano.yaci.store.core.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Liveness health indicator for on-chain sync — checks that the node connection
 * is alive and blocks are being received. Does NOT check sync progress, since
 * liveness runs in parallel with readiness and the pod must not be killed during
 * initial sync.
 */
@Component
@RequiredArgsConstructor
public class OnchainLivenessHealthIndicator implements HealthIndicator {

    private static final String DETAIL_SYNC_STATUS = "syncStatus";

    private final HealthService healthService;

    @Override
    public Health health() {
        HealthStatus status;
        try {
            status = healthService.getHealthStatus();
        } catch (NullPointerException e) {
            return Health.unknown()
                    .withDetail(DETAIL_SYNC_STATUS, "Block fetcher not initialized")
                    .build();
        }

        Health.Builder builder = new Health.Builder()
                .withDetail("connectionAlive", status.isConnectionAlive())
                .withDetail("receivingBlocks", status.isReceivingBlocks())
                .withDetail("error", status.isError());

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

        if (status.isScheduleToStop()) {
            return builder.outOfService()
                    .withDetail(DETAIL_SYNC_STATUS, "Scheduled to stop")
                    .build();
        }

        return builder.up()
                .withDetail(DETAIL_SYNC_STATUS, "Connected")
                .build();
    }

}
