package org.cardanofoundation.tokenmetadata.registry.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.cardanofoundation.tokenmetadata.registry.api.model.validate.Cip113ValidateRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.validate.Cip26ValidateRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.validate.Cip68ValidateRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.validate.ValidateResponse;
import org.cardanofoundation.tokenmetadata.registry.api.service.TokenValidationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Validates a token before it exists: what the registry would do with a CIP-26 registry entry, a CIP-68 reference NFT
 * datum or a CIP-113 registry node, and how the V2 API would serve it (ADR-023). Nothing is stored or read from the
 * index. Switched on and off with {@code registry.validate.enabled}, checked at run time so that it also works in a
 * native image.
 */
@RestController
@RequestMapping("/api/v2/validate")
@RequiredArgsConstructor
@Tag(name = "Validate", description = "Check token metadata before you mint or submit it: what the registry would store, "
        + "with which validation errors, and how the V2 API would serve it. Nothing is stored.")
public class ValidateApiController {

    private final TokenValidationService tokenValidationService;

    @Value("${registry.validate.enabled:true}")
    private boolean enabled;

    @Operation(operationId = "validateCip26", summary = "Check a CIP-26 registry entry",
            description = "The JSON file you would submit to the token registry. Signatures are not verified.")
    @PostMapping(path = "/cip26", consumes = "application/json", produces = "application/json")
    public ValidateResponse validateCip26(@RequestBody Cip26ValidateRequest request) {
        requireEnabled();
        return tokenValidationService.validateCip26(request);
    }

    @Operation(operationId = "validateCip68", summary = "Check a CIP-68 reference NFT datum",
            description = "Give the datum as CBOR hex, or the token's fields to build it from (the built datum is returned). "
                    + "The labels say which user tokens it is for: 333 (fungible) and/or 444 (rich fungible).")
    @PostMapping(path = "/cip68", consumes = "application/json", produces = "application/json")
    public ValidateResponse validateCip68(@RequestBody Cip68ValidateRequest request) {
        requireEnabled();
        return tokenValidationService.validateCip68(request);
    }

    @Operation(operationId = "validateCip113", summary = "Check a CIP-113 registry node datum",
            description = "Whether the registry can read the node, and the extension V2 would add to the programmable token.")
    @PostMapping(path = "/cip113", consumes = "application/json", produces = "application/json")
    public ValidateResponse validateCip113(@RequestBody Cip113ValidateRequest request) {
        requireEnabled();
        return tokenValidationService.validateCip113(request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    private void requireEnabled() {
        if (!enabled) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
