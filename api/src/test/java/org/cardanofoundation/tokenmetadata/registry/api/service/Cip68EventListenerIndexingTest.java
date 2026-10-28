package org.cardanofoundation.tokenmetadata.registry.api.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ListPlutusData;
import com.bloxbean.cardano.client.plutus.spec.MapPlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.core.protocol.chainsync.messages.Point;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.invalidtoken.InvalidTokenReason;
import org.cardanofoundation.tokenmetadata.registry.invalidtoken.InvalidTokenRecorder;
import org.cardanofoundation.tokenmetadata.registry.invalidtoken.InvalidTokenReport;
import org.cardanofoundation.tokenmetadata.registry.repository.MetadataReferenceNftRepository;
import org.cardanofoundation.tokenmetadata.registry.validation.FieldCategory;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;
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

import static org.cardanofoundation.tokenmetadata.registry.api.model.cip68.Cip68Constants.LABEL_FT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The CIP-68 listener with the real parser and validator: a datum that can be decoded is stored as written, with what
 * departs from CIP-68 in its validation errors, per label (ADR-019); only 333 and 444 datums are stored; what cannot be
 * decoded, or fails unexpectedly, is recorded in {@code invalid_token} (ADR-020).
 */
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

    private final MetadataReferenceNftRepository repository = mock(MetadataReferenceNftRepository.class);
    private final InvalidTokenRecorder recorder = mock(InvalidTokenRecorder.class);
    private final Cip68FTDatumParser parser = new Cip68FTDatumParser();
    private final Cip68EventListener listener = new Cip68EventListener(
            new Cip68FungibleTokenService(repository, parser), parser, repository, recorder);

    private final Logger listenerLogger = (Logger) LoggerFactory.getLogger(Cip68EventListener.class);
    private final Logger parserLogger = (Logger) LoggerFactory.getLogger(Cip68FTDatumParser.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        logs.start();
        listenerLogger.addAppender(logs);
        parserLogger.addAppender(logs);
    }

    @AfterEach
    void releaseLogs() {
        listenerLogger.detachAppender(logs);
        parserLogger.detachAppender(logs);
    }

    @Nested
    @DisplayName("A datum that follows CIP-68")
    class ValidDatums {

        @Test
        void storesAFungibleTokenWithNoErrors() throws Exception {
            listener.processTransaction(event(output(flatDatum("Token", "Desc", "TKN"), REF + A), userToken(FT + A)));

            assertThat(saved()).singleElement().satisfies(row -> {
                assertThat(row.getName()).isEqualTo("Token");
                assertThat(row.getTicker()).isEqualTo("TKN");
                assertThat(row.getValidationErrors()).isEmpty();
            });
            assertThat(warnings()).isEmpty();
        }

        @Test
        void storesARichFungibleTokenWithItsImageAndNoDescription() throws Exception {
            listener.processTransaction(event(output(datum("name", "Token", "image", "ipfs://bafy", "mediaType", "image/png"),
                    REF + A), userToken(RFT + A)));

            assertThat(saved()).singleElement().satisfies(row -> {
                assertThat(row.getImage()).isEqualTo("ipfs://bafy");
                assertThat(row.getMediaType()).isEqualTo("image/png");
                assertThat(row.getDescription()).isNull();
                assertThat(row.getValidationErrors()).isEmpty();
            });
        }

        @Test
        void keepsTheAdditionalPropertiesAsJson() throws Exception {
            listener.processTransaction(event(output(datum("name", "Token", "description", "Desc", "website", "https://x.io"),
                    REF + A), userToken(FT + A)));

            assertThat(saved()).singleElement().satisfies(row ->
                    assertThat(row.getProperties()).containsKey("additional_properties"));
        }
    }

    @Nested
    @DisplayName("A datum that breaks CIP-68 is stored, and served with its errors")
    class DatumsWithErrors {

        @Test
        void storesAFungibleTokenWithoutDescriptionAndServesItWithAMandatoryError() throws Exception {
            listener.processTransaction(event(output(flatDatum("Token", null, "TKN"), REF + A), userToken(FT + A)));

            // nothing is wrong with the datum itself; what a 333 requires is checked when it is served
            assertThat(saved()).singleElement().satisfies(row -> assertThat(row.getValidationErrors()).isEmpty());
            assertThat(saved()).singleElement().satisfies(row -> assertThat(Cip68Validator.validate(row, LABEL_FT))
                    .extracting(ValidationError::subStandard, ValidationError::fieldName, ValidationError::fieldCategory,
                            ValidationError::code)
                    .containsExactly(tuple("333", "description", FieldCategory.MANDATORY, "missing_value")));
            // what is indexed with errors is a DEBUG line, not a WARN: the errors are the report
            assertThat(warnings()).isEmpty();
        }

        @Test
        void storesADatumWithoutNameAndServesItWithAMandatoryError() throws Exception {
            listener.processTransaction(event(output(flatDatum(null, "Desc", "TKN"), REF + A), userToken(FT + A)));

            assertThat(saved()).singleElement().satisfies(row -> {
                assertThat(row.getName()).isNull();
                assertThat(Cip68Validator.validate(row, LABEL_FT)).extracting(ValidationError::fieldName).containsExactly("name");
            });
        }

        @Test
        void keepsABareCidLogoAsWrittenWithAnOptionalError() throws Exception {
            listener.processTransaction(event(output(flatDatum("Token", "Desc", "TKN", "QmXyz"), REF + A), userToken(FT + A)));

            assertThat(saved()).singleElement().satisfies(row -> {
                assertThat(row.getLogo()).isEqualTo("QmXyz");
                assertThat(row.getValidationErrors()).singleElement().satisfies(error -> {
                    assertThat(error.fieldName()).isEqualTo("logo");
                    assertThat(error.fieldCategory()).isEqualTo(FieldCategory.OPTIONAL);
                    assertThat(error.code()).isEqualTo("invalid_uri_scheme");
                    assertThat(error.subStandard()).isNull();
                });
            });
        }

        @Test
        void servesTheFungibleTokenOfAReferenceNftPairedWithAnNftWithoutTheNftRequirement() throws Exception {
            // the SundaeSwap LP case: a pool NFT (222) and the LP token (333) share one reference NFT; the NFT, which
            // needs an image, is not served, and the LP token has what a 333 needs
            listener.processTransaction(event(output(flatDatum("LP", "Pool share", "LP"), REF + A),
                    userToken(NFT + A), userToken(FT + A)));

            assertThat(saved()).singleElement().satisfies(row -> {
                assertThat(row.getValidationErrors()).isEmpty();
                assertThat(Cip68Validator.validate(row, LABEL_FT)).isEmpty();
            });
        }

        /** Mainnet: 570 fungible tokens give their description as 64-byte chunks (Templar Protocol, ...). */
        @Test
        void servesAChunkedDescriptionWithOneMandatoryErrorFor333() throws Exception {
            MapPlutusData metadata = map("name", BytesPlutusData.of("Templar Protocol"));
            metadata.put(BytesPlutusData.of("description"), ListPlutusData.of(
                    BytesPlutusData.of("Templar Protocol - Templar lets "), BytesPlutusData.of("you borrow")));

            listener.processTransaction(event(output(serialize(metadata, 1), REF + A), userToken(FT + A)));

            assertThat(saved()).singleElement().satisfies(row -> {
                assertThat(row.getDescription()).isEqualTo("Templar Protocol - Templar lets you borrow");
                // stored without a label, served with the category of the label: only a 333 requires a description
                assertThat(row.getValidationErrors()).extracting(ValidationError::subStandard, ValidationError::code)
                        .containsExactly(tuple(null, "chunked_text"));
                assertThat(Cip68Validator.validate(row, LABEL_FT))
                        .extracting(ValidationError::subStandard, ValidationError::fieldName, ValidationError::fieldCategory,
                                ValidationError::code)
                        .containsExactly(tuple("333", "description", FieldCategory.MANDATORY, "chunked_text"));
            });
        }

        /** Mainnet: Greenland Reserve Coin, whose datum declares version 0. */
        @Test
        void storesARealVersion0FungibleTokenWithAnInvalidVersionError() {
            String gnrcPolicy = "67cee89d59ab5354ee22c8af0638224126aecc6210f9372a61f13a64";
            String gnrcReferenceNft = "000643b0474e5243";
            String gnrcDatum = "d8799fa6446e616d6556477265656e6c616e64205265736572766520436f696e4b6465736372697074696f6e583c4173"
                    + "736574206261636b656420746f6b656e207365637572656420627920477265656e6c616e642072756269657320616e64"
                    + "20736170706869726573467469636b657244474e52434375726c582368747470733a2f2f7777772e7468652d6d696e74"
                    + "2e636f6d2f636f6d706c69616e6365446c6f676f4048646563696d616c730600d866821a951b3c2b9f81581cc0bb241d"
                    + "37ffbdfdbb07d3d34bff54671c00935128da06966bc033810103ffff";
            AddressUtxo output = AddressUtxo.builder().txHash(TX_HASH).inlineDatum(gnrcDatum)
                    .amounts(List.of(amount(gnrcPolicy + gnrcReferenceNft))).build();

            listener.processTransaction(event(output));

            assertThat(saved()).singleElement().satisfies(row -> {
                assertThat(row.getName()).isEqualTo("Greenland Reserve Coin");
                assertThat(row.getVersion()).isZero();
                assertThat(row.getValidationErrors()).extracting(ValidationError::fieldName, ValidationError::code)
                        .contains(tuple("version", "invalid_version"));
            });
        }
    }

    /**
     * The registry serves fungible (333) and rich fungible (444) tokens only: a datum minted with a 222 NFT only is
     * skipped, unless it updates a reference NFT that is already stored. A datum without a user token in its transaction
     * is stored, whatever it looks like: its 333 or 444 token can be minted in another transaction.
     */
    @Nested
    @DisplayName("Which datums are stored")
    class StoredLabels {

        @Test
        void skipsADatumOfAnNftOnly() throws Exception {
            listener.processTransaction(event(output(datum("name", "Token", "image", "ipfs://bafy"), REF + A), userToken(NFT + A)));

            verify(repository, never()).saveAll(any());
            assertThat(warnings()).isEmpty();
        }

        @Test
        void storesAnUpdateWithoutTheUserTokenOfAReferenceNftThatIsAlreadyStored() throws Exception {
            // without decimals, the update of an RFT reads as a 222 NFT
            when(repository.existsByPolicyIdAndAssetName(POLICY, REF + A)).thenReturn(true);

            listener.processTransaction(event(output(datum("name", "Token v2", "image", "ipfs://bafy"), REF + A)));

            assertThat(saved()).singleElement().satisfies(row -> assertThat(row.getName()).isEqualTo("Token v2"));
        }

        @Test
        void storesAnUnpairedFungibleTokenDatum() throws Exception {
            listener.processTransaction(event(output(flatDatum("Token", "Desc", "TKN"), REF + A)));

            assertThat(saved()).hasSize(1);
        }

        /**
         * Mainnet: the GOAT RFTs (e987e459…) minted their reference NFTs before their 444 tokens, and BushWifPlanesTier1
         * (cc816014…) its 444 token before its reference NFT. Their datums have an image and no decimals, so they read
         * as an NFT's: the datum is stored, and served under its 444 subject.
         */
        @Test
        void storesAnUnpairedDatumThatReadsAsAnNft() throws Exception {
            listener.processTransaction(event(output(datum("name", "GOATah", "description", "GOAT", "image", "ipfs://bafy"), REF + A)));

            assertThat(saved()).singleElement().satisfies(row -> assertThat(row.getName()).isEqualTo("GOATah"));
        }

        @Test
        void storesADatumPairedWithAnRftAndAnNft() throws Exception {
            listener.processTransaction(event(output(datum("name", "Token", "image", "ipfs://bafy"), REF + A),
                    userToken(NFT + A), userToken(RFT + A)));

            assertThat(saved()).hasSize(1);
        }

        /** Mainnet: Atrium profiles and HouseOfTitans' empty datums were indexed as 333 fungible tokens by default. */
        @Test
        void skipsAnUnpairedDatumWithNoneOfTheTokenFields() throws Exception {
            listener.processTransaction(event(
                    output(datum("mainnet-testing", "this is an updated testnet profile."), REF + A),
                    output(datum(), REF + B),
                    output(serialize(map("insert_fee", BigIntPlutusData.of(30_000_000)), 1), REF + C)));

            verify(repository, never()).saveAll(any());
            assertThat(warnings()).isEmpty();
        }

        @Test
        void storesADatumWithNoneOfTheTokenFieldsWhenItsUserTokenIsInTheTransaction() throws Exception {
            // the minting transaction says what the token is: a 333 without metadata is stored, and served with its errors
            listener.processTransaction(event(output(datum("mainnet-testing", "profile"), REF + A), userToken(FT + A)));

            assertThat(saved()).singleElement().satisfies(row -> assertThat(Cip68Validator.validate(row, LABEL_FT))
                    .extracting(ValidationError::fieldName).containsExactly("name", "description"));
        }

        @Test
        void storesAnUnpairedDatumWithNoneOfTheTokenFieldsOfAReferenceNftThatIsAlreadyStored() throws Exception {
            // an owner who clears the metadata must not be served the old one
            when(repository.existsByPolicyIdAndAssetName(POLICY, REF + A)).thenReturn(true);

            listener.processTransaction(event(output(datum(), REF + A)));

            assertThat(saved()).singleElement().satisfies(row -> assertThat(row.getName()).isNull());
        }

        @Test
        void storesAnUnpairedDatumWhoseOnlyTokenFieldHasTheWrongType() throws Exception {
            // the field is there, so it is token metadata, broken
            listener.processTransaction(event(output(serialize(map("name", BigIntPlutusData.of(7)), 1), REF + A)));

            assertThat(saved()).singleElement().satisfies(row -> assertThat(row.getValidationErrors())
                    .extracting(ValidationError::fieldName, ValidationError::code)
                    .contains(tuple("name", "wrong_type")));
        }
    }

    @Nested
    @DisplayName("Several reference NFTs in one output")
    class SeveralReferenceNftsInOneOutput {

        @Test
        void indexesEveryReferenceNftOfANestedDatumEachWithItsOwnEntry() throws Exception {
            String datum = nestedDatum(new Entry(A, "Token A"), new Entry(B, "Token B"), new Entry(C, "Token C"));

            listener.processTransaction(event(output(datum, REF + A, REF + B, REF + C)));

            assertThat(saved()).extracting(MetadataReferenceNft::getAssetName, MetadataReferenceNft::getName)
                    .containsExactly(tuple(REF + A, "Token A"), tuple(REF + B, "Token B"), tuple(REF + C, "Token C"));
        }

        @Test
        void storesAReferenceNftWithNoNestedEntryWithAnError() throws Exception {
            String datum = nestedDatum(new Entry(A, "Token A"), new Entry(C, "Token C"));

            listener.processTransaction(event(output(datum, REF + A, REF + B, REF + C)));

            assertThat(saved()).extracting(MetadataReferenceNft::getAssetName).containsExactly(REF + A, REF + B, REF + C);
            assertThat(saved().get(1).getValidationErrors()).extracting(ValidationError::code).contains("no_nested_entry");
        }

        @Test
        void indexesEveryReferenceNftOfAFlatDatumWithOneInfoLine() throws Exception {
            listener.processTransaction(event(output(flatDatum("Token", "Desc", null), REF + A, REF + B)));

            assertThat(saved()).extracting(MetadataReferenceNft::getAssetName, MetadataReferenceNft::getName)
                    .containsExactly(tuple(REF + A, "Token"), tuple(REF + B, "Token"));
            assertThat(messages(Level.INFO)).anySatisfy(info -> assertThat(info)
                    .contains("2 reference NFTs").contains("flat").contains(POLICY + REF + A).contains(POLICY + REF + B));
        }

        @Test
        void recordsAnUndecodableSharedDatumOnceForTheOutput() {
            listener.processTransaction(event(output("deadbeef", REF + A, REF + B)));

            verify(repository, never()).saveAll(any());
            verify(recorder).report(any());
        }
    }

    /** What cannot be decoded is dropped and recorded in invalid_token (ADR-020). */
    @Nested
    @DisplayName("Dropped datums")
    class DroppedDatums {

        @Test
        void recordsAnUndecodableDatum() {
            listener.processTransaction(event(output("deadbeef", REF + A)));

            verify(repository, never()).saveAll(any());
            InvalidTokenReport report = recorded();
            assertThat(report.standard()).isEqualTo(InvalidTokenReason.CIP_68);
            assertThat(report.reasonCode()).isEqualTo(InvalidTokenReason.DATUM_UNDECODABLE);
            assertThat(report.policyId()).isEqualTo(POLICY);
            assertThat(report.assetName()).isEqualTo(REF + A);
            assertThat(report.txHash()).isEqualTo(TX_HASH);
            assertThat(report.slot()).isEqualTo(100L);
        }

        @Test
        void ignoresADatumThatIsNotACip68Datum() throws Exception {
            // a reference NFT output can carry any datum: one that is not CIP-68 is neither stored nor recorded
            String notCip68 = HexUtil.encodeHexString(CborSerializationUtil.serialize(BigIntPlutusData.of(42).serialize()));

            listener.processTransaction(event(output(notCip68, REF + A)));

            verify(repository, never()).saveAll(any());
            verify(recorder, never()).report(any());
        }
    }

    /**
     * The listener runs inside yaci-store's block transaction, so an exception would stop the whole sync. A reference NFT
     * whose indexing fails unexpectedly is skipped, recorded, and the others are indexed.
     */
    @Nested
    @DisplayName("Unexpected failure")
    class UnexpectedFailure {

        private final Cip68FTDatumParser failingParser = spy(new Cip68FTDatumParser());
        private final Cip68EventListener failingListener = new Cip68EventListener(
                new Cip68FungibleTokenService(repository, failingParser), failingParser, repository, recorder);

        @Test
        void skipsTheReferenceNftThatFailsRecordsItAndIndexesTheOthers() throws Exception {
            String failing = flatDatum("Broken", "Desc", null);
            doThrow(new IllegalStateException("unforeseen datum")).when(failingParser).parseDetailed(eq(failing), any(AssetType.class));

            failingListener.processTransaction(event(
                    output(flatDatum("Token A", "Desc", null), REF + A),
                    output(failing, REF + B),
                    output(flatDatum("Token C", "Desc", null), REF + C)));

            assertThat(saved()).extracting(MetadataReferenceNft::getName).containsExactly("Token A", "Token C");
            assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                    .contains(TX_HASH).contains("failed unexpectedly").contains("IllegalStateException").contains("unforeseen datum"));
            InvalidTokenReport report = recorded();
            assertThat(report.reasonCode()).isEqualTo(InvalidTokenReason.PROCESSING_FAILURE);
            assertThat(report.assetName()).isEqualTo(REF + B);
        }
    }

    @Nested
    @DisplayName("Rollback")
    class Rollback {

        @Test
        void deletesTheRowsAfterTheRollbackSlot() {
            when(repository.deleteBySlotGreaterThan(500L)).thenReturn(5);

            listener.handleRollback(RollbackEvent.builder().rollbackTo(new Point(500L, "blockhash")).build());

            verify(repository).deleteBySlotGreaterThan(500L);
        }
    }

    /** The rows of the single batched save the listener makes per event. */
    private List<MetadataReferenceNft> saved() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MetadataReferenceNft>> captor = ArgumentCaptor.forClass(List.class);
        verify(repository).saveAll(captor.capture());
        return captor.getValue();
    }

    private InvalidTokenReport recorded() {
        ArgumentCaptor<InvalidTokenReport> report = ArgumentCaptor.forClass(InvalidTokenReport.class);
        verify(recorder).report(report.capture());
        return report.getValue();
    }

    private List<String> warnings() {
        return messages(Level.WARN);
    }

    private List<String> messages(Level level) {
        return logs.list.stream().filter(e -> e.getLevel() == level).map(ILoggingEvent::getFormattedMessage).toList();
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
        return flatDatum(name, description, ticker, null);
    }

    private static String flatDatum(String name, String description, String ticker, String logo) throws Exception {
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
        if (logo != null) {
            metadata.put(BytesPlutusData.of("logo"), BytesPlutusData.of(logo));
        }
        return serialize(metadata, 1);
    }

    /** A flat version 1 datum with the given text properties, as key-value pairs. */
    private static String datum(String... keyValues) throws Exception {
        MapPlutusData metadata = new MapPlutusData();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            metadata.put(BytesPlutusData.of(keyValues[i]), BytesPlutusData.of(keyValues[i + 1]));
        }
        return serialize(metadata, 1);
    }

    /** A metadata map with one entry. */
    private static MapPlutusData map(String key, PlutusData value) {
        MapPlutusData metadata = new MapPlutusData();
        metadata.put(BytesPlutusData.of(key), value);
        return metadata;
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
