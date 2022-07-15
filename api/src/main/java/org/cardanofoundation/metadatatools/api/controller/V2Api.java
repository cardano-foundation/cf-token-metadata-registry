package org.cardanofoundation.metadatatools.api.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.cardanofoundation.metadatatools.api.model.rest.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Validated
@Tag(name = "v2-api", description = "The extended Cardano offchain metadata API")
public interface V2Api {
    /**
     * DELETE /v2/subjects/{subject} : Request deletion of the given metadata subject.
     *
     * @param subject   (required)
     * @param signature The hex string representation of the signature generated from the string &#39;VOID&#39; using the signing/private key that has been used for metadata signing. (required)
     * @param vkey      The hex string representation of the cbor encoded verification/public key that fits the signing/private key that has been used for signature creation. (required)
     * @return The metadata related to the queried subject. (status code 200)
     * or If the subject is present in the metadata server but has not data for the given fields list. (status code 204)
     * or If the subject is not present in the metadata server. (status code 404)
     */
    @Operation(operationId = "deleteSubjectV2", summary = "Request deletion of the given metadata subject.", responses = {@ApiResponse(responseCode = "200", description = "The metadata related to the queried subject.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenMetadata.class))), @ApiResponse(responseCode = "204", description = "If the subject is present in the metadata server but has not data for the given fields list."), @ApiResponse(responseCode = "404", description = "If the subject is not present in the metadata server.")})
    @RequestMapping(method = RequestMethod.DELETE, value = "/v2/subjects/{subject}", produces = {"application/json;charset=utf-8"})
    ResponseEntity<TokenMetadata> deleteSubjectV2(@Parameter(name = "subject", description = "", required = true, schema = @Schema(description = "")) @PathVariable("subject") final String subject, @NotNull @Parameter(name = "signature", description = "The hex string representation of the signature generated from the string 'VOID' using the signing/private key that has been used for metadata signing.", required = true, schema = @Schema(description = "")) @Valid @RequestParam(value = "signature", required = true) final String signature, @NotNull @Parameter(name = "vkey", description = "The hex string representation of the cbor encoded verification/public key that fits the signing/private key that has been used for signature creation.", required = true, schema = @Schema(description = "")) @Valid @RequestParam(value = "vkey", required = true) final String vkey);


    /**
     * GET /v2/health : health status of API
     *
     * @return Returned if service is healthy. (status code 200)
     * or Returned if service is not healthy. (status code 500)
     */
    @Operation(operationId = "getHealthV2", summary = "health status of API", responses = {@ApiResponse(responseCode = "200", description = "Returned if service is healthy."), @ApiResponse(responseCode = "500", description = "Returned if service is not healthy.")})
    @RequestMapping(method = RequestMethod.GET, value = "/v2/health")
    ResponseEntity<Void> getHealthV2();


    /**
     * GET /v2/subjects/{subject} : Query all properties or specified properties of the given subject
     *
     * @param subject (required)
     * @param fields  (optional)
     * @return The metadata related to the queried subject. (status code 200)
     * or If the subject is present in the metadata server but has not data for the given fields list. (status code 204)
     * or If the subject is not present in the metadata server. (status code 404)
     */
    @Operation(operationId = "getSubjectV2", summary = "Query all properties or specified properties of the given subject", responses = {@ApiResponse(responseCode = "200", description = "The metadata related to the queried subject.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenMetadata.class))), @ApiResponse(responseCode = "204", description = "If the subject is present in the metadata server but has not data for the given fields list."), @ApiResponse(responseCode = "404", description = "If the subject is not present in the metadata server.")})
    @RequestMapping(method = RequestMethod.GET, value = "/v2/subjects/{subject}", produces = {"application/json;charset=utf-8"})
    ResponseEntity<TokenMetadata> getSubjectV2(@Parameter(name = "subject", description = "", required = true, schema = @Schema(description = "")) @PathVariable("subject") final String subject, @Parameter(name = "fields", description = "", schema = @Schema(description = "")) @Valid @RequestParam(value = "fields", required = false) final String fields);


    /**
     * GET /v2/subjects : Query all subjects that meet the filter criteria.
     *
     * @param fields         (optional)
     * @param sortBy         (optional)
     * @param name           (optional)
     * @param nameOp         (optional)
     * @param ticker         (optional)
     * @param tickerOp       (optional)
     * @param description    (optional)
     * @param descriptionOp  (optional)
     * @param url            (optional)
     * @param urlOp          (optional)
     * @param policy         (optional)
     * @param policyOp       (optional)
     * @param updated        (optional)
     * @param updatedOp      (optional)
     * @param updatedBy      (optional)
     * @param updatedbyOp    (optional)
     * @param decimals       (optional)
     * @param decimalsOp     (optional)
     * @param q              Perform a fulltext search on the fields name, ticker, description, updated_by and url. (optional)
     * @param vkey           Retrieve entries that got signed by the key given identified by its hex CBOR representation of the public key part. (optional)
     * @param limit          Limits the size of the result set. Max &#x60;limit&#x60; entries will be returned. (optional)
     * @param page           The page that shall be displayed, meaning results are limited using a clause like `limit page offset (page)*limit`
     * @param pivotId        Used for pagination. Only records after or before the given pivot element will be returned. (optional)
     * @param pivotDirection Used for pagination. Specified if elements before or after the pivotId will be returned. (optional)
     * @return The metadata related to the queried subjects. (status code 200)
     * or If no metadata has been found that matches the given filter criteria. (status code 204)
     */
    @Operation(operationId = "getSubjectsV2", summary = "Query all subjects that meet the filter criteria.", responses = {@ApiResponse(responseCode = "200", description = "The metadata related to the queried subjects.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = SubjectsResponse.class))), @ApiResponse(responseCode = "204", description = "If no metadata has been found that matches the given filter criteria.")})
    @RequestMapping(method = RequestMethod.GET, value = "/v2/subjects", produces = {"application/json;charset=utf-8"})
    ResponseEntity<SubjectsResponse> getSubjectsV2(@Parameter(name = "fields", description = "", schema = @Schema(description = "")) @Valid @RequestParam(value = "fields", required = false) final String fields, @Parameter(name = "sort_by", description = "", schema = @Schema(description = "")) @Valid @RequestParam(value = "sort_by", required = false) final String sortBy, @Parameter(name = "name", description = "", schema = @Schema(description = "")) @Valid @RequestParam(value = "name", required = false) final String name, @Parameter(name = "name_op", description = "", schema = @Schema(description = "", allowableValues = {"eq", "neq", "lt", "lte", "gt", "gte"})) @Valid @RequestParam(value = "name_op", required = false) final FilterOperand nameOp, @Parameter(name = "ticker", description = "", schema = @Schema(description = "")) @Valid @RequestParam(value = "ticker", required = false) final String ticker, @Parameter(name = "ticker_op", description = "", schema = @Schema(description = "", allowableValues = {"eq", "neq", "lt", "lte", "gt", "gte"})) @Valid @RequestParam(value = "ticker_op", required = false) final FilterOperand tickerOp, @Parameter(name = "description", description = "", schema = @Schema(description = "")) @Valid @RequestParam(value = "description", required = false) final String description, @Parameter(name = "description_op", description = "", schema = @Schema(description = "", allowableValues = {"eq", "neq", "lt", "lte", "gt", "gte"})) @Valid @RequestParam(value = "description_op", required = false) final FilterOperand descriptionOp, @Parameter(name = "url", description = "", schema = @Schema(description = "")) @Valid @RequestParam(value = "url", required = false) final String url, @Parameter(name = "url_op", description = "", schema = @Schema(description = "", allowableValues = {"eq", "neq", "lt", "lte", "gt", "gte"})) @Valid @RequestParam(value = "url_op", required = false) final FilterOperand urlOp, @Parameter(name = "policy", description = "", schema = @Schema(description = "")) @Valid @RequestParam(value = "policy", required = false) final String policy, @Parameter(name = "policy_op", description = "", schema = @Schema(description = "", allowableValues = {"eq", "neq", "lt", "lte", "gt", "gte"})) @Valid @RequestParam(value = "policy_op", required = false) final FilterOperand policyOp, @Parameter(name = "updated", description = "", schema = @Schema(description = "")) @Valid @RequestParam(value = "updated", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) final LocalDate updated, @Parameter(name = "updated_op", description = "", schema = @Schema(description = "", allowableValues = {"eq", "neq", "lt", "lte", "gt", "gte"})) @Valid @RequestParam(value = "updated_op", required = false) final FilterOperand updatedOp, @Parameter(name = "updated_by", description = "", schema = @Schema(description = "")) @Valid @RequestParam(value = "updated_by", required = false) final String updatedBy, @Parameter(name = "updatedby_op", description = "", schema = @Schema(description = "", allowableValues = {"eq", "neq", "lt", "lte", "gt", "gte"})) @Valid @RequestParam(value = "updatedby_op", required = false) final FilterOperand updatedbyOp, @Parameter(name = "decimals", description = "", schema = @Schema(description = "")) @Valid @RequestParam(value = "decimals", required = false) final Integer decimals, @Parameter(name = "decimals_op", description = "", schema = @Schema(description = "", allowableValues = {"eq", "neq", "lt", "lte", "gt", "gte"})) @Valid @RequestParam(value = "decimals_op", required = false) final FilterOperand decimalsOp, @Parameter(name = "q", description = "Perform a fulltext search on the fields name, ticker, description, updated_by and url.", schema = @Schema(description = "")) @Valid @RequestParam(value = "q", required = false) final String q, @Parameter(name = "vkey", description = "Retrieve entries that got signed by the key given identified by its hex CBOR representation of the public key part.", schema = @Schema(description = "")) @Valid @RequestParam(value = "vkey", required = false) final String vkey, @Min(1) @Max(200) @Parameter(name = "limit", description = "Limits the size of the result set. Max `limit` entries will be returned.", schema = @Schema(description = "")) @Valid @RequestParam(value = "limit", required = false) final Integer limit, @Parameter(name = "page", description = "The page that shall be displayed, meaning results are limited using a clause like `limit page offset (page)*limit`", schema = @Schema(description = "")) @Valid @RequestParam(value = "page", required = false) final Long page, @Parameter(name = "pivot_id", description = "Used for pagination. Only records before or after the given pivot element will be returned.", schema = @Schema(description = "")) @Valid @RequestParam(value = "pivot_id", required = false) final String pivotId, @Parameter(name = "pivot_direction", description = "Used for pagination. Specified if elements before or after the pivotId will be returned.", schema = @Schema(description = "")) @Valid @RequestParam(value = "pivot_direction", required = false) final PivotDirection pivotDirection);


    /**
     * POST /v2/subjects/{subject} : Post a new token metadata submission.
     *
     * @param subject  (required)
     * @param property (required)
     * @return Returns the complete metadata after a successfull request. (status code 200)
     * or Returned if validation of the given metadata object failed. (status code 400)
     */
    @Operation(operationId = "postSubjectV2", summary = "Post a new token metadata submission.", responses = {@ApiResponse(responseCode = "200", description = "Returns the complete metadata after a successfull request.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenMetadata.class))), @ApiResponse(responseCode = "400", description = "Returned if validation of the given metadata object failed.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = VerifyFailureResponse.class)))})
    @RequestMapping(method = RequestMethod.POST, value = "/v2/subjects/{subject}", produces = {"application/json;charset=utf-8"}, consumes = {"application/json;charset=utf-8"})
    ResponseEntity<?> postSubjectV2(@Parameter(name = "subject", description = "", required = true, schema = @Schema(description = "")) @PathVariable("subject") final String subject, @Parameter(name = "Property", description = "", required = true, schema = @Schema(description = "")) @Valid @RequestBody final TokenMetadata property);

    /**
     * POST /v2/subjects/{subject}/sign : Sign properties of a subject. A property is identified by the property name and the sequence number.
     *
     * @param subject  (required)
     * @return Returns the complete metadata after a successful request. (status code 200)
     * or Returned if validation of the given metadata object failed. (status code 400)
     */
    @Operation(operationId = "postSignaturesV2", summary = "Post signatures for properties of a subject.", responses = {@ApiResponse(responseCode = "200", description = "Returns the complete metadata after a successful request.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = TokenMetadata.class))), @ApiResponse(responseCode = "400", description = "Returned if validation of the given metadata object failed.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = VerifyFailureResponse.class)))})
    @RequestMapping(method = RequestMethod.POST, value = "/v2/subjects/{subject}/signatures", produces = {"application/json;charset=utf-8"}, consumes = {"application/json;charset=utf-8"})
    ResponseEntity<TokenMetadata> postSignaturesV2(
            @Parameter(name = "subject", description = "", required = true, schema = @Schema(description = "")) @PathVariable("subject") final String subject,
            @Parameter(name = "signatures", description = "", required = true, schema = @Schema(description = "")) @Valid @RequestBody final TokenMetadata property);

    /**
     * GET /v2/forensics/wallet/{addresshash} : Check if there are any scam or other fraud incidents related to this SHA-256 wallet address hash.
     *
     * @param addresshash (required)
     * @return Detailed information about fraud incidents. (status code 200)
     * or If there are no fraud incidents related to this address hash. (status code 204)
     * or If anything internal went wrong. (status code 500)
     */
    @Hidden
    @Operation(operationId = "v2ForensicsWallet", summary = "Check if there are any scam or other fraud incidents related to this SHA-256 wallet address hash.", responses = {@ApiResponse(responseCode = "200", description = "Detailed information about fraud incidents.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WalletTrustCheckResponse.class))), @ApiResponse(responseCode = "204", description = "If there are no fraud incidents related to this address hash."), @ApiResponse(responseCode = "500", description = "If anything internal went wrong.")})
    @RequestMapping(method = RequestMethod.GET, value = "/v2/forensics/wallet/{addresshash}", produces = {"application/json;charset=utf-8"})
    ResponseEntity<WalletTrustCheckResponse> v2ForensicsWallet(@Parameter(name = "addresshash", description = "", required = true, schema = @Schema(description = "")) @PathVariable("addresshash") final String addresshash);


    /**
     * POST /v2/forensics/wallet : Check if there are any scam or other fraud incidents related to the given SHA-256 wallet address hashes.
     *
     * @param walletHashes (required)
     * @return Detailed information about fraud incidents related to the given address hashes. (status code 200)
     * or If there are no fraud incidents related to the given address hashes. (status code 204)
     * or If anything internal went wrong. (status code 500)
     */
    @Hidden
    @Operation(operationId = "v2ForensicsWallets", summary = "Check if there are any scam or other fraud incidents related to the given SHA-256 wallet address hashes.", responses = {@ApiResponse(responseCode = "200", description = "Detailed information about fraud incidents related to the given address hashes.", content = @Content(mediaType = "application/json", schema = @Schema(implementation = WalletTrustCheckResponse.class))), @ApiResponse(responseCode = "204", description = "If there are no fraud incidents related to the given address hashes."), @ApiResponse(responseCode = "500", description = "If anything internal went wrong.")})
    @RequestMapping(method = RequestMethod.POST, value = "/v2/forensics/wallet", produces = {"application/json;charset=utf-8"}, consumes = {"application/json;charset=utf-8"})
    ResponseEntity<WalletTrustCheckResponse> v2ForensicsWallets(@Parameter(name = "WalletHashes", description = "", required = true, schema = @Schema(description = "")) @Valid @RequestBody final WalletHashes walletHashes);

    /**
     * POST /v2/subjects/{subject}/verify : Verifies and validates the given metadata object.
     *
     * @param subject  (required)
     * @param tokenMetadata (required)
     * @return Returned on successful verification and validation. (status code 200)
     * or Returns a list of errors found during the validation (status code 400)
     */
    @Operation(operationId = "verifySubjectV2", summary = "Verifies and validates the given metadata object.", responses = {@ApiResponse(responseCode = "200", description = "Returned on successfull verification and validation."), @ApiResponse(responseCode = "400", description = "Returns a list of errors found during the validation", content = @Content(mediaType = "application/json", schema = @Schema(implementation = VerifyFailureResponse.class)))})
    @RequestMapping(method = RequestMethod.POST, value = "/v2/subjects/{subject}/verify", produces = {"application/json;charset=utf-8"}, consumes = {"application/json;charset=utf-8"})
    ResponseEntity<Void> verifySubjectV2(@Parameter(name = "subject", description = "", required = true, schema = @Schema(description = "")) @PathVariable("subject") final String subject, @Parameter(name = "Property", description = "", required = true, schema = @Schema(description = "")) @Valid @RequestBody final TokenMetadata tokenMetadata);
}
