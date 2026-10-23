package org.cardanofoundation.tokenmetadata.registry.api.model.validate;

import io.swagger.v3.oas.annotations.media.Schema;

/** A CIP-113 registry node datum to check. */
@Schema(description = "A CIP-113 registry node datum to check")
public record Cip113ValidateRequest(
        @Schema(description = "The inline datum of the registry node, as CBOR hex") String datum) {
}
