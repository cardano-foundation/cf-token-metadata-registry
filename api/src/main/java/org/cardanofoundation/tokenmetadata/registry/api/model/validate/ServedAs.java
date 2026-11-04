package org.cardanofoundation.tokenmetadata.registry.api.model.validate;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.Metadata;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.ValidationScope;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;

import java.util.List;

/**
 * How the V2 API would serve the token: its metadata, the validation errors of its subject, and the
 * {@code ?validation=} scopes that return it.
 *
 * @param label the CIP-68 user token label (333 or 444); absent for CIP-26
 */
@Schema(description = "How the V2 API would serve the token")
public record ServedAs(
        @Schema(description = "The CIP-68 label of the subject (333 or 444); absent for CIP-26", nullable = true)
        @Nullable Integer label,
        @Schema(description = "The metadata block of the V2 response") Metadata metadata,
        @Schema(description = "The validation errors of the V2 response") @JsonProperty("validation_errors") List<ValidationError> validationErrors,
        @Schema(description = "The ?validation= scopes under which V2 returns the token") List<ValidationScope> scopes) {
}
