package org.cardanofoundation.tokenmetadata.registry.api.model.v2;

import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;

public record Standards(TokenMetadata cip26, FungibleTokenMetadata cip68) {
}
