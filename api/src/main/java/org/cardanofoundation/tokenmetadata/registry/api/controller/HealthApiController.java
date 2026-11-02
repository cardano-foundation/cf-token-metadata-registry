package org.cardanofoundation.tokenmetadata.registry.api.controller;

import com.bloxbean.cardano.yaci.store.core.health.NodeSyncIndicator;
import jakarta.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.health.OffchainSyncHealthIndicator;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.HealthResponse;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
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
     * Nullable because {@link NodeSyncIndicator} is conditional on Yaci Store's
     * {@code HealthService} bean, which is not registered in read-only mode
     * ({@code store.read-only-mode=true}). When null, the legacy health endpoint reports
     * on-chain sync as "disabled" instead of failing.
     */
    @Nullable
    private final NodeSyncIndicator onchainSyncHealthIndicator;

    public HealthApiController(OffchainSyncHealthIndicator offchainSyncHealthIndicator,
                               @Nullable NodeSyncIndicator onchainSyncHealthIndicator) {
        this.offchainSyncHealthIndicator = offchainSyncHealthIndicator;
        this.onchainSyncHealthIndicator = onchainSyncHealthIndicator;
    }

    @Override
    public ResponseEntity<HealthResponse> getHealthStatus() {
        Health offchainHealth = offchainSyncHealthIndicator.health();
        // yaci-store 2.0.7 still returns the Boot 3 health type. Boot 4 moved Health to
        // org.springframework.boot.health.contributor, so the two results are compared by status code.
        boolean onchainUp;
        Object onchainDetail;
        if (onchainSyncHealthIndicator != null) {
            org.springframework.boot.actuate.health.Health onchainHealth = onchainSyncHealthIndicator.health();
            onchainUp = org.springframework.boot.actuate.health.Status.UP.equals(onchainHealth.getStatus());
            onchainDetail = onchainHealth.getDetails().get(SYNC_STATUS);
        } else {
            onchainUp = true;
            onchainDetail = "On-chain sync disabled (read-only mode)";
        }

        boolean synced = Status.UP.equals(offchainHealth.getStatus()) && onchainUp;

        String syncStatus = "offchain: %s, onchain: %s".formatted(
                offchainHealth.getDetails().get(SYNC_STATUS),
                onchainDetail);

        return new ResponseEntity<>(HealthResponse.builder()
                .synced(synced)
                .syncStatus(syncStatus)
                .build(), HttpStatus.OK);
    }

}
