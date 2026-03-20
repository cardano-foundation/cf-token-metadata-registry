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
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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

    private final AtomicReference<NetworkTip> cachedTip = new AtomicReference<>();
    private final AtomicLong lastTipFetchTime = new AtomicLong(0);

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
        NetworkTip currentCachedTip = cachedTip.get();

        if (currentCachedTip != null && currentBlock >= currentCachedTip.block()) {
            return Optional.of(currentCachedTip);
        }

        long refreshInterval = INITIAL_SYNC_REFRESH_INTERVAL;
        if (currentCachedTip != null) {
            long blocksBehind = currentCachedTip.block() - currentBlock;
            if (blocksBehind <= SYNC_THRESHOLD_BLOCKS) {
                refreshInterval = SYNCED_REFRESH_INTERVAL;
            }
        }

        if (currentCachedTip != null && (now - lastTipFetchTime.get()) < refreshInterval) {
            return Optional.of(currentCachedTip);
        }

        try {
            Optional<Tuple<Tip, Integer>> tipAndEpoch = chainTipService.getTipAndCurrentEpoch();
            if (tipAndEpoch.isPresent()) {
                Tip tip = tipAndEpoch.get()._1;
                NetworkTip newTip = new NetworkTip(tip.getBlock(), tip.getPoint().getSlot());
                cachedTip.set(newTip);
                lastTipFetchTime.set(now);
                return Optional.of(newTip);
            }
            return Optional.ofNullable(currentCachedTip);
        } catch (Exception e) {
            log.debug("Could not get network tip: {}", e.getMessage());
            return Optional.ofNullable(currentCachedTip);
        }
    }

}
