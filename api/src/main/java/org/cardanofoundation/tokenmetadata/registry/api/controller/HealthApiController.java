package org.cardanofoundation.tokenmetadata.registry.api.controller;

import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.health.OffchainSyncHealthIndicator;
import org.cardanofoundation.tokenmetadata.registry.api.health.OnchainReadinessHealthIndicator;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.HealthResponse;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @deprecated Use /actuator/health/readiness instead. This endpoint will be removed in a future release.
 */
@Deprecated(since = "1.5.0", forRemoval = true)
@Controller
@CrossOrigin
@RequestMapping("${openapi.metadataServer.base-path:}")
@Slf4j
public class HealthApiController implements HealthApi {

    public static final String SYNC_STATUS = "syncStatus";

    private final OffchainSyncHealthIndicator offchainSyncHealthIndicator;

    /**
     * Nullable because {@link OnchainReadinessHealthIndicator} is conditional on Yaci Store's
     * {@code HealthService} bean, which is not registered in read-only mode
     * ({@code store.read-only-mode=true}). When null, the legacy health endpoint reports
     * on-chain sync as "disabled" instead of failing.
     */
    @Nullable
    private final OnchainReadinessHealthIndicator onchainSyncHealthIndicator;

    public HealthApiController(OffchainSyncHealthIndicator offchainSyncHealthIndicator,
                               @Nullable OnchainReadinessHealthIndicator onchainSyncHealthIndicator) {
        this.offchainSyncHealthIndicator = offchainSyncHealthIndicator;
        this.onchainSyncHealthIndicator = onchainSyncHealthIndicator;
    }

    @Override
    public ResponseEntity<HealthResponse> getHealthStatus() {
        Health offchainHealth = offchainSyncHealthIndicator.health();
        Health onchainHealth = onchainSyncHealthIndicator != null
                ? onchainSyncHealthIndicator.health()
                : Health.up().withDetail(SYNC_STATUS, "On-chain sync disabled (read-only mode)").build();

        boolean synced = Status.UP.equals(offchainHealth.getStatus())
                && Status.UP.equals(onchainHealth.getStatus());

        String syncStatus = "offchain: %s, onchain: %s".formatted(
                offchainHealth.getDetails().get(SYNC_STATUS),
                onchainHealth.getDetails().get(SYNC_STATUS));

        return new ResponseEntity<>(HealthResponse.builder()
                .synced(synced)
                .syncStatus(syncStatus)
                .build(), HttpStatus.OK);
    }

}
