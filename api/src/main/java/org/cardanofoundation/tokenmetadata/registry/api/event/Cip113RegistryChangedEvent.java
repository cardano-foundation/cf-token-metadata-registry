package org.cardanofoundation.tokenmetadata.registry.api.event;

/**
 * Published by the CIP-113 listener when registry nodes were indexed, or removed by a rollback, so that what depends on
 * the set of registered policies, such as the CIP-113 companion check (ADR-020), is recomputed.
 *
 * @param nodesIndexed the registry nodes stored by a block
 * @param nodesRemoved the registry nodes removed by a rollback
 */
public record Cip113RegistryChangedEvent(int nodesIndexed, int nodesRemoved) {

    public static Cip113RegistryChangedEvent indexed(int nodes) {
        return new Cip113RegistryChangedEvent(nodes, 0);
    }

    public static Cip113RegistryChangedEvent rolledBack(int nodes) {
        return new Cip113RegistryChangedEvent(0, nodes);
    }
}
