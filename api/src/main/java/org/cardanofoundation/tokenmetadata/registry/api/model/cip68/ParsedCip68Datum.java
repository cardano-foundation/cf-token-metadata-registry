package org.cardanofoundation.tokenmetadata.registry.api.model.cip68;

import jakarta.annotation.Nullable;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;

import java.util.List;
import java.util.Map;

/**
 * A parsed CIP-68 datum, as written: the fungible token fields the registry serves, and the NFT and RFT fields
 * ({@code image}, {@code mediaType}) and the {@code files} and additional properties ({@code properties}, as JSON:
 * {@code {"files": [...], "additional_properties": {...}}}, either key may be absent, {@code null} when there is neither).
 * <p>
 * {@link #validationErrors} are the ways the datum departs from CIP-68 that do not depend on the label of the token (a
 * wrong type, a version CIP-68 does not define, a Plutus constructor in a property, ...). What a label requires is checked
 * later, by {@code Cip68Validator} (ADR-019).
 */
public record ParsedCip68Datum(@Nullable Long decimals,
                               @Nullable String description,
                               @Nullable String logo,
                               @Nullable String name,
                               @Nullable String ticker,
                               @Nullable String url,
                               @Nullable Long version,
                               @Nullable String image,
                               @Nullable String mediaType,
                               @Nullable Map<String, Object> properties,
                               List<ValidationError> validationErrors) {

    /** A datum that departs from nothing the parser checks (tests and callers that do not care about errors). */
    public ParsedCip68Datum(@Nullable Long decimals, @Nullable String description, @Nullable String logo,
                            @Nullable String name, @Nullable String ticker, @Nullable String url,
                            @Nullable Long version, @Nullable String image, @Nullable String mediaType,
                            @Nullable Map<String, Object> properties) {
        this(decimals, description, logo, name, ticker, url, version, image, mediaType, properties, List.of());
    }

    /** Whether the datum has a {@code files} property (kept as written, whether or not it follows CIP-68). */
    public boolean hasFiles() {
        return properties != null && properties.containsKey("files");
    }
}
