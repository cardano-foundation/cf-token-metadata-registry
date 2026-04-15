package org.cardanofoundation.tokenmetadata.registry.repository;

import org.cardanofoundation.tokenmetadata.registry.entity.Cip113RegistryNode;
import org.cardanofoundation.tokenmetadata.registry.entity.Cip113RegistryNodeId;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@Profile("!test")
public interface Cip113RegistryNodeRepository extends JpaRepository<Cip113RegistryNode, Cip113RegistryNodeId> {

    /**
     * Returns the most recent registry node state (highest slot) for a given key
     * (i.e. the policy ID of the programmable token being looked up).
     */
    Optional<Cip113RegistryNode> findFirstByKeyOrderBySlotDesc(String key);

    /**
     * Returns the latest registry node state per key using ROW_NUMBER() window function.
     * Only returns one row per key (the one with the highest slot).
     * Portable across PostgreSQL, H2, and MySQL 8+.
     */
    @Query(value = "SELECT * FROM (SELECT *, ROW_NUMBER() OVER (PARTITION BY key ORDER BY slot DESC) AS rn " +
            "FROM cip113_registry_node WHERE key IN (:keys)) ranked WHERE rn = 1",
            nativeQuery = true)
    List<Cip113RegistryNode> findLatestByKeys(@Param("keys") Collection<String> keys);

    int deleteBySlotGreaterThan(Long slot);

}
