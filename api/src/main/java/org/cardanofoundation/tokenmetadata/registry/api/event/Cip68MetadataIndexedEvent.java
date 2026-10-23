package org.cardanofoundation.tokenmetadata.registry.api.event;

import java.util.Set;

/**
 * Published by the CIP-68 listener for a block that stored reference NFT datums, with the policies they belong to.
 * Listeners that depend on CIP-68 metadata react to it, such as the CIP-113 companion check (ADR-020).
 *
 * @param policyIds the policies of the reference NFTs stored
 */
public record Cip68MetadataIndexedEvent(Set<String> policyIds) {
}
