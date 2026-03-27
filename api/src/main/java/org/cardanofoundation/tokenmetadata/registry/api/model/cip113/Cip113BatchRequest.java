package org.cardanofoundation.tokenmetadata.registry.api.model.cip113;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import java.util.List;

@Schema(description = "Batch request for CIP-113 registry lookups by policy IDs.")
public record Cip113BatchRequest(

        @JsonProperty("policy_ids")
        @Valid
        @Schema(description = "List of policy IDs to look up.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        List<String> policyIds

) {}
