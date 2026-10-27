package org.cardanofoundation.tokenmetadata.registry.api.service.cip113;

import org.cardanofoundation.tokenmetadata.registry.validation.FieldCategory;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationCode;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;

/**
 * What is reported about a programmable token (ADR-016, ADR-020). A CIP-113 registry node says only that a policy is
 * programmable: its name, ticker, decimals and logo come from CIP-26 or CIP-68, its <em>companion</em> metadata. A token
 * of a registered policy without any is still served, as the registry indexed it, with this error, as a token that breaks
 * CIP-26 or CIP-68 is (ADR-019); it is not an {@code invalid_token} row.
 */
public final class Cip113Validation {

    /** The field of the error: the CIP-26 or CIP-68 metadata of the programmable token. */
    public static final String COMPANION_METADATA = "companion_metadata";

    /**
     * A programmable token needs a companion: without CIP-26 or CIP-68 metadata it does not meet a mandatory requirement
     * (decided 2026-10-27), so it is served under {@code ?validation=ALL} and left out by {@code MANDATORY_ONLY} and
     * {@code STRICT}.
     */
    static final FieldCategory MISSING_COMPANION_CATEGORY = FieldCategory.MANDATORY;

    private Cip113Validation() {
    }

    /** The error of a programmable token that has no CIP-26 or CIP-68 metadata. */
    public static ValidationError missingCompanion() {
        return new ValidationError(ValidationError.CIP_113, null, COMPANION_METADATA, MISSING_COMPANION_CATEGORY,
                ValidationCode.MISSING_VALUE,
                "the policy is registered in the CIP-113 registry, but the token has no CIP-26 or CIP-68 metadata "
                        + "(name, ticker, decimals, logo)");
    }
}
