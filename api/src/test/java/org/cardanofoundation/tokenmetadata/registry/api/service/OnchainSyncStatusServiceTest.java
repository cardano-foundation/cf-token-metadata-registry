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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
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

        @Test
        void halfwaySynced_returnsFiftyPercent() {
            when(cursorService.getCursor()).thenReturn(Optional.of(cursorAt(500)));
            when(chainTipService.getTipAndCurrentEpoch()).thenReturn(tipAt(1000));

            assertThat(syncStatusService.getSyncPercentage()).isCloseTo(50.0, within(0.1));
        }

        @Test
        void fullySynced_returnsHundredPercent() {
            when(cursorService.getCursor()).thenReturn(Optional.of(cursorAt(1000)));
            when(chainTipService.getTipAndCurrentEpoch()).thenReturn(tipAt(1000));

            assertThat(syncStatusService.getSyncPercentage()).isCloseTo(100.0, within(0.1));
        }

        @Test
        void almostSynced_returnsHighPercentage() {
            when(cursorService.getCursor()).thenReturn(Optional.of(cursorAt(980)));
            when(chainTipService.getTipAndCurrentEpoch()).thenReturn(tipAt(1000));

            assertThat(syncStatusService.getSyncPercentage()).isCloseTo(98.0, within(0.1));
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
        void below98Percent_returnsNotSynced() {
            when(cursorService.getCursor()).thenReturn(Optional.of(cursorAt(500)));
            when(chainTipService.getTipAndCurrentEpoch()).thenReturn(tipAt(1000));

            assertThat(syncStatusService.isSynced()).isFalse();
        }

        @Test
        void at98Percent_returnsSynced() {
            when(cursorService.getCursor()).thenReturn(Optional.of(cursorAt(980)));
            when(chainTipService.getTipAndCurrentEpoch()).thenReturn(tipAt(1000));

            assertThat(syncStatusService.isSynced()).isTrue();
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

}
