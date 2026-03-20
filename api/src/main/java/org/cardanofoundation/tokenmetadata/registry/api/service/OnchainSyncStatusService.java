package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.common.util.Tuple;
import com.bloxbean.cardano.yaci.store.core.service.ChainTipService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Tracks on-chain sync progress by comparing the local cursor position against the network tip.
 * Ported and simplified from yaci-store's admin-ui SyncStatusService.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OnchainSyncStatusService {

    private static final long INITIAL_SYNC_REFRESH_INTERVAL = 15 * 60 * 1000; // 15 minutes
    private static final long SYNCED_REFRESH_INTERVAL = 3 * 60 * 1000;        // 3 minutes
    private static final long SYNC_THRESHOLD_BLOCKS = 1000;
    private static final double SYNCED_PERCENTAGE_THRESHOLD = 98.0;

    private final CursorService cursorService;
    private final ChainTipService chainTipService;

    private volatile NetworkTip cachedTip;
    private volatile long lastTipFetchTime = 0;

    record NetworkTip(long block, long slot) {}

    public boolean isSynced() {
        return getSyncPercentage() >= SYNCED_PERCENTAGE_THRESHOLD;
    }

    public double getSyncPercentage() {
        Optional<Cursor> cursorOpt = cursorService.getCursor();
        if (cursorOpt.isEmpty()) {
            return 0.0;
        }

        long currentBlock = cursorOpt.get().getBlock();
        Optional<NetworkTip> tip = getCachedTip(currentBlock);
        if (tip.isEmpty()) {
            return 0.0;
        }

        long networkBlock = tip.get().block();
        if (networkBlock <= 0) {
            return 0.0;
        }

        long finalNetworkBlock = Math.max(currentBlock, networkBlock);
        return (double) currentBlock / finalNetworkBlock * 100.0;
    }

    private Optional<NetworkTip> getCachedTip(long currentBlock) {
        long now = System.currentTimeMillis();

        if (cachedTip != null && currentBlock >= cachedTip.block()) {
            return Optional.of(cachedTip);
        }

        long refreshInterval = INITIAL_SYNC_REFRESH_INTERVAL;
        if (cachedTip != null) {
            long blocksBehind = cachedTip.block() - currentBlock;
            if (blocksBehind <= SYNC_THRESHOLD_BLOCKS) {
                refreshInterval = SYNCED_REFRESH_INTERVAL;
            }
        }

        if (cachedTip != null && (now - lastTipFetchTime) < refreshInterval) {
            return Optional.of(cachedTip);
        }

        try {
            Optional<Tuple<Tip, Integer>> tipAndEpoch = chainTipService.getTipAndCurrentEpoch();
            if (tipAndEpoch.isPresent()) {
                Tip tip = tipAndEpoch.get()._1;
                cachedTip = new NetworkTip(tip.getBlock(), tip.getPoint().getSlot());
                lastTipFetchTime = now;
                return Optional.of(cachedTip);
            }
            return Optional.ofNullable(cachedTip);
        } catch (Exception e) {
            log.debug("Could not get network tip: {}", e.getMessage());
            return Optional.ofNullable(cachedTip);
        }
    }

}
