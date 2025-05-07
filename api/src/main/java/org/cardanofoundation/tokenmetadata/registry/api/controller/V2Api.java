package org.cardanofoundation.tokenmetadata.registry.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.cardanofoundation.tokenmetadata.registry.api.model.QueryPriority;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.BatchRequest;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.BatchResponse;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface V2Api {

    @Operation(operationId = "getSubject", summary = "Query either all or a subset of properties of a given subject")
    @GetMapping(path = "/subjects/{subject}", produces = {"application/json;charset=utf-8"})
    ResponseEntity<Response> getSubject(@Parameter(description = "the concatenation of policy id and asset name (if any) to query")
                                        @PathVariable("subject") final String subject,
                                        @Parameter(description = "the list of properties to be returned in the reponse, if none specified, all properties will be returned")
                                        @RequestParam(value = "property", required = false) final List<String> properties,
                                        @Parameter(description = "the CIP priority: if the same property is present in multiple standards, the one with highest priority is returned")
                                        @RequestParam(value = "query_priority", required = false) final List<QueryPriority> priorities,
                                        @Parameter(description = "whether all the CIP specific properties should be returned in the response. False by default")
                                        @RequestParam(value = "show_cips_details", defaultValue = "false", required = false) final Boolean showCipsDetails);


    @Operation(operationId = "getSubjects", summary = "Query either all or a subset of properties of the given subjects")
    @PostMapping(value = "/subjects/query", produces = {"application/json;charset=utf-8"}, consumes = {"application/json;charset=utf-8"})
    ResponseEntity<BatchResponse> getSubjects(@Parameter(name = "body", required = true, schema = @Schema) @Valid @RequestBody final BatchRequest body,
                                              @Parameter(description = "the CIP priority: if the same property is present in multiple standards, the one with highest priority is returned")
                                              @RequestParam(value = "query_priority", required = false) final List<QueryPriority> priorities,
                                              @Parameter(description = "whether all the CIP specific properties should be returned in the response. False by default")
                                              @RequestParam(value = "show_cips_details", defaultValue = "false", required = false) final Boolean showCipsDetails);


}







