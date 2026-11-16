package org.cardanofoundation.tokenmetadata.registry.api.model.validate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * A CIP-68 reference NFT datum to check: either the datum itself, or the fields to build it from.
 *
 * @param datum  the inline datum of the reference NFT, as CBOR hex
 * @param fields the metadata to build the datum from
 * @param labels the user token labels the datum is for: 333 (fungible) and/or 444 (rich fungible); 333 by default
 */
@Schema(description = "A CIP-68 reference NFT datum to check: give either `datum` (CBOR hex) or `fields`")
public record Cip68ValidateRequest(
        @Schema(description = "The inline datum of the reference NFT, as CBOR hex", nullable = true) @Nullable String datum,
        @Schema(description = "The token metadata to build the datum from", nullable = true) @Nullable Cip68Fields fields,
        @Schema(description = "The labels of the user tokens: 333 (fungible token) and/or 444 (rich fungible token). Default 333",
                example = "[333]", nullable = true) @Nullable List<Integer> labels) {
}
