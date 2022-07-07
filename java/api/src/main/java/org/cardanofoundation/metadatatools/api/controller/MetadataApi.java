package org.cardanofoundation.metadatatools.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.cardanofoundation.metadatatools.api.model.rest.BatchRequest;
import org.cardanofoundation.metadatatools.api.model.rest.BatchResponse;
import org.cardanofoundation.metadatatools.api.model.rest.Property;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;

@Validated
@Tag(name = "v1-api", description = "The Cardano offchain metadata API")
public interface MetadataApi {

    default MetadataApiDelegate getDelegate() {
        return new MetadataApiDelegate() {};
    }

    /**
     * GET /metadata/{subject} : Query all properties of a single subject.
     *
     * @param subject  (required)
     * @return  (status code 200)
     *         or &#x60;subject&#x60; not found (status code 404)
     */
    @Operation(
        operationId = "getAllPropertiesForSubject",
        summary = "Query all properties of the single subject specified by the given subject id.",
        responses = {
            @ApiResponse(responseCode = "200", description = "", content = @Content(mediaType = "application/json", schema = @Schema(implementation =  Property.class))),
            @ApiResponse(responseCode = "404", description = "`subject` not found")
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/metadata/{subject}",
        produces = { "application/json;charset=utf-8" }
    )
    default ResponseEntity<Property> getAllPropertiesForSubject(
        @Parameter(name = "subject", description = "", required = true, schema = @Schema(description = "")) @PathVariable("subject") final String subject
    ) {
        return getDelegate().getAllPropertiesForSubject(subject);
    }


    /**
     * GET /metadata/{subject}/properties/{properties} : Query a single property of a single subject
     *
     * @param subject  (required)
     * @param properties  (required)
     * @return  (status code 200)
     *         or &#x60;subject&#x60; or &#x60;properties&#x60; not found (status code 404)
     */
    @Operation(
        operationId = "getPropertyForSubject",
        summary = "Query a specific property of a single subject specified by the given subject id.",
        responses = {
            @ApiResponse(responseCode = "200", description = "", content = @Content(mediaType = "application/json", schema = @Schema(implementation =  Property.class))),
            @ApiResponse(responseCode = "404", description = "`subject` or `properties` not found")
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/metadata/{subject}/properties/{properties}",
        produces = { "application/json;charset=utf-8" }
    )
    default ResponseEntity<Property> getPropertyForSubject(
        @Parameter(name = "subject", description = "", required = true, schema = @Schema(description = "")) @PathVariable("subject") final String subject,
        @Parameter(name = "properties", description = "", required = true, schema = @Schema(description = "")) @PathVariable("properties") final String properties
    ) {
        return getDelegate().getPropertyForSubject(subject, properties);
    }


    /**
     * POST /metadata/query : Batch metadata query
     *
     * @param body  (required)
     * @return  (status code 200)
     *         or Invalid &#x60;body&#x60; (status code 400)
     */
    @Operation(
        operationId = "getSubjects",
        summary = "Query multiple properties of multiple subjects.",
        responses = {
            @ApiResponse(responseCode = "200", description = "", content = @Content(mediaType = "application/json", schema = @Schema(implementation =  BatchResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid `body`")
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/metadata/query",
        produces = { "application/json;charset=utf-8" },
        consumes = { "application/json;charset=utf-8" }
    )
    default ResponseEntity<BatchResponse> getSubjects(
        @Parameter(name = "body", description = "", required = true, schema = @Schema(description = "")) @Valid @RequestBody final BatchRequest body
    ) {
        return getDelegate().getSubjects(body);
    }
}
