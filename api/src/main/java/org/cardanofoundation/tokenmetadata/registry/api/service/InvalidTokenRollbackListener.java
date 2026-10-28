package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.repository.InvalidTokenRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A chain rollback removes the {@code invalid_token} rows found after the rollback slot, like the CIP-68 and CIP-113 rows
 * (ADR-020). Rows with no slot (from the CIP-26 registry in Git, or a reconciliation) are not touched: they do not come
 * from a block.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InvalidTokenRollbackListener {

    private final InvalidTokenRepository invalidTokenRepository;

    @EventListener
    @Transactional
    public void handleRollback(RollbackEvent rollbackEvent) {
        long rollbackSlot = rollbackEvent.getRollbackTo().getSlot();
        int count = invalidTokenRepository.deleteBySlotGreaterThan(rollbackSlot);
        log.info("Rollback to slot {}: deleted {} invalid token records", rollbackSlot, count);
    }
}
