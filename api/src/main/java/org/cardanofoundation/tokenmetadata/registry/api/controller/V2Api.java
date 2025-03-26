package org.cardanofoundation.tokenmetadata.registry.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Validated
public interface V2Api {

    /**
     * GET /v2/subjects/{subject} : Query all properties or specified properties of the
     * given subject
     *
     * @param subject (required)
     * @param properties  (optional)
     * @return The metadata related to the queried subject. (status code 200) or If the subject is
     * present in the metadata server but has no data for the given fields list. (status code 204)
     * or If the subject is not present in the metadata server. (status code 404)
     */
    @Operation(operationId = "getSubjectV2",
            summary = "Query all properties or specified properties of the given subject",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "The metadata related to the queried subject.",
                            content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenMetadata.class))),
                    @ApiResponse(responseCode = "204",
                            description = "If the subject is present in the metadata server but has not data for the given fields list."),
                    @ApiResponse(responseCode = "404", description = "If the subject is not present in the metadata server.")
            })
    @RequestMapping(method = RequestMethod.GET, value = "/v2/subjects/{subject}", produces = {"application/json;charset=utf-8"})
    ResponseEntity<TokenMetadata> getSubjectV2(@Parameter(name = "subject", required = true, schema = @Schema)
                                               @PathVariable("subject") final String subject,
                                               @Parameter(name = "properties", schema = @Schema)
                                               @Valid
                                               @RequestParam(value = "properties", required = false) final String properties);





}







