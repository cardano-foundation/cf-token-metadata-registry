package org.cardanofoundation.tokenmetadata.registry.api.model.v2;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationCode;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;

import java.util.List;
import java.util.Map;

@Schema(description = "A token subject with its metadata, standards details, and optional extensions from additional CIPs")
public record Subject(

        @Schema(description = "The subject identifier — concatenation of policy ID and asset name (hex)",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String subject,

        @Schema(description = "Token type: NATIVE for standard tokens, PROGRAMMABLE for tokens with on-chain transfer logic (e.g. CIP-113)",
                requiredMode = Schema.RequiredMode.REQUIRED)
        TokenType type,

        @Schema(description = "Merged display metadata from CIP-26/CIP-68 based on query priority",
                requiredMode = Schema.RequiredMode.REQUIRED)
        Metadata metadata,

        @Schema(description = "Raw per-standard metadata, only present when show_cips_details=true",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Standards standards,

        @Schema(description = "Additional on-chain properties from CIP extensions. "
                + "Each key is a CIP identifier (e.g. 'cip113'). "
                + "Omitted when the token has no extensions.",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        Map<String, Extension> extensions,

        @JsonProperty("validation_errors") @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(name = "validation_errors",
                description = "How the metadata of the token departs from its CIP, from every standard that has it, in query "
                        + "priority order (each entry names its cip and, for CIP-68, its sub_standard, the label). The token is "
                        + "returned as indexed; a MANDATORY error means it lacks something its CIP requires, so a wallet that wants "
                        + "to be strict may reject it, or ask with validation=MANDATORY_ONLY. Absent when the token was indexed "
                        + "before validation existed; metadata.valid then falls back to name and description being present.\n\n"
                        + ValidationCode.DOCUMENTATION,
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @Nullable List<ValidationError> validationErrors) {
}
