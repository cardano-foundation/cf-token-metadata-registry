package org.cardanofoundation.tokenmetadata.registry.repository;

import org.cardanofoundation.tokenmetadata.registry.entity.InvalidToken;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@Profile("!test")
public interface InvalidTokenRepository extends JpaRepository<InvalidToken, String> {

    /** A chain rollback: what was found after the rollback slot is gone with the blocks. Rows with no slot are not touched. */
    @Modifying
    @Query("DELETE FROM InvalidToken t WHERE t.slot > :slot")
    int deleteBySlotGreaterThan(@Param("slot") Long slot);

    /** What an off-chain file had wrong is forgotten when the file is processed again or removed. */
    @Modifying
    @Query("DELETE FROM InvalidToken t WHERE t.standard = :standard AND t.origin = :origin")
    int deleteByStandardAndOrigin(@Param("standard") String standard, @Param("origin") String origin);

    /** The rows of one reason, for a reconciliation that keeps them in step with the data they describe. */
    List<InvalidToken> findByStandardAndReasonCode(String standard, String reasonCode);

    @Modifying
    @Query("DELETE FROM InvalidToken t WHERE t.id IN :ids")
    int deleteByIdIn(@Param("ids") Collection<String> ids);
}
