package org.cardanofoundation.tokenmetadata.registry.api.model.cip68;

import jakarta.annotation.Nullable;

/**
 * A parsed CIP-68 datum: the fungible token fields the registry serves, plus the fields that tell which user-token
 * label the datum is for and that the 222 NFT and 444 RFT require ({@code image}, {@code mediaType}, {@code files}).
 * Those are only used to validate the datum and are not stored. {@code hasFiles} is true only for a {@code files}
 * property that CIP-68 defines (see {@code Cip68FTDatumParser}).
 */
public record ParsedCip68Datum(@Nullable Long decimals, @Nullable String description, @Nullable String logo,
                               @Nullable String name, @Nullable String ticker, @Nullable String url, long version,
                               @Nullable String image, @Nullable String mediaType, boolean hasFiles) {

    /** The same datum without its {@code logo}. */
    public ParsedCip68Datum withoutLogo() {
        return new ParsedCip68Datum(decimals, description, null, name, ticker, url, version, image, mediaType, hasFiles);
    }

    public FungibleTokenMetadata toFungibleTokenMetadata() {
        return new FungibleTokenMetadata(decimals, description, logo, name, ticker, url, version);
    }

}
