package org.cardanofoundation.tokenmetadata.registry.api.model.validate;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip113.ProgrammableTokenCip113;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;

import java.util.List;

/**
 * What the registry would do with the input: store it or not, with which validation errors, and how the V2 API would
 * serve it. Nothing is stored.
 *
 * @param standard         {@code CIP-26}, {@code CIP-68} or {@code CIP-113}
 * @param stored           whether the registry would index it
 * @param dropped          why it would not, when {@code stored} is false
 * @param validationErrors everything that departs from the standard, as the registry would record it
 * @param served           how V2 would serve it: one entry for CIP-26, one per label for CIP-68, none for CIP-113
 * @param datum            CIP-68: the datum, as given or built from the fields
 * @param cip113           CIP-113: the extension V2 would add to the token's response
 * @param notes            what the result assumes, for example that the user token is minted
 */
@Schema(description = "What the registry would do with the input. Nothing is stored")
public record ValidateResponse(
        @Schema(example = "CIP-68") String standard,
        @Schema(description = "Whether the registry would index it") boolean stored,
        @Schema(nullable = true) @Nullable DropReason dropped,
        @Schema(description = "What departs from the standard") @JsonProperty("validation_errors") List<ValidationError> validationErrors,
        @Schema(description = "How the V2 API would serve it") List<ServedAs> served,
        @Schema(description = "CIP-68: the reference NFT datum as CBOR hex, as given or built from the fields", nullable = true)
        @Nullable String datum,
        @Schema(description = "CIP-113: the extension V2 would add to the token", nullable = true) @Nullable ProgrammableTokenCip113 cip113,
        @Schema(description = "What the result assumes") List<String> notes) {
}
