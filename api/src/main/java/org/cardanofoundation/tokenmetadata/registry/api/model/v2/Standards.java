package org.cardanofoundation.tokenmetadata.registry.api.model.v2;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;

public record Standards(TokenMetadata cip26, FungibleTokenMetadata cip68) {

    /** True when neither standard has anything indexed for the subject. */
    @JsonIgnore
    public boolean isEmpty() {
        return cip26 == null && cip68 == null;
    }

    public static Standards empty() {
        return new Standards(null, null);
    }

    public Standards merge(Standards that) {
        TokenMetadata finalCip26 = cip26 != null ? cip26 : that.cip26();
        FungibleTokenMetadata finalCip68 = cip68 != null ? cip68 : that.cip68();
        return new Standards(finalCip26, finalCip68);
    }

}
