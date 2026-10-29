package org.cardanofoundation.tokenmetadata.registry.repository;

import org.cardanofoundation.tokenmetadata.registry.entity.InvalidToken;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


/**
 * The {@code invalid_token} table (ADR-020). Its delete queries are {@code @Transactional}: they join the caller's
 * transaction (block event listeners, the CIP-113 reconciler) and open their own when there is none, as when the CIP-26
 * sync forgets the items of a file again.
 */
@Repository
@Profile("!test")
public interface InvalidTokenRepository extends JpaRepository<InvalidToken, String> {

    /** A chain rollback: what was found after the rollback slot is gone with the blocks. Rows with no slot are not touched. */
    @Modifying
    @Transactional
    @Query("DELETE FROM InvalidToken t WHERE t.slot > :slot")
    int deleteBySlotGreaterThan(@Param("slot") Long slot);

    /** What an off-chain file had wrong is forgotten when the file is processed again or removed. */
    @Modifying
    @Transactional
    @Query("DELETE FROM InvalidToken t WHERE t.standard = :standard AND t.origin = :origin")
    int deleteByStandardAndOrigin(@Param("standard") String standard, @Param("origin") String origin);
}
