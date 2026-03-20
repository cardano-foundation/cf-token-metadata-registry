package org.cardanofoundation.tokenmetadata.registry.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.health.OffchainSyncHealthIndicator;
import org.cardanofoundation.tokenmetadata.registry.api.health.OnchainSyncHealthIndicator;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.HealthResponse;
import org.springframework.boot.actuate.health.Status;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @deprecated Use /actuator/health/readiness instead. This endpoint will be removed in a future release.
 */
@Deprecated
@Controller
@CrossOrigin
@RequestMapping("${openapi.metadataServer.base-path:}")
@Slf4j
@RequiredArgsConstructor
public class HealthApiController implements HealthApi {

    private final OffchainSyncHealthIndicator offchainSyncHealthIndicator;
    private final OnchainSyncHealthIndicator onchainSyncHealthIndicator;

    @Override
    public ResponseEntity<HealthResponse> getHealthStatus() {
        var offchainHealth = offchainSyncHealthIndicator.health();
        var onchainHealth = onchainSyncHealthIndicator.health();

        boolean synced = Status.UP.equals(offchainHealth.getStatus())
                && Status.UP.equals(onchainHealth.getStatus());

        String syncStatus = "offchain: %s, onchain: %s".formatted(
                offchainHealth.getDetails().get("syncStatus"),
                onchainHealth.getDetails().get("syncStatus"));

        return new ResponseEntity<>(HealthResponse.builder()
                .synced(synced)
                .syncStatus(syncStatus)
                .build(), HttpStatus.OK);
    }

}
