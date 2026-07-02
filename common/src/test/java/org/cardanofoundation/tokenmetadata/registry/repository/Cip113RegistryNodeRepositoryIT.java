package org.cardanofoundation.tokenmetadata.registry.repository;

import org.cardanofoundation.tokenmetadata.registry.config.RepositoryTestConfig;
import org.cardanofoundation.tokenmetadata.registry.entity.Cip113RegistryNode;
import org.junit.jupiter.api.Test;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Integration tests for {@link Cip113RegistryNodeRepository}, focused on the current-state /
 * overwrite-same-slot data model (PK {@code (key, slot)}, tx_hash provenance-only).
 *
 * <p>These lock in the behaviour that fixes the same-slot / intra-block-chaining defect flagged in
 * yaci-store PR #870: two updates to the same registry node in the same slot must collapse to a
 * single deterministic row rather than producing duplicates that crash the batch lookup.
 */
@SpringBootTest(classes = RepositoryTestConfig.class)
@Transactional
@ActiveProfiles("integration-test")
// The `key` column mirrors the on-chain Aiken datum field name; it is a reserved word in H2 but
// not in PostgreSQL (the production target, and what the V3 migration is written for). H2 is only
// used here as an in-memory test DB, so NON_KEYWORDS=KEY lets the unquoted `key` in the entity and
// the ROW_NUMBER() native query parse — without quoting `key` in a way that would diverge from PG.
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:cip113regnode;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;NON_KEYWORDS=KEY",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false"
})
class Cip113RegistryNodeRepositoryIT {

    // 56-hex (28-byte) policy IDs — the real-registration form of the `key` column.
    private static final String KEY_A = "deadbeefcafebabedeadbeefcafebabedeadbeefcafebabedeadbeef0";
    private static final String KEY_B = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd0";

    // 64-hex (32-byte) transaction hashes.
    private static final String TX_1 = "1111111111111111111111111111111111111111111111111111111111111111";
    private static final String TX_2 = "2222222222222222222222222222222222222222222222222222222222222222";

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private Cip113RegistryNodeRepository repository;

    @Test
    void sameSlotUpdate_collapsesToSingleRow_lastWriterWins() {
        // Two updates to the SAME registry node in the SAME slot (intra-block tx chaining),
        // differing only in tx_hash and the transfer-logic script. Processed in block/tx order.
        Cip113RegistryNode first = node(KEY_A, 100L, TX_1, "1111111111111111111111111111111111111111111111111111111a");
        Cip113RegistryNode second = node(KEY_A, 100L, TX_2, "2222222222222222222222222222222222222222222222222222222b");

        repository.saveAll(List.of(first, second));
        entityManager.flush();
        entityManager.clear();

        // PK (key, slot) collapses the two into one row — no duplicate, no constraint violation.
        List<Cip113RegistryNode> all = repository.findAll();
        assertThat(all).hasSize(1);
        // Last writer wins: the surviving row is the later tx in processing order.
        assertThat(all.getFirst().getTransferLogicScript())
                .isEqualTo("2222222222222222222222222222222222222222222222222222222b");
    }

    @Test
    void findLatestByKeys_returnsExactlyOneRowPerKey_andNeverCrashesToMap() {
        // KEY_A has history across two slots plus a same-slot double-update at slot 100;
        // KEY_B has a single row. This is the exact shape that used to produce duplicate
        // rows at MAX(slot) and blow up Cip113RegistryService.findByPolicyIds' Collectors.toMap.
        repository.saveAll(List.of(
                node(KEY_A, 100L, TX_1, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa1"),
                node(KEY_A, 100L, TX_2, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa2"),
                node(KEY_A, 200L, TX_2, "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa3"),
                node(KEY_B, 150L, TX_1, "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb1")
        ));
        entityManager.flush();
        entityManager.clear();

        List<Cip113RegistryNode> latest = repository.findLatestByKeys(List.of(KEY_A, KEY_B));

        // Exactly one row per key.
        assertThat(latest).hasSize(2);
        // Collecting by key — mirrors Cip113RegistryService.findByPolicyIds — must not throw.
        Map<String, Cip113RegistryNode> byKey = assertNoToMapCrash(latest);
        // Latest-by-slot resolution.
        assertThat(byKey.get(KEY_A).getSlot()).isEqualTo(200L);
        assertThat(byKey.get(KEY_B).getSlot()).isEqualTo(150L);
    }

    @Test
    void findFirstByKeyOrderBySlotDesc_returnsLatestSlot() {
        repository.saveAll(List.of(
                node(KEY_A, 100L, TX_1, "cccccccccccccccccccccccccccccccccccccccccccccccccccccc01"),
                node(KEY_A, 300L, TX_2, "cccccccccccccccccccccccccccccccccccccccccccccccccccccc02")
        ));
        entityManager.flush();
        entityManager.clear();

        Optional<Cip113RegistryNode> result = repository.findFirstByKeyOrderBySlotDesc(KEY_A);

        assertThat(result).isPresent();
        assertThat(result.get().getSlot()).isEqualTo(300L);
    }

    private static Map<String, Cip113RegistryNode> assertNoToMapCrash(List<Cip113RegistryNode> rows) {
        Map<String, Cip113RegistryNode>[] holder = new Map[1];
        assertThatCode(() -> holder[0] = rows.stream()
                .collect(Collectors.toMap(Cip113RegistryNode::getKey, r -> r)))
                .doesNotThrowAnyException();
        return holder[0];
    }

    private static Cip113RegistryNode node(String key, long slot, String txHash, String transferLogic) {
        return Cip113RegistryNode.builder()
                .key(key)
                .slot(slot)
                .txHash(txHash)
                .transferLogicScript(transferLogic)
                .thirdPartyTransferLogicScript(null)
                .globalStatePolicyId(null)
                .next("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff")
                .datum("d8799f40ff")
                .build();
    }
}
