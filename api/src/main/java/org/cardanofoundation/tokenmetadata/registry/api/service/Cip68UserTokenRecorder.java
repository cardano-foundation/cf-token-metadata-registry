package org.cardanofoundation.tokenmetadata.registry.api.service;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.entity.Cip68UserToken;
import org.cardanofoundation.tokenmetadata.registry.repository.Cip68UserTokenRepository;
import org.cardanofoundation.tokenmetadata.registry.repository.Cip68UserTokenWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.FUNGIBLE_TOKEN_PREFIX;
import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.RICH_FUNGIBLE_TOKEN_PREFIX;

/**
 * Remembers the CIP-68 fungible (333) and rich fungible (444) user tokens that appear in outputs, so that the V2 API can
 * serve a 333 or 444 subject only when the token exists (ADR-022, ported from yaci-store, bloxbean/yaci-store#1294). It
 * is called from {@link Cip68EventListener}, in Yaci Store's block transaction, where an exception stops the whole sync,
 * so it is:
 * <ul>
 *   <li><b>bounded:</b> only a unit whose asset name starts with the 333 or 444 label and has a name after it is kept, and
 *       a unit is at most 120 characters;</li>
 *   <li><b>idempotent and safe in parallel:</b> a token already recorded at the same or an earlier slot is not written
 *       again, and the write is an upsert that the database resolves ({@link Cip68UserTokenWriter}), in a fixed order, so
 *       two blocks processed in parallel that see the same new token do not fail with a duplicate key. The row keeps the
 *       smallest slot;</li>
 *   <li><b>best effort:</b> a failure of its own is a WARN, not an exception.</li>
 * </ul>
 */
@Component
@Slf4j
public class Cip68UserTokenRecorder {

    private final Cip68UserTokenRepository repository;
    private final Cip68UserTokenWriter writer;

    public Cip68UserTokenRecorder(Cip68UserTokenRepository repository, Cip68UserTokenWriter writer) {
        this.repository = repository;
        this.writer = writer;
    }

    /** Whether a unit is a 333 or 444 user token: its asset name starts with the label and has a name after it. */
    public static boolean isFungibleOrRichFungibleUnit(String unit) {
        String assetName = AssetType.fromUnit(unit).assetName();
        return assetName.length() > FUNGIBLE_TOKEN_PREFIX.length()
                && (assetName.startsWith(FUNGIBLE_TOKEN_PREFIX) || assetName.startsWith(RICH_FUNGIBLE_TOKEN_PREFIX));
    }

    /**
     * @param txHashByUnit the user tokens seen in a block, each with the hash of the first transaction of the block that
     *                     showed it (units in lower case hex)
     * @return the units written: seen for the first time, or earlier than recorded
     */
    @Transactional
    public Set<String> recordSeen(Map<String, String> txHashByUnit, long slot) {
        if (txHashByUnit.isEmpty()) {
            return Set.of();
        }
        Set<String> written = new HashSet<>();
        try {
            Map<String, Long> recordedSlot = repository.findAllById(txHashByUnit.keySet()).stream()
                    .collect(Collectors.toMap(Cip68UserToken::getUnit, Cip68UserToken::getSlot));
            // in a fixed order, so that two transactions that write the same tokens cannot wait for each other
            new TreeMap<>(txHashByUnit).forEach((unit, txHash) -> {
                // a token recorded at this slot or earlier is left alone; one recorded later (a later block committed
                // first) is lowered
                Long recorded = recordedSlot.get(unit);
                if (recorded == null || recorded > slot) {
                    writer.upsertFirstSeen(unit, txHash, slot);
                    written.add(unit);
                }
            });
        } catch (RuntimeException e) {
            log.warn("Could not record {} CIP-68 user token(s) of slot {}: {}: {}", txHashByUnit.size(), slot,
                    e.getClass().getSimpleName(), e.getMessage());
        }
        return written;
    }
}
