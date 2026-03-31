package org.cardanofoundation.tokenmetadata.registry.api.model.cip113;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import org.cardanofoundation.tokenmetadata.registry.api.model.v2.Extension;
import jakarta.annotation.Nullable;

@Schema(description = "CIP-113 programmable token metadata. Present when the token's policy ID is "
        + "registered in an on-chain CIP-113 programmable token registry. CIP-113 tokens are standard "
        + "CIP-26/CIP-68 tokens with additional on-chain transfer validation logic — effectively "
        + "tokens in a 'smart contract jail' for regulatory compliance, freeze/seize, or custom rules.")
public record ProgrammableTokenCip113(

        @Schema(description = "Blake2b-224 hash of the Plutus script that validates every transfer of this token. "
                + "This script runs on-chain via the withdraw-zero pattern whenever tokens move between addresses.",
                example = "aaa513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126102",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @JsonProperty("transfer_logic_script") String transferLogicScript,

        @Schema(description = "Blake2b-224 hash of the Plutus script that validates issuer/admin operations "
                + "such as freeze, seize, or burn. Allows a token issuer to perform privileged actions "
                + "on tokens held by other addresses. Null when the substandard has no third-party operations.",
                example = "def513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126103",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("third_party_transfer_logic_script") @Nullable String thirdPartyTransferLogicScript,

        @Schema(description = "Policy ID of an optional global state NFT used by the transfer logic. "
                + "For example, a freeze-and-seize substandard uses this to reference a denylist "
                + "of blocked addresses. Null when the token's substandard has no global state.",
                example = "1234567890abcdef1234567890abcdef1234567890abcdef12345678",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @JsonProperty("global_state_policy_id") @Nullable String globalStatePolicyId

) implements Extension {

    public static final String EXTENSION_KEY = "cip113";

}
