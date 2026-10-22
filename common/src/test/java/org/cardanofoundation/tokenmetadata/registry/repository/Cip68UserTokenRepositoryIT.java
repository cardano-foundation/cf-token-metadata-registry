package org.cardanofoundation.tokenmetadata.registry.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.cardanofoundation.tokenmetadata.registry.config.RepositoryTestConfig;
import org.cardanofoundation.tokenmetadata.registry.entity.Cip68UserToken;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The table of the CIP-68 user tokens seen on chain (ADR-022) against a real database: the upsert of the first sighting
 * keeps the smallest slot and its transaction, a replay changes nothing, and a rollback removes what was seen after it.
 */
@SpringBootTest(classes = RepositoryTestConfig.class)
@AutoConfigureTestDatabase
@Import(Cip68UserTokenWriter.class)
@Transactional
@ActiveProfiles("integration-test")
class Cip68UserTokenRepositoryIT {

    private static final String FT_A = "aa".repeat(28) + "0014df1041";
    private static final String RFT_B = "aa".repeat(28) + "001bc28042";

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private Cip68UserTokenRepository repository;
    @Autowired
    private Cip68UserTokenWriter writer;

    private Cip68UserToken read(String unit) {
        entityManager.flush();
        entityManager.clear();
        return repository.findById(unit).orElseThrow();
    }

    @Nested
    class UpsertFirstSeen {

        @Test
        void recordsATokenSeenForTheFirstTime() {
            writer.upsertFirstSeen(FT_A, "tx100", 100L);

            assertThat(read(FT_A)).extracting(Cip68UserToken::getSlot, Cip68UserToken::getTxHash).containsExactly(100L, "tx100");
        }

        @Test
        void aReplayChangesNothing() {
            writer.upsertFirstSeen(FT_A, "tx100", 100L);
            writer.upsertFirstSeen(FT_A, "tx100", 100L);

            assertThat(repository.count()).isEqualTo(1);
            assertThat(read(FT_A).getSlot()).isEqualTo(100L);
        }

        @Test
        void keepsTheEarliestSightingWhicheverIsWrittenFirst() {
            // with parallel block processing, a later block can commit before an earlier one
            writer.upsertFirstSeen(FT_A, "tx200", 200L);
            writer.upsertFirstSeen(FT_A, "tx100", 100L);
            writer.upsertFirstSeen(FT_A, "tx300", 300L);

            assertThat(read(FT_A)).extracting(Cip68UserToken::getSlot, Cip68UserToken::getTxHash).containsExactly(100L, "tx100");
        }
    }

    @Test
    void aRollbackRemovesTheTokensFirstSeenAfterTheRollbackSlot() {
        writer.upsertFirstSeen(FT_A, "tx100", 100L);
        writer.upsertFirstSeen(RFT_B, "tx300", 300L);

        int deleted = repository.deleteBySlotGreaterThan(200L);

        assertThat(deleted).isEqualTo(1);
        entityManager.clear();
        assertThat(repository.findAll()).extracting(Cip68UserToken::getUnit).containsExactly(FT_A);
    }
}
