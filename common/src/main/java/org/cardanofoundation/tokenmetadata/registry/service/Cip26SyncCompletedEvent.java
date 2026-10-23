package org.cardanofoundation.tokenmetadata.registry.service;

/**
 * Published by {@link TokenMetadataSyncService} when a CIP-26 sync run has stored or removed entries, so that what
 * depends on the CIP-26 entries, such as the CIP-113 companion check (ADR-020), is recomputed. It is an in-process
 * event: a sync run by the separate {@code job} process does not publish to the API.
 *
 * @param filesProcessed  the registry files stored or judged again
 * @param subjectsDeleted the subjects removed because their file was deleted upstream
 */
public record Cip26SyncCompletedEvent(int filesProcessed, int subjectsDeleted) {
}
