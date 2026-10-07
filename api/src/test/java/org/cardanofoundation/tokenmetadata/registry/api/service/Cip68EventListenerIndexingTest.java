package org.cardanofoundation.tokenmetadata.registry.api.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.MapPlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.repository.MetadataReferenceNftRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * {@link Cip68EventListener} with the real datum parser and token service: which reference NFTs of an output are
 * indexed, and the warning left by a datum that is skipped.
 */
@DisplayName("Cip68EventListener indexing")
class Cip68EventListenerIndexingTest {

    private static final String POLICY = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String TX_HASH = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
    private static final String REF = "000643b0";
    private static final String FT = "0014df10";
    private static final String NFT = "000de140";
    private static final String RFT = "001bc280";
    private static final String A = HexUtil.encodeHexString("A".getBytes());
    private static final String B = HexUtil.encodeHexString("B".getBytes());
    private static final String C = HexUtil.encodeHexString("C".getBytes());

    /** Mainnet NFT from bloxbean/yaci-store#1159: a name, an empty image, a constructor property, no description. */
    private static final String NFT_WITHOUT_DESCRIPTION =
            "d87982a3446e616d65464e465420233145696d616765404c636f6e747261637444617461d879860181581cdc9acfee35"
            + "243d123e8f10bc58692a6bc5aa3135c7eafc2aac9daafcd87a80581c9abc17656a6d1c24688292777c18c1ce599845a5"
            + "88f4d893c1884da2d87a80d87a8001";

    private final MetadataReferenceNftRepository repository = mock(MetadataReferenceNftRepository.class);
    private final Cip68EventListener listener = new Cip68EventListener(
            new Cip68FungibleTokenService(repository), new Cip68FTDatumParser(), repository);

    private final Logger listenerLogger = (Logger) LoggerFactory.getLogger(Cip68EventListener.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        logs.start();
        listenerLogger.addAppender(logs);
    }

    @AfterEach
    void releaseLogs() {
        listenerLogger.detachAppender(logs);
    }

    @Nested
    @DisplayName("Several reference NFTs in one output")
    class SeveralReferenceNftsInOneOutput {

        @Test
        void indexesEveryReferenceNftOfANestedDatumEachWithItsOwnEntry() throws Exception {
            String datum = nestedDatum(new Entry(A, "Token A"), new Entry(B, "Token B"), new Entry(C, "Token C"));

            listener.processTransaction(event(output(datum, REF + A, REF + B, REF + C)));

            assertThat(saved()).extracting(MetadataReferenceNft::getAssetName, MetadataReferenceNft::getName)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple(REF + A, "Token A"),
                            org.assertj.core.groups.Tuple.tuple(REF + B, "Token B"),
                            org.assertj.core.groups.Tuple.tuple(REF + C, "Token C"));
            assertThat(warnings()).isEmpty();
        }

        @Test
        void indexesOnlyTheReferenceNftsThatHaveAnEntry() throws Exception {
            String datum = nestedDatum(new Entry(A, "Token A"), new Entry(C, "Token C"));

            listener.processTransaction(event(output(datum, REF + A, REF + B, REF + C)));

            assertThat(saved()).extracting(MetadataReferenceNft::getAssetName).containsExactly(REF + A, REF + C);
        }

        @Test
        void indexesOnlyTheFirstReferenceNftOfAFlatDatumAndWarns() throws Exception {
            listener.processTransaction(event(output(flatDatum("Token", "Desc", null), REF + A, REF + B)));

            assertThat(saved()).extracting(MetadataReferenceNft::getAssetName).containsExactly(REF + A);
            assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                    .contains("2 reference NFTs").contains("flat").contains(POLICY + REF + A));
        }

        @Test
        void indexesEveryTokenOfACollectionMintedWithOneOutputEach() throws Exception {
            listener.processTransaction(event(
                    output(flatDatum("Token A", "Desc", null), REF + A),
                    output(flatDatum("Token B", "Desc", null), REF + B)));

            assertThat(saved()).extracting(MetadataReferenceNft::getName).containsExactly("Token A", "Token B");
            assertThat(warnings()).isEmpty();
        }
    }

    /**
     * A datum without a name, or a fungible token without a description, is not indexed and leaves a warning. Only
     * 333 fungible tokens are served, and CIP-68 does not require a description from NFTs and RFTs, so a datum without
     * a description that is not identifiable as a fungible token is skipped without a warning.
     */
    @Nested
    @DisplayName("Skipped datums")
    class SkippedDatums {

        @Test
        void warnsWhenADatumHasNoName() throws Exception {
            listener.processTransaction(event(output(flatDatum(null, "Desc", null), REF + A)));

            verifyNoInteractions(repository);
            assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                    .contains(POLICY).contains(REF + A).contains("no name"));
        }

        @Test
        void warnsWhenAPairedFungibleTokenHasNoDescription() throws Exception {
            listener.processTransaction(event(output(flatDatum("Token", null, null), REF + A), userToken(FT + A)));

            verifyNoInteractions(repository);
            assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                    .contains(POLICY).contains(REF + A).contains("no description"));
        }

        @Test
        void warnsWhenAnUnpairedDatumWithFungibleTokenFieldsHasNoDescription() throws Exception {
            // ticker is a field only the 333 fungible token defines; its user token was minted in another transaction
            listener.processTransaction(event(output(flatDatum("Token", null, "TKN"), REF + A)));

            assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("no description"));
        }

        @Test
        void doesNotWarnForARealNftWithoutDescription() {
            listener.processTransaction(event(output(NFT_WITHOUT_DESCRIPTION, REF + A)));

            verifyNoInteractions(repository);
            assertThat(warnings()).isEmpty();
        }

        @Test
        void doesNotWarnWhenThePairedUserTokenIsAnNftOrRft() throws Exception {
            for (String label : List.of(NFT, RFT)) {
                logs.list.clear();

                listener.processTransaction(event(output(flatDatum("Token", null, "TKN"), REF + A), userToken(label + A)));

                assertThat(warnings()).as(label).isEmpty();
            }
        }

        @Test
        void ignoresAUserTokenOfAnotherBaseNameOrPolicy() throws Exception {
            String otherPolicy = "11223344aabbccdd11223344aabbccdd11223344aabbccdd11223344";
            AddressUtxo unrelated = AddressUtxo.builder().txHash(TX_HASH).amounts(List.of(
                    amount(POLICY + FT + B), amount(otherPolicy + FT + A))).build();

            listener.processTransaction(event(output(flatDatum("Token", null, null), REF + A), unrelated));

            assertThat(warnings()).isEmpty();
        }
    }

    private List<MetadataReferenceNft> saved() {
        ArgumentCaptor<MetadataReferenceNft> captor = ArgumentCaptor.forClass(MetadataReferenceNft.class);
        verify(repository, atLeast(1)).save(captor.capture());
        return captor.getAllValues();
    }

    private List<String> warnings() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).toList();
    }

    private static AddressUtxoEvent event(AddressUtxo... outputs) {
        return AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(100L).build())
                .txInputOutputs(List.of(TxInputOutput.builder().outputs(Arrays.asList(outputs)).build()))
                .build();
    }

    private static AddressUtxo output(String datum, String... referenceNftAssetNames) {
        List<Amt> amounts = new ArrayList<>();
        for (String assetName : referenceNftAssetNames) {
            amounts.add(amount(POLICY + assetName));
        }
        return AddressUtxo.builder().txHash(TX_HASH).inlineDatum(datum).amounts(amounts).build();
    }

    private static AddressUtxo userToken(String assetName) {
        return AddressUtxo.builder().txHash(TX_HASH).amounts(List.of(amount(POLICY + assetName))).build();
    }

    private static Amt amount(String unit) {
        return Amt.builder().unit(unit).quantity(BigInteger.ONE).build();
    }

    private static String flatDatum(String name, String description, String ticker) throws Exception {
        MapPlutusData metadata = new MapPlutusData();
        if (name != null) {
            metadata.put(BytesPlutusData.of("name"), BytesPlutusData.of(name));
        }
        if (description != null) {
            metadata.put(BytesPlutusData.of("description"), BytesPlutusData.of(description));
        }
        if (ticker != null) {
            metadata.put(BytesPlutusData.of("ticker"), BytesPlutusData.of(ticker));
        }
        return serialize(metadata, 1);
    }

    /** One entry of a nested datum: the base name (hex, without the label prefix) and the token name. */
    private record Entry(String baseName, String name) {
    }

    /** {"721": {policy: {base name: {name, description}}}}, version 4, for the given entries. */
    private static String nestedDatum(Entry... entries) throws Exception {
        MapPlutusData byAsset = new MapPlutusData();
        for (Entry entry : entries) {
            MapPlutusData metadata = new MapPlutusData();
            metadata.put(BytesPlutusData.of("name"), BytesPlutusData.of(entry.name()));
            metadata.put(BytesPlutusData.of("description"), BytesPlutusData.of("Desc"));
            byAsset.put(BytesPlutusData.of(HexUtil.decodeHexString(entry.baseName())), metadata);
        }
        MapPlutusData byPolicy = new MapPlutusData();
        byPolicy.put(BytesPlutusData.of(HexUtil.decodeHexString(POLICY)), byAsset);
        MapPlutusData root = new MapPlutusData();
        root.put(BytesPlutusData.of("721"), byPolicy);
        return serialize(root, 4);
    }

    private static String serialize(MapPlutusData metadata, long version) throws Exception {
        ConstrPlutusData datum = ConstrPlutusData.of(0, metadata, BigIntPlutusData.of(version));
        return HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));
    }
}
