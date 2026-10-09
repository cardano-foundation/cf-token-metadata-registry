package org.cardanofoundation.tokenmetadata.registry.api.model.cip68;

import jakarta.annotation.Nullable;

/**
 * A parsed CIP-68 datum: the fungible token fields the registry serves, plus the fields that tell which user-token
 * label the datum is for and that the 222 NFT and 444 RFT require ({@code image}, {@code mediaType}, {@code files}).
 * Those are only used to validate the datum and are not stored.
 */
public record ParsedCip68Datum(@Nullable Long decimals, @Nullable String description, @Nullable String logo,
                               @Nullable String name, @Nullable String ticker, @Nullable String url, long version,
                               @Nullable String image, @Nullable String mediaType, boolean hasFiles) {

    public FungibleTokenMetadata toFungibleTokenMetadata() {
        return new FungibleTokenMetadata(decimals, description, logo, name, ticker, url, version);
    }

}
