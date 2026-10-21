package org.cardanofoundation.tokenmetadata.registry.api.model.cip68;

/**
 * What reading an inline datum gave: a datum that was read, a datum that is not a CIP-68 datum (ignored, since a
 * reference NFT output can carry any datum), or a datum that is dropped as unusable or an attack, with the reason
 * (recorded in {@code invalid_token}, ADR-020).
 */
public sealed interface ParseOutcome {

    record Parsed(ParsedCip68Datum datum) implements ParseOutcome {
    }

    record NotCip68() implements ParseOutcome {
    }

    /** {@code reasonCode} is one of {@code InvalidTokenReason}; {@code message} is for people. */
    record Dropped(String reasonCode, String message) implements ParseOutcome {
    }
}
