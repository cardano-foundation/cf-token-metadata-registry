package org.cardanofoundation.tokenmetadata.registry.api.model.v2;

import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;

public record Standards(TokenMetadata cip26, FungibleTokenMetadata cip68) {

    public static Standards empty() {
        return new Standards(null, null);
    }

    public Standards merge(Standards that) {
        var finalCip26 = cip26 != null ? cip26 : that.cip26();
        var finalCip68 = cip68 != null ? cip68 : that.cip68();
        return new Standards(finalCip26, finalCip68);
    }

}
