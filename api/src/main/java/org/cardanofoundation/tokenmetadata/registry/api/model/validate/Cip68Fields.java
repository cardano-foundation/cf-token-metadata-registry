package org.cardanofoundation.tokenmetadata.registry.api.model.validate;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

/** The metadata of a CIP-68 token, from which the reference NFT datum is built. Unset fields are left out of the datum. */
@Schema(description = "The metadata of a CIP-68 token. The validator builds the reference NFT datum from it and returns it")
public record Cip68Fields(
        @Schema(example = "My Token", nullable = true) @Nullable String name,
        @Schema(example = "A token that does something useful", nullable = true) @Nullable String description,
        @Schema(example = "MTK", nullable = true) @Nullable String ticker,
        @Schema(example = "https://example.com", nullable = true) @Nullable String url,
        @Schema(description = "333 tokens: a URI (ipfs://, https://, ar:// or data:)", example = "ipfs://bafy...", nullable = true)
        @Nullable String logo,
        @Schema(description = "444 tokens (required): a URI (ipfs://, https://, ar:// or data:)", nullable = true)
        @Nullable String image,
        @Schema(description = "444 tokens: the MIME type of the image", example = "image/png", nullable = true)
        @JsonProperty("media_type") @Nullable String mediaType,
        @Schema(example = "6", nullable = true) @Nullable Long decimals,
        @Schema(description = "The CIP-68 version of the datum, 1 to 4", example = "1", nullable = true) @Nullable Long version) {
}
