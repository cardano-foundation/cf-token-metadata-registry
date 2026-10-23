package org.cardanofoundation.tokenmetadata.registry.invalidtoken;

/**
 * The stable {@code reason_code}s of {@code invalid_token} (ADR-020), and the standards that report them. The CIP-26 and
 * CIP-68 codes are yaci-store's (its ADR 0007). A code is never renamed or reused; a new one is added.
 */
public final class InvalidTokenReason {

    public static final String CIP_26 = "CIP-26";
    public static final String CIP_68 = "CIP-68";
    public static final String CIP_113 = "CIP-113";

    // CIP-26: a registry file that cannot become an entry
    public static final String FILENAME_SUBJECT_MISMATCH = "filename_subject_mismatch";
    public static final String UNUSABLE_SUBJECT = "unusable_subject";
    public static final String UNPARSEABLE_MAPPING = "unparseable_mapping";
    public static final String MISSING_GIT_HISTORY = "missing_git_history";

    // CIP-68: an output whose datum is not stored
    public static final String DATUM_UNDECODABLE = "datum_undecodable";
    public static final String DATUM_TOO_DEEP_TO_DECODE = "datum_too_deep_to_decode";
    public static final String PROCESSING_FAILURE = "processing_failure";

    // CIP-113: a registry node that cannot be used, and a registered policy that cannot be served
    public static final String REGISTRY_NODE_UNPARSEABLE = "registry_node_unparseable";
    /**
     * A policy registered in the CIP-113 registry whose tokens have no CIP-26 or CIP-68 metadata: a programmable token
     * is served only with a companion standard, so none of them can be. The row is kept in step with the metadata
     * (written while there is none, removed when some appears).
     */
    public static final String NO_COMPANION_METADATA = "no_companion_metadata";

    private InvalidTokenReason() {
    }
}
