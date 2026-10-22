package org.cardanofoundation.tokenmetadata.registry.api.event;

/**
 * Published by the CIP-113 listener when registry nodes were indexed, or removed by a rollback, so that what depends on
 * the set of registered policies, such as the CIP-113 companion check (ADR-020), is recomputed.
 */
public record Cip113RegistryChangedEvent() {
}
