package org.cardanofoundation.tokenmetadata.registry.api.model;

import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.ProgrammableTokenCip113;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;

import java.util.Map;

public record BatchPrefetchData(
        Map<String, TokenMetadata> cip26Map,
        Map<String, MetadataReferenceNft> cip68Map,
        Map<String, ProgrammableTokenCip113> cip113Map) {
}
