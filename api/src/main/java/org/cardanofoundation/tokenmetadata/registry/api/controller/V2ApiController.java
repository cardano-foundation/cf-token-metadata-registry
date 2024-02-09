package org.cardanofoundation.tokenmetadata.registry.api.controller;

import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.metadatatools.core.cip26.MetadataCreator;
import org.cardanofoundation.metadatatools.core.cip26.ValidationResult;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.FetchMetadataResultSet;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.V1ApiMetadataIndexer;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.V2ApiMetadataIndexer;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Controller
@CrossOrigin(exposedHeaders = {"X-Total-Count"})
@RequestMapping("${openapi.metadataServer.base-path:}")
public class V2ApiController implements V2Api {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MIN_PAGE_SIZE = 1;
    private static final int MAX_PAGE_SIZE = 200;
    private static final Pattern HEXADECIMAL_PATTERN = Pattern.compile("\\p{XDigit}+");

    @Autowired
    private V1ApiMetadataIndexer v1ApiMetadataIndexer;

    @Autowired
    private V2ApiMetadataIndexer v2ApiMetadataIndexer;

    private static String sanitizeNetworkRequestParameter(final String network) {
        return network.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public ResponseEntity<TokenMetadata> getSubjectV2(final String subject, final String fields) {
        try {
            final Optional<TokenMetadata> queryResult;
            if (fields != null) {
                queryResult = v1ApiMetadataIndexer.findSubjectSelectProperties(subject, List.of(fields.trim().split(",")));
            } else {
                queryResult = v1ApiMetadataIndexer.findSubject(subject);
            }
            return queryResult
                    .map(tokenMetadata -> new ResponseEntity<>(tokenMetadata, HttpStatus.OK))
                    .orElseGet(() -> new ResponseEntity<>(HttpStatus.NO_CONTENT));
        } catch (final IllegalArgumentException e) {
            log.error("Not able to process request for single subject.", e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (final IllegalStateException e) {
            log.error("Could not process query result.", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static int pageSizeFromLimitQueryParam(final Integer limit) {
        return (limit == null)
                ? DEFAULT_PAGE_SIZE
                : Math.max(MIN_PAGE_SIZE, Math.min(limit, MAX_PAGE_SIZE));
    }

    @Override
    public ResponseEntity<SubjectsResponse> getSubjectsV2(
            final String fields,
            final String sortBy,
            final String name,
            final FilterOperand nameOp,
            final String ticker,
            final FilterOperand tickerOp,
            final String description,
            final FilterOperand descriptionOp,
            final String url,
            final FilterOperand urlOp,
            final String policy,
            final FilterOperand policyOp,
            final LocalDate updated,
            final FilterOperand updatedOp,
            final String updatedBy,
            final FilterOperand updatedbyOp,
            final Integer decimals,
            final FilterOperand decimalsOp,
            final String q,
            final String vkey,
            final Integer limit,
            final Long page,
            final String pivotId,
            final PivotDirection pivotDirection) {
        try {

            final int pageSize = pageSizeFromLimitQueryParam(limit);
            final PivotDirection pivotDirectionSanitized =
                    (pivotId != null && pivotDirection == null) ? PivotDirection.AFTER : pivotDirection;

            final FetchMetadataResultSet resultSet =
                    v2ApiMetadataIndexer.fetchMetadata(
                            fields,
                            sortBy,
                            name,
                            nameOp,
                            ticker,
                            tickerOp,
                            description,
                            descriptionOp,
                            url,
                            urlOp,
                            policy,
                            policyOp,
                            updated,
                            updatedOp,
                            updatedBy,
                            updatedbyOp,
                            decimals,
                            decimalsOp,
                            q,
                            vkey,
                            pageSize,
                            page,
                            pivotId,
                            pivotDirectionSanitized);
            if (resultSet.getTotalCount() == 0) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                final SubjectsResponse subjectsResponse = new SubjectsResponse();
                subjectsResponse.setSubjects(resultSet.getResults());

                final HttpHeaders headers = new HttpHeaders();
                headers.add("X-Total-Count", String.valueOf(resultSet.getTotalCount()));
                return new ResponseEntity<>(subjectsResponse, headers, HttpStatus.OK);
            }
        } catch (final IllegalArgumentException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (final IllegalStateException e) {
            log.error("Could not process query result.", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<TokenMetadata> postSubjectV2(final String subject, final TokenMetadata property) {
        // 1. verfiy data
        // 2. submit as PR to Github or where-ever
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    @Override
    public ResponseEntity<TokenMetadata> postSignaturesV2(final String subject, final TokenMetadata property) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    private boolean metadataHasRequiredFields(final TokenMetadata property) {
        return property.getSubject() != null
                && !property.getSubject().isBlank()
                && property.getName() != null
                && property.getName().getValue() != null
                && !property.getName().getValue().isBlank()
                && property.getDescription() != null
                && property.getDescription().getValue() != null
                && !property.getDescription().getValue().isBlank();
    }

    @Override
    public ResponseEntity verifySubjectV2(final String subject, final TokenMetadata metadata) {
        final Optional<TokenMetadata> baseMetadata = v1ApiMetadataIndexer.findSubject(subject);
        final ValidationResult validationResult;
        if (baseMetadata.isPresent()) {
            validationResult = MetadataCreator.validateMetadataUpdate(metadata.toCip26Metadata(),
                    null,
                    baseMetadata.get().toCip26Metadata());
        } else {
            validationResult = MetadataCreator.validateMetadata(metadata.toCip26Metadata());
        }

        if (validationResult.isValid()) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(new VerifyFailureResponse(validationResult.getValidationErrors()));
        }
    }

    @Override
    public ResponseEntity<TokenMetadata> deleteSubjectV2(final String subject,
                                                         final String signature,
                                                         final String vkey) {
        // 1. verify signature (which is sig(subject | "VOID")) with given vkey
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }

    private static List<String> sanitizeAddressHashes(@NotNull final List<String> walletAddressHashes) throws IllegalArgumentException {
        return walletAddressHashes.stream()
                .map(walletAddressHash -> {
                    if (walletAddressHash.trim().isEmpty()) {
                        throw new IllegalArgumentException("Wallet address hash cannot be empty.");
                    } else {
                        final Matcher matcher = HEXADECIMAL_PATTERN.matcher(walletAddressHash.trim());
                        if (!matcher.matches() || walletAddressHash.trim().length() >= 64) {
                            throw new IllegalArgumentException(
                                    "Wallet address hash is not a valid hex represented SHA-256 hash.");
                        } else {
                            return walletAddressHash.trim().toLowerCase(Locale.ROOT);
                        }
                    }
                })
                .collect(Collectors.toList());
    }

    @Override
    public ResponseEntity<WalletTrustCheckResponse> v2ForensicsWallet(final String walletAddressHash) {
        return v2ForensicsWallets(new WalletHashes(List.of(walletAddressHash)));
    }

    @Override
    public ResponseEntity<WalletTrustCheckResponse> v2ForensicsWallets(final WalletHashes walletAddressHashes) {
        try {
            final List<WalletFraudIncident> fraudIncidents = v2ApiMetadataIndexer.findScamIncidents(
                    sanitizeAddressHashes(walletAddressHashes.getAddressHashes()));
            if (fraudIncidents.isEmpty()) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                final WalletTrustCheckResponse response = new WalletTrustCheckResponse();
                response.setIncidents(fraudIncidents);
                return new ResponseEntity<>(response, HttpStatus.OK);
            }
        } catch (final IllegalArgumentException e) {
            log.error("Could not process request due to invalid arguments provided.", e);
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        } catch (final IllegalStateException e) {
            log.error("Could not process request due to an internal server error.", e);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
