package org.cardanofoundation.tokenmetadata.registry.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.Cip113BatchRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.Cip113RegistryEntry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface Cip113Api {

    @Operation(operationId = "getCip113RegistryEntry",
            summary = "Look up a single CIP-113 programmable token by policy ID")
    @GetMapping(path = "/registry/{policyId}", produces = "application/json;charset=utf-8")
    ResponseEntity<Cip113RegistryEntry> getRegistryEntry(
            @Parameter(description = "the policy ID of the programmable token")
            @PathVariable("policyId") String policyId);

    @Operation(operationId = "queryCip113Registry",
            summary = "Batch lookup of CIP-113 programmable tokens by policy IDs")
    @PostMapping(path = "/registry/query",
            produces = "application/json;charset=utf-8",
            consumes = "application/json;charset=utf-8")
    ResponseEntity<List<Cip113RegistryEntry>> queryRegistry(
            @Valid @RequestBody Cip113BatchRequest body);

}
