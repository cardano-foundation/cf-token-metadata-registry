package org.cardanofoundation.tokenmetadata.registry.api.health;

import com.bloxbean.cardano.yaci.store.common.domain.HealthStatus;
import com.bloxbean.cardano.yaci.store.core.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Health indicator for on-chain token sync via Yaci indexer.
 * Covers CIP-68, CIP-113, and future on-chain token standards.
 * Uses Yaci Store's HealthService to check N2N connection and block sync status.
 */
@Component
@RequiredArgsConstructor
public class OnchainSyncHealthIndicator implements HealthIndicator {

    private final HealthService healthService;

    @Override
    public Health health() {
        HealthStatus status = healthService.getHealthStatus();

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

        return builder.up()
                .withDetail("syncStatus", "Syncing")
                .build();
    }

}
