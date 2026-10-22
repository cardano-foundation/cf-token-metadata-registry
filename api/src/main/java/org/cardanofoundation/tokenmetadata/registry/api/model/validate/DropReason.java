package org.cardanofoundation.tokenmetadata.registry.api.model.validate;

import io.swagger.v3.oas.annotations.media.Schema;

/** Why the registry would not store the input at all; the same codes as the {@code invalid_token} table (ADR-020). */
@Schema(description = "Why the registry would not store it")
public record DropReason(
        @Schema(example = "datum_undecodable") String code,
        @Schema(example = "the datum cannot be decoded") String message) {
}
