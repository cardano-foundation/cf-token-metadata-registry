package org.cardanofoundation.tokenmetadata.registry.api.model.cip68;

public record FungibleTokenMetadata(Long decimals, String description, String logo, String name, String ticker,
                                    String url, Long version) {


}
