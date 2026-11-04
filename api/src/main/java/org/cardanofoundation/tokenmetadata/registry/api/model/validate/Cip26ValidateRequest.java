package org.cardanofoundation.tokenmetadata.registry.api.model.validate;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import org.cardanofoundation.tokenmetadata.registry.model.Mapping;

/**
 * A CIP-26 registry entry to check, as it would be submitted to the token registry.
 *
 * @param mapping  the content of the registry file
 * @param fileName the file name it would have, e.g. {@code <subject>.json}; checked against the subject when given
 */
@Schema(description = "A CIP-26 registry entry, as the JSON file you would submit to the token registry")
public record Cip26ValidateRequest(
        @Schema(description = "The registry file content: subject, policy, name, description, ticker, url, decimals, logo, "
                + "each property as {\"value\": ..., \"sequenceNumber\": ..., \"signatures\": [...]}")
        Mapping mapping,
        @Schema(description = "The file name the entry would have in the registry, e.g. <subject>.json", nullable = true)
        @JsonProperty("file_name") @Nullable String fileName) {
}
