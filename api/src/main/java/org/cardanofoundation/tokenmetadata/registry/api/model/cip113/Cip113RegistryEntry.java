package org.cardanofoundation.tokenmetadata.registry.api.model.cip113;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;

@Schema(description = "A registered CIP-113 programmable token with optional CIP-68 display metadata.")
public record Cip113RegistryEntry(

        @JsonProperty("policy_id")
        @Schema(description = "Policy ID of the registered programmable token.",
                example = "ae563991eada7867dd5b734d7f0dbdbd7b8a26938b0256bba8cc77db",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String policyId,

        @JsonProperty("transfer_logic_script")
        @Schema(description = "Blake2b-224 hash of the Plutus script that validates every transfer of this token.",
                example = "aaa513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126102",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String transferLogicScript,

        @JsonProperty("third_party_transfer_logic_script")
        @Schema(description = "Blake2b-224 hash of the Plutus script that validates issuer/admin operations such as freeze or seize.",
                example = "def513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126103",
                requiredMode = Schema.RequiredMode.REQUIRED)
        String thirdPartyTransferLogicScript,

        @JsonProperty("global_state_policy_id")
        @Nullable
        @Schema(description = "Policy ID of an optional global state NFT used by the transfer logic (e.g., a denylist).",
                nullable = true)
        String globalStatePolicyId,

        @Nullable
        @Schema(description = "CIP-68 display metadata if a reference NFT exists for this policy.",
                nullable = true)
        Cip113DisplayMetadata display

) {}
