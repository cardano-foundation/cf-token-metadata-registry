package org.cardanofoundation.tokenmetadata.registry.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.PolicyBatchRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.PolicyResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface PolicyApi {

    @Operation(operationId = "getPolicy",
            summary = "Look up a policy: returns all tokens and programmable token status for the given minting policy")
    @GetMapping(path = "/policies/{policyId}", produces = "application/json;charset=utf-8")
    ResponseEntity<PolicyResponse> getPolicy(
            @Parameter(description = "the minting policy ID (56 hex characters)")
            @PathVariable("policyId") String policyId);

    @Operation(operationId = "queryPolicies",
            summary = "Batch lookup of policies: returns tokens and programmable token status for each policy")
    @PostMapping(path = "/policies/query",
            produces = "application/json;charset=utf-8",
            consumes = "application/json;charset=utf-8")
    ResponseEntity<List<PolicyResponse>> queryPolicies(
            @Valid @RequestBody PolicyBatchRequest body);

}
