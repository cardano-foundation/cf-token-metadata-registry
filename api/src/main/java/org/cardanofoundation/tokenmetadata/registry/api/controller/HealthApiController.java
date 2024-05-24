package org.cardanofoundation.tokenmetadata.registry.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.HealthResponse;
import org.cardanofoundation.tokenmetadata.registry.model.enums.SyncStatusEnum;
import org.cardanofoundation.tokenmetadata.registry.service.TokenMetadataSyncService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@CrossOrigin
@RequestMapping("${openapi.metadataServer.base-path:}")
@Slf4j
@RequiredArgsConstructor
public class HealthApiController implements HealthApi {

    private final TokenMetadataSyncService tokenMetadataSyncService;

    @Override
    public ResponseEntity<HealthResponse> getHealthStatus() {

        return new ResponseEntity<>(HealthResponse.builder()
                .synced(tokenMetadataSyncService.isInitialSyncDone())
                .syncStatus(tokenMetadataSyncService.getSyncStatusEnum().toString())
                .build(), HttpStatus.OK);
    }
}
