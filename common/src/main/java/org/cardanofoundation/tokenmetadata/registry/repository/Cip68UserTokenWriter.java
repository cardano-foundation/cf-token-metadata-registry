package org.cardanofoundation.tokenmetadata.registry.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Writes the first sighting of a CIP-68 user token with an upsert that the database resolves, not with a read followed
 * by an insert (ported from yaci-store, bloxbean/yaci-store#1294).
 * <p>
 * Why: Yaci Store can process blocks in parallel ({@code store.executor.enable-parallel-processing}), each in its own
 * transaction. Two transactions that both see a token for the first time both read "not recorded" and both insert it,
 * and the second fails with a duplicate key when it <em>commits</em>, inside the block transaction, where an exception
 * stops the whole sync. A statement that resolves the conflict itself cannot fail that way.
 * <p>
 * The row keeps the <b>smallest</b> slot: with parallel processing a later block can commit first, and a rollback to a
 * slot between the two must not delete a token that was already seen before it. The database is PostgreSQL in
 * production and H2 in the repository tests, hence one statement for each.
 */
@Component
@Profile("!test")
public class Cip68UserTokenWriter {

    private static final String POSTGRESQL = "INSERT INTO cip68_user_token (unit, slot, tx_hash) VALUES (:unit, :slot, :txHash) "
            + "ON CONFLICT (unit) DO UPDATE SET slot = EXCLUDED.slot, tx_hash = EXCLUDED.tx_hash "
            + "WHERE EXCLUDED.slot < cip68_user_token.slot";

    private static final String H2 = "MERGE INTO cip68_user_token t USING (VALUES (CAST(:unit AS VARCHAR(120)), "
            + "CAST(:slot AS BIGINT), CAST(:txHash AS VARCHAR(64)))) s (unit, slot, tx_hash) ON t.unit = s.unit "
            + "WHEN MATCHED AND s.slot < t.slot THEN UPDATE SET slot = s.slot, tx_hash = s.tx_hash "
            + "WHEN NOT MATCHED THEN INSERT (unit, slot, tx_hash) VALUES (s.unit, s.slot, s.tx_hash)";

    @PersistenceContext
    private EntityManager entityManager;

    private volatile String statement;

    /** Records the token, or lowers the slot of its row when this sighting is earlier. Needs a transaction. */
    public void upsertFirstSeen(String unit, String txHash, long slot) {
        entityManager.createNativeQuery(statement())
                .setParameter("unit", unit)
                .setParameter("slot", slot)
                .setParameter("txHash", txHash)
                .executeUpdate();
    }

    private String statement() {
        String sql = statement;
        if (sql == null) {
            Dialect dialect = entityManager.getEntityManagerFactory().unwrap(SessionFactoryImplementor.class)
                    .getJdbcServices().getDialect();
            if (dialect instanceof PostgreSQLDialect) {
                sql = POSTGRESQL;
            } else if (dialect instanceof H2Dialect) {
                sql = H2;
            } else {
                throw new IllegalStateException("No upsert of the CIP-68 user tokens for the database dialect "
                        + dialect.getClass().getName());
            }
            statement = sql;
        }
        return sql;
    }
}
