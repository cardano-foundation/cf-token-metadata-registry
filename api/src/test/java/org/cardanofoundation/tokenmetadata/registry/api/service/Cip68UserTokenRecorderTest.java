package org.cardanofoundation.tokenmetadata.registry.api.service;

import org.cardanofoundation.tokenmetadata.registry.entity.Cip68UserToken;
import org.cardanofoundation.tokenmetadata.registry.repository.Cip68UserTokenRepository;
import org.cardanofoundation.tokenmetadata.registry.repository.Cip68UserTokenWriter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InOrder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * The recorder of the CIP-68 user tokens seen on chain (ADR-022): it writes what is new or seen earlier than recorded, in
 * a fixed order, and never throws into the block transaction.
 */
class Cip68UserTokenRecorderTest {

    private static final String POLICY = "aa".repeat(28);
    private static final String FT_A = POLICY + "0014df1041";
    private static final String RFT_B = POLICY + "001bc28042";

    private final Cip68UserTokenRepository repository = mock(Cip68UserTokenRepository.class);
    private final Cip68UserTokenWriter writer = mock(Cip68UserTokenWriter.class);
    private final Cip68UserTokenRecorder recorder = new Cip68UserTokenRecorder(repository, writer);

    @Nested
    class RecordSeen {

        @Test
        void writesTokensSeenForTheFirstTime() {
            when(repository.findAllById(Map.of(FT_A, "tx1").keySet())).thenReturn(List.of());

            recorder.recordSeen(Map.of(FT_A, "tx1"), 100L);

            verify(writer).upsertFirstSeen(FT_A, "tx1", 100L);
        }

        @Test
        void leavesAloneATokenRecordedAtTheSameOrAnEarlierSlot() {
            when(repository.findAllById(Map.of(FT_A, "tx1", RFT_B, "tx1").keySet()))
                    .thenReturn(List.of(new Cip68UserToken(FT_A, 100L, "tx0"), new Cip68UserToken(RFT_B, 50L, "tx0")));

            recorder.recordSeen(Map.of(FT_A, "tx1", RFT_B, "tx1"), 100L);

            verify(writer, never()).upsertFirstSeen(anyString(), anyString(), anyLong());
        }

        @Test
        void lowersATokenRecordedAtALaterSlot() {
            // with parallel block processing a later block can commit first
            when(repository.findAllById(Map.of(FT_A, "tx1").keySet())).thenReturn(List.of(new Cip68UserToken(FT_A, 200L, "tx2")));

            recorder.recordSeen(Map.of(FT_A, "tx1"), 100L);

            verify(writer).upsertFirstSeen(FT_A, "tx1", 100L);
        }

        @Test
        void writesInAFixedOrder() {
            // two transactions that write the same tokens in the same order cannot wait for each other
            Map<String, String> seen = Map.of(RFT_B, "tx1", FT_A, "tx1");
            when(repository.findAllById(seen.keySet())).thenReturn(List.of());

            recorder.recordSeen(seen, 100L);

            InOrder order = inOrder(writer);
            order.verify(writer).upsertFirstSeen(FT_A, "tx1", 100L);
            order.verify(writer).upsertFirstSeen(RFT_B, "tx1", 100L);
        }

        @Test
        void doesNothingForABlockWithoutUserTokens() {
            recorder.recordSeen(Map.of(), 100L);

            verifyNoInteractions(repository, writer);
        }

        @Test
        void aFailureIsAWarningNotAnException() {
            // the recorder runs in the block transaction, where an exception stops the whole sync
            when(repository.findAllById(Map.of(FT_A, "tx1").keySet())).thenThrow(new IllegalStateException("database down"));

            assertThatCode(() -> recorder.recordSeen(Map.of(FT_A, "tx1"), 100L)).doesNotThrowAnyException();
        }
    }

    @Nested
    class IsFungibleOrRichFungibleUnit {

        @ParameterizedTest
        @ValueSource(strings = {"0014df1041", "001bc28042"})
        void acceptsAFungibleOrRichFungibleTokenWithAName(String assetName) {
            assertThat(Cip68UserTokenRecorder.isFungibleOrRichFungibleUnit(POLICY + assetName)).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"000643b041", "000de14041", "0014df10", "001bc280", "41", ""})
        void rejectsAReferenceNftAnNftALabelWithoutANameAndOtherAssets(String assetName) {
            assertThat(Cip68UserTokenRecorder.isFungibleOrRichFungibleUnit(POLICY + assetName)).isFalse();
        }
    }
}
