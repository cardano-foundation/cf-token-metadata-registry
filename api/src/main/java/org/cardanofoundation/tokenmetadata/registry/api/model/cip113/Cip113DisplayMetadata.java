package org.cardanofoundation.tokenmetadata.registry.api.model.cip113;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;

@Schema(description = "CIP-68 display metadata for a programmable token, if a reference NFT exists for the policy.")
public record Cip113DisplayMetadata(
        @Nullable String name,
        @Nullable String description,
        @Nullable String ticker,
        @Nullable Long decimals,
        @Nullable String logo
) {

    public static Cip113DisplayMetadata from(MetadataReferenceNft nft) {
        return new Cip113DisplayMetadata(
                nft.getName(),
                nft.getDescription(),
                nft.getTicker(),
                nft.getDecimals(),
                nft.getLogo()
        );
    }

}
