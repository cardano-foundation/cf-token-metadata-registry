package org.cardanofoundation.tokenmetadata.registry.api.model.cip68;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;

import java.util.List;

/**
 * The CIP-68 metadata of a fungible token (label 333) or a rich fungible token (label 444), as indexed, with how it
 * departs from CIP-68 (ADR-019).
 */
@Schema(description = "CIP-68 fungible (label 333) or rich fungible (label 444) token metadata from a reference NFT inline datum. "
        + "The token is served as indexed, whatever is wrong with it: validation_errors lists how it departs from CIP-68.")
public record FungibleTokenMetadata(
        @Nullable Long decimals,
        @Nullable String description,
        @Nullable @Schema(description = "The logo as written, a URI with scheme https, ipfs, ar or data when it follows CIP-68. "
                + "For a rich fungible token (444), which has no logo, this is its image.") String logo,
        @Nullable String name,
        @Nullable String ticker,
        @Nullable String url,
        @Nullable Long version,
        @JsonProperty("validation_errors") @JsonInclude(JsonInclude.Include.NON_NULL)
        @Schema(name = "validation_errors", description = "How the datum departs from CIP-68 for this token's label; empty when it "
                + "follows it, absent when the row was indexed before validation existed.")
        @Nullable List<ValidationError> validationErrors) {

    /** Metadata with no validation errors. */
    public FungibleTokenMetadata(@Nullable Long decimals, @Nullable String description, @Nullable String logo,
                                 @Nullable String name, @Nullable String ticker, @Nullable String url,
                                 @Nullable Long version) {
        this(decimals, description, logo, name, ticker, url, version, List.of());
    }

    /** The same metadata with other validation errors (for example those of the properties a request asked for). */
    public FungibleTokenMetadata withValidationErrors(@Nullable List<ValidationError> errors) {
        return new FungibleTokenMetadata(decimals, description, logo, name, ticker, url, version, errors);
    }
}
