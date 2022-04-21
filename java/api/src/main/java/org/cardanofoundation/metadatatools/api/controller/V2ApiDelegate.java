package org.cardanofoundation.metadatatools.api.controller;

import org.cardanofoundation.metadatatools.api.model.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.NativeWebRequest;

import java.time.LocalDate;
import java.util.Optional;

/**
 * A delegate to be called by the {@link V2ApiController}}.
 * Implement this interface with a {@link org.springframework.stereotype.Service} annotated class.
 */
public interface V2ApiDelegate {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    default ResponseEntity<Property> deleteSubjectV2(final String subject, final String signature, final String vkey) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    default ResponseEntity<Void> getHealthV2() {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    default ResponseEntity<Property> getSubjectV2(final String subject, final String fields) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    default ResponseEntity<SubjectsResponse> getSubjectsV2(final String fields,
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
                                                           final String afterId) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    default ResponseEntity<Property> postSubjectV2(final String subject, final Property property) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    default ResponseEntity<WalletTrustCheckResponse> v2ForensicsWallet(final String addresshash) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    default ResponseEntity<WalletTrustCheckResponse> v2ForensicsWallets(final WalletHashes walletHashes) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    default ResponseEntity<Void> verifySubjectV2(final String subject, final Property property) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }
}
