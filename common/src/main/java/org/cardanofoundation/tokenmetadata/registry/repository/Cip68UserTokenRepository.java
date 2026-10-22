package org.cardanofoundation.tokenmetadata.registry.repository;

import org.cardanofoundation.tokenmetadata.registry.entity.Cip68UserToken;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
@Profile("!test")
public interface Cip68UserTokenRepository extends JpaRepository<Cip68UserToken, String> {

    /** A chain rollback: the tokens first seen after the rollback slot are gone with the blocks. */
    @Modifying
    @Query("DELETE FROM Cip68UserToken t WHERE t.slot > :slot")
    int deleteBySlotGreaterThan(@Param("slot") Long slot);
}
