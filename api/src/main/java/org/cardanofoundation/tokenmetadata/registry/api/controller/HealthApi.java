package org.cardanofoundation.tokenmetadata.registry.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * @deprecated Use /actuator/health/readiness instead. This endpoint will be removed in a future release.
 */
@Deprecated(since = "1.5.0", forRemoval = true)
@Validated
@Tag(name = "health", description = "Health Endpoint for the cardano token metadata registry")
public interface HealthApi {

    @Deprecated(since = "1.5.0", forRemoval = true)
    @Operation(operationId = "getHealthStatus", summary = "Returns health status of service including if the initial sync is done", deprecated = true, responses = {
            @ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = HealthResponse.class)))
    })
    @GetMapping(value = "/health", produces = {"application/json;charset=utf-8"})
    ResponseEntity<HealthResponse> getHealthStatus();

}
