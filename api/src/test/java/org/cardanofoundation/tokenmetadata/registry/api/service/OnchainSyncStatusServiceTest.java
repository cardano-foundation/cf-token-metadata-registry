package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Tip;
import com.bloxbean.cardano.yaci.store.common.domain.Cursor;
import com.bloxbean.cardano.yaci.store.common.service.CursorService;
import com.bloxbean.cardano.yaci.store.common.util.Tuple;
import com.bloxbean.cardano.yaci.store.core.service.ChainTipService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OnchainSyncStatusServiceTest {

    @Mock
    private CursorService cursorService;

    @Mock
    private ChainTipService chainTipService;

    @InjectMocks
    private OnchainSyncStatusService syncStatusService;

    private Cursor cursorAt(long block) {
        return Cursor.builder().block(block).slot(block * 20L).build();
    }

    private Optional<Tuple<Tip, Integer>> tipAt(long block) {
        Tip tip = new Tip(new Point(block * 20L, "abcd"), block);
        return Optional.of(new Tuple<>(tip, 100));
    }

    @Nested
    @DisplayName("getSyncPercentage")
    class GetSyncPercentage {

        @Test
        void noCursor_returnsZero() {
            when(cursorService.getCursor()).thenReturn(Optional.empty());

            assertThat(syncStatusService.getSyncPercentage()).isEqualTo(0.0);
        }

        @Test
        void noNetworkTip_returnsZero() {
            when(cursorService.getCursor()).thenReturn(Optional.of(cursorAt(1000)));
            when(chainTipService.getTipAndCurrentEpoch()).thenReturn(Optional.empty());

            assertThat(syncStatusService.getSyncPercentage()).isEqualTo(0.0);
        }

        @ParameterizedTest(name = "cursor at {0}, tip at 1000 → {1}%")
        @CsvSource({"500, 50.0", "980, 98.0", "1000, 100.0"})
        void syncPercentage_matchesCursorProgress(long cursorBlock, double expectedPercentage) {
            when(cursorService.getCursor()).thenReturn(Optional.of(cursorAt(cursorBlock)));
            when(chainTipService.getTipAndCurrentEpoch()).thenReturn(tipAt(1000));

            assertThat(syncStatusService.getSyncPercentage()).isCloseTo(expectedPercentage, within(0.1));
        }

        @Test
        void chainTipServiceThrows_returnsZero() {
            when(cursorService.getCursor()).thenReturn(Optional.of(cursorAt(500)));
            when(chainTipService.getTipAndCurrentEpoch()).thenThrow(new RuntimeException("connection refused"));

            assertThat(syncStatusService.getSyncPercentage()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("isSynced")
    class IsSynced {

        @Test
        void below100Percent_returnsNotSynced() {
            when(cursorService.getCursor()).thenReturn(Optional.of(cursorAt(500)));
            when(chainTipService.getTipAndCurrentEpoch()).thenReturn(tipAt(1000));

            assertThat(syncStatusService.isSynced()).isFalse();
        }

        @Test
        void at98Percent_returnsNotSynced() {
            when(cursorService.getCursor()).thenReturn(Optional.of(cursorAt(980)));
            when(chainTipService.getTipAndCurrentEpoch()).thenReturn(tipAt(1000));

            assertThat(syncStatusService.isSynced()).isFalse();
        }

        @Test
        void fullySynced_returnsSynced() {
            when(cursorService.getCursor()).thenReturn(Optional.of(cursorAt(1000)));
            when(chainTipService.getTipAndCurrentEpoch()).thenReturn(tipAt(1000));

            assertThat(syncStatusService.isSynced()).isTrue();
        }

        @Test
        void noCursor_returnsNotSynced() {
            when(cursorService.getCursor()).thenReturn(Optional.empty());

            assertThat(syncStatusService.isSynced()).isFalse();
        }

        @Test
        void noTip_returnsNotSynced() {
            when(cursorService.getCursor()).thenReturn(Optional.of(cursorAt(500)));
            when(chainTipService.getTipAndCurrentEpoch()).thenReturn(Optional.empty());

            assertThat(syncStatusService.isSynced()).isFalse();
        }
    }

    @Nested
    @DisplayName("tip caching")
    class TipCaching {

        @Test
        void secondCall_usesCachedTip_doesNotFetchAgain() {
            when(cursorService.getCursor()).thenReturn(Optional.of(cursorAt(500)));
            when(chainTipService.getTipAndCurrentEpoch()).thenReturn(tipAt(1000));

            syncStatusService.getSyncPercentage(); // populates cache
            syncStatusService.getSyncPercentage(); // should use cache

            verify(chainTipService, times(1)).getTipAndCurrentEpoch();
        }

        @Test
        void cursorAheadOfCachedTip_returnsCachedTip() {
            // First call: cursor at 500, tip at 1000
            when(cursorService.getCursor()).thenReturn(Optional.of(cursorAt(500)));
            when(chainTipService.getTipAndCurrentEpoch()).thenReturn(tipAt(1000));
            syncStatusService.getSyncPercentage();

            // Second call: cursor has caught up past the cached tip
            when(cursorService.getCursor()).thenReturn(Optional.of(cursorAt(1100)));
            double percentage = syncStatusService.getSyncPercentage();

            // Should use cached tip (1000), cursor (1100) > tip, so finalNetworkBlock = 1100
            assertThat(percentage).isCloseTo(100.0, within(0.1));
            verify(chainTipService, times(1)).getTipAndCurrentEpoch();
        }

        @Test
        void networkBlockZero_returnsZero() {
            when(cursorService.getCursor()).thenReturn(Optional.of(cursorAt(500)));
            when(chainTipService.getTipAndCurrentEpoch()).thenReturn(tipAt(0));

            assertThat(syncStatusService.getSyncPercentage()).isEqualTo(0.0);
        }

        @Test
        void nearTip_usesShorterRefreshInterval() {
            // First call at 999 blocks behind (near tip)
            when(cursorService.getCursor()).thenReturn(Optional.of(cursorAt(9001)));
            when(chainTipService.getTipAndCurrentEpoch()).thenReturn(tipAt(10000));
            syncStatusService.getSyncPercentage();

            // Second call still behind — should use cached (within 1min interval)
            when(cursorService.getCursor()).thenReturn(Optional.of(cursorAt(9500)));
            syncStatusService.getSyncPercentage();

            verify(chainTipService, times(1)).getTipAndCurrentEpoch();
        }
    }

}
