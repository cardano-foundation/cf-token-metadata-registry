package org.cardanofoundation.tokenmetadata.registry.repository;

import org.cardanofoundation.tokenmetadata.registry.entity.OffChainSyncState;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Profile("!test")
public interface SyncStateRepository extends JpaRepository<OffChainSyncState, Long> {
    Optional<OffChainSyncState> findTopByOrderByIdDesc();
}
