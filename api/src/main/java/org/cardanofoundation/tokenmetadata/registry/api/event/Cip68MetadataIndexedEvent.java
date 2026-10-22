package org.cardanofoundation.tokenmetadata.registry.api.event;

import java.util.Set;

/**
 * Published by the CIP-68 listener for a block that stored a reference NFT datum or saw a 333 or 444 user token for
 * the first time, with the policies they belong to. Listeners that depend on CIP-68 metadata react to it, such as the
 * CIP-113 companion check (ADR-020).
 *
 * @param policyIds the policies of the reference NFTs stored and of the user tokens seen for the first time
 */
public record Cip68MetadataIndexedEvent(Set<String> policyIds) {
}
