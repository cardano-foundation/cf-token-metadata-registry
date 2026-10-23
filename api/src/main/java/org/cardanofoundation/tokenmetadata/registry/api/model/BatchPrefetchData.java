package org.cardanofoundation.tokenmetadata.registry.api.model;

import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.ProgrammableTokenCip113;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;

import java.util.List;
import java.util.Map;

/**
 * What a batch query reads up front, so that each subject is resolved without a query of its own.
 *
 * @param cip26ErrorsMap the CIP-26 validation errors by subject; a {@code null} value means never checked (ADR-019)
 */
public record BatchPrefetchData(
        Map<String, TokenMetadata> cip26Map,
        Map<String, List<ValidationError>> cip26ErrorsMap,
        Map<String, MetadataReferenceNft> cip68Map,
        Map<String, ProgrammableTokenCip113> cip113Map) {
}
