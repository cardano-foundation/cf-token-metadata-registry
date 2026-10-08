package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ListPlutusData;
import com.bloxbean.cardano.client.plutus.spec.MapPlutusData;
import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bloxbean.cardano.client.util.HexUtil;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
class Cip68FTDatumParserTest {

    private final Cip68FTDatumParser cip68FTDatumParser = new Cip68FTDatumParser();

    private final Logger parserLogger = (Logger) LoggerFactory.getLogger(Cip68FTDatumParser.class);
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();

    @BeforeEach
    void captureLogs() {
        logs.start();
        parserLogger.addAppender(logs);
    }

    @AfterEach
    void releaseLogs() {
        parserLogger.detachAppender(logs);
    }

    private List<String> warnings() {
        return logs.list.stream().filter(e -> e.getLevel() == Level.WARN).map(ILoggingEvent::getFormattedMessage).toList();
    }

    @Test
    void parseFLDTDatumTest() {

        String fldtCip68Datum = "d8799fa648646563696d616c73064b6465736372697074696f6e5f5840546865206f6666696369616c20746f6b656e206f6620466c756964546f6b656e732c2061206c656164696e6720446546692065636f73797374656d206675656c5827656420627920696e6e6f766174696f6e20616e6420636f6d6d756e697479206261636b696e672eff446c6f676f582068747470733a2f2f666c756964746f6b656e732e636f6d2f666c64742e706e67446e616d6544464c4454467469636b657244464c44544777656273697465581868747470733a2f2f666c756964746f6b656e732e636f6d2f0101ff";

        Optional<FungibleTokenMetadata> tokenMetadataOpt = cip68FTDatumParser.parse(fldtCip68Datum);

        if (tokenMetadataOpt.isEmpty()) {
            Assertions.fail();
        }

        FungibleTokenMetadata tokenMetadata = tokenMetadataOpt.get();

        Assertions.assertEquals(new FungibleTokenMetadata(6L,
                        "The official token of FluidTokens, a leading DeFi ecosystem fueled by innovation and community backing.",
                        "https://fluidtokens.com/fldt.png",
                        "FLDT",
                        "FLDT",
                        null,
                        1L),
                tokenMetadata);


    }

    @Test
    void parseUSDMDatumTest() {

        String fldtCip68Datum = "d8799fa7446e616d65445553444d4b6465736372697074696f6e5837466961742d6261636b656420737461626c65636f696e206e617469766520746f207468652043617264616e6f20626c6f636b636861696e467469636b6572445553444d4375726c5168747470733a2f2f6d6568656e2e696f2f446c6f676f5835697066733a2f2f516d5078596570454648747533474252754b3652684c35774b72536d7867596a624575384341644677344467687148646563696d616c7306456c6567616c582868747470733a2f2f6d6568656e2e696f2f6d6568656e5f7465726d735f6f665f736572766963652f01ff";

        Optional<FungibleTokenMetadata> tokenMetadataOpt = cip68FTDatumParser.parse(fldtCip68Datum);

        if (tokenMetadataOpt.isEmpty()) {
            Assertions.fail();
        }

        FungibleTokenMetadata tokenMetadata = tokenMetadataOpt.get();

        Assertions.assertEquals(new FungibleTokenMetadata(6L,
                        "Fiat-backed stablecoin native to the Cardano blockchain",
                        "ipfs://QmPxYepEFHtu3GBRuK6RhL5wKrSmxgYjbEu8CAdFw4Dghq",
                        "USDM",
                        "USDM",
                        "https://mehen.io/",
                        1L),
                tokenMetadata);


    }

    private static String datumHex(MapPlutusData properties, BigInteger version) throws Exception {
        ConstrPlutusData datum = ConstrPlutusData.of(0, properties, BigIntPlutusData.of(version));
        return HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize()));
    }

    private static MapPlutusData metadata(String name) {
        MapPlutusData metadata = new MapPlutusData();
        metadata.put(BytesPlutusData.of("name"), BytesPlutusData.of(name));
        metadata.put(BytesPlutusData.of("description"), BytesPlutusData.of("Desc"));
        return metadata;
    }

    @Nested
    class OutOfRangeDecimals {

        private Optional<FungibleTokenMetadata> parseWithDecimals(BigInteger decimals) throws Exception {
            MapPlutusData properties = metadata("Token");
            properties.put(BytesPlutusData.of("decimals"), BigIntPlutusData.of(decimals));
            return cip68FTDatumParser.parse(datumHex(properties, BigInteger.ONE));
        }

        @Test
        void shouldKeepDecimalsAtUpperBound() throws Exception {
            assertThat(parseWithDecimals(BigInteger.valueOf(255)))
                    .hasValueSatisfying(m -> assertThat(m.decimals()).isEqualTo(255L));
        }

        @Test
        void shouldDropDecimalsAboveUpperBoundButKeepMetadata() throws Exception {
            assertThat(parseWithDecimals(BigInteger.valueOf(256))).hasValueSatisfying(m -> {
                assertThat(m.name()).isEqualTo("Token");
                assertThat(m.decimals()).isNull();
            });
        }

        @Test
        void shouldDropDecimalsBeyondIntRange() throws Exception {
            assertThat(parseWithDecimals(BigInteger.TWO.pow(40)))
                    .hasValueSatisfying(m -> assertThat(m.decimals()).isNull());
        }

        @Test
        void shouldDropDecimalsThatWouldWrapWhenNarrowedToLong() throws Exception {
            // 2^64 + 3: longValue() gives 3, which would otherwise look valid
            assertThat(parseWithDecimals(BigInteger.TWO.pow(64).add(BigInteger.valueOf(3))))
                    .hasValueSatisfying(m -> assertThat(m.decimals()).isNull());
        }

        @Test
        void shouldDropNegativeDecimals() throws Exception {
            assertThat(parseWithDecimals(BigInteger.valueOf(-1)))
                    .hasValueSatisfying(m -> assertThat(m.decimals()).isNull());
        }
    }

    @Nested
    class OutOfRangeVersion {

        @Test
        void shouldRejectDatumWhoseVersionWouldWrapWhenNarrowedToLong() throws Exception {
            // 2^64 + 1: longValue() gives 1, which would otherwise look like a valid version
            assertThat(cip68FTDatumParser.parse(datumHex(metadata("Token"), BigInteger.TWO.pow(64).add(BigInteger.ONE))))
                    .isEmpty();
        }

        @Test
        void shouldRejectEveryVersionBeyondLongWhateverItNarrowsTo() throws Exception {
            // 2^63 narrows to Long.MIN_VALUE, 2^64 + 4 to 4 (a defined version): both must be rejected, not narrowed
            for (BigInteger version : List.of(BigInteger.TWO.pow(63), BigInteger.TWO.pow(64).add(BigInteger.valueOf(4)))) {
                assertThat(cip68FTDatumParser.parse(datumHex(metadata("Token"), version))).as(version.toString()).isEmpty();
            }
        }
    }

    @Nested
    class ChunkedLogo {

        @Test
        void shouldJoinLogoGivenAsListOfChunks() throws Exception {
            // CIP-68 FT: logo is a uri = bounded_bytes / [* bounded_bytes]
            String first = "data:image/png;base64,";
            String second = "iVBORw0KGgo=";
            MapPlutusData properties = metadata("Token");
            properties.put(BytesPlutusData.of("logo"), ListPlutusData.of(BytesPlutusData.of(first), BytesPlutusData.of(second)));

            assertThat(cip68FTDatumParser.parse(datumHex(properties, BigInteger.ONE)))
                    .hasValueSatisfying(m -> assertThat(m.logo()).isEqualTo(first + second));
        }

        @Test
        void shouldSkipChunksThatAreNotByteStrings() throws Exception {
            MapPlutusData properties = metadata("Token");
            properties.put(BytesPlutusData.of("logo"), ListPlutusData.of(
                    BytesPlutusData.of("ipfs://"), BigIntPlutusData.of(42), BytesPlutusData.of("QmLogo")));

            assertThat(cip68FTDatumParser.parse(datumHex(properties, BigInteger.ONE)))
                    .hasValueSatisfying(m -> assertThat(m.logo()).isEqualTo("ipfs://QmLogo"));
        }

        @Test
        void shouldDropLogoGivenAsEmptyListButKeepMetadata() throws Exception {
            MapPlutusData properties = metadata("Token");
            properties.put(BytesPlutusData.of("logo"), ListPlutusData.of());

            assertThat(cip68FTDatumParser.parse(datumHex(properties, BigInteger.ONE))).hasValueSatisfying(m -> {
                assertThat(m.name()).isEqualTo("Token");
                assertThat(m.logo()).isNull();
            });
        }
    }

    @Nested
    class DeeplyNestedDatum {

        /** {@code depth} nested single-element lists around 0, built as raw CBOR so building it doesn't recurse. */
        private String nestedDatumHex(int depth) {
            return "81".repeat(depth) + "00";
        }

        /** Parses on a 1 MB stack, the default on Linux x86_64, and returns the result or what was thrown. */
        private Object parseOnDefaultLinuxStack(String datumHex) throws InterruptedException {
            AtomicReference<Object> outcome = new AtomicReference<>();
            Thread thread = new Thread(null, () -> {
                try {
                    outcome.set(cip68FTDatumParser.parse(datumHex));
                } catch (Throwable t) {
                    outcome.set(t);
                }
            }, "deep-datum", 1024 * 1024);
            thread.start();
            thread.join();
            return outcome.get();
        }

        @Test
        void shouldSkipDatumNestedTooDeeplyToDecode() throws Exception {
            // 5,000 levels overflows the current decoder on a 1 MB stack; 16,000 is about the most a
            // 16 KB transaction can carry. This stays valid once the decoder is stack-safe
            // (cardano-client-lib#681): the datum isn't a CIP-68 constructor, so it is skipped either way.
            for (int depth : new int[]{5_000, 16_000}) {
                assertThat(parseOnDefaultLinuxStack(nestedDatumHex(depth)))
                        .as("depth %d", depth)
                        .isEqualTo(Optional.empty());
            }
        }
    }

    @Nested
    class NestedMapFormat {

        private static final String POLICY_ID = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
        private static final String OTHER_POLICY_ID = "11223344aabbccdd11223344aabbccdd11223344aabbccdd11223344";
        private static final String ASSET_NAME_HEX = HexUtil.encodeHexString("Token".getBytes());
        private static final AssetType REFERENCE_NFT = new AssetType(POLICY_ID, "000643b0" + ASSET_NAME_HEX);

        /** {"721": {policy_id: {asset_name: metadata}}} with raw-byte policy and asset keys, per CIP-68 version 4. */
        private static MapPlutusData nested(String policyId, String assetNameHex, MapPlutusData metadata) {
            MapPlutusData byAsset = new MapPlutusData();
            byAsset.put(BytesPlutusData.of(HexUtil.decodeHexString(assetNameHex)), metadata);
            MapPlutusData byPolicy = new MapPlutusData();
            byPolicy.put(BytesPlutusData.of(HexUtil.decodeHexString(policyId)), byAsset);
            MapPlutusData root = new MapPlutusData();
            root.put(BytesPlutusData.of("721"), byPolicy);
            return root;
        }

        @Test
        void shouldResolveVersion4NestedMapForReferenceNft() throws Exception {
            String datum = datumHex(nested(POLICY_ID, ASSET_NAME_HEX, metadata("Nested")), BigInteger.valueOf(4));

            assertThat(cip68FTDatumParser.parse(datum, REFERENCE_NFT)).hasValueSatisfying(m -> {
                assertThat(m.name()).isEqualTo("Nested");
                assertThat(m.description()).isEqualTo("Desc");
                assertThat(m.version()).isEqualTo(4L);
            });
        }

        @Test
        void shouldPickTheEntryForThisReferenceNftAmongSeveral() throws Exception {
            MapPlutusData root = nested(POLICY_ID, ASSET_NAME_HEX, metadata("Mine"));
            MapPlutusData byPolicy = (MapPlutusData) root.getMap().get(BytesPlutusData.of("721"));
            MapPlutusData byAsset = (MapPlutusData) byPolicy.getMap().get(BytesPlutusData.of(HexUtil.decodeHexString(POLICY_ID)));
            byAsset.put(BytesPlutusData.of("Other".getBytes()), metadata("Other"));

            assertThat(cip68FTDatumParser.parse(datumHex(root, BigInteger.valueOf(4)), REFERENCE_NFT))
                    .hasValueSatisfying(m -> assertThat(m.name()).isEqualTo("Mine"));
        }

        @Test
        void shouldReturnEmptyWhenNestedMapHasNoEntryForReferenceNft() throws Exception {
            String datum = datumHex(nested(OTHER_POLICY_ID, ASSET_NAME_HEX, metadata("Foreign")), BigInteger.valueOf(4));

            assertThat(cip68FTDatumParser.parse(datum, REFERENCE_NFT)).isEmpty();
        }

        @Test
        void shouldResolveSingleEntryWithoutAssetContext() throws Exception {
            String datum = datumHex(nested(POLICY_ID, ASSET_NAME_HEX, metadata("Only")), BigInteger.valueOf(4));

            assertThat(cip68FTDatumParser.parse(datum)).hasValueSatisfying(m -> assertThat(m.name()).isEqualTo("Only"));
        }

        @Test
        void shouldReturnEmptyForSeveralEntriesWithoutAssetContext() throws Exception {
            // Without the reference NFT the right entry can't be chosen, so the datum is not guessed at
            MapPlutusData root = nested(POLICY_ID, ASSET_NAME_HEX, metadata("Mine"));
            MapPlutusData byPolicy = (MapPlutusData) root.getMap().get(BytesPlutusData.of("721"));
            MapPlutusData byAsset = (MapPlutusData) byPolicy.getMap().get(BytesPlutusData.of(HexUtil.decodeHexString(POLICY_ID)));
            byAsset.put(BytesPlutusData.of("Other".getBytes()), metadata("Other"));

            assertThat(cip68FTDatumParser.parse(datumHex(root, BigInteger.valueOf(4)))).isEmpty();
        }

        @Test
        void shouldReadVersion4DatumDirectlyWhen721IsNotAMap() throws Exception {
            MapPlutusData properties = metadata("Direct");
            properties.put(BytesPlutusData.of("721"), BytesPlutusData.of("x"));

            assertThat(cip68FTDatumParser.parse(datumHex(properties, BigInteger.valueOf(4)), REFERENCE_NFT))
                    .hasValueSatisfying(m -> assertThat(m.name()).isEqualTo("Direct"));
        }

        @Test
        void shouldReadVersion4DatumDirectlyWithoutA721Key() throws Exception {
            // Version 4 allows the nested layout but doesn't require it: a flat v4 datum is valid
            assertThat(cip68FTDatumParser.parse(datumHex(metadata("Flat"), BigInteger.valueOf(4)), REFERENCE_NFT))
                    .hasValueSatisfying(m -> assertThat(m.name()).isEqualTo("Flat"));
        }

        @Test
        void shouldReadVersion3DatumDirectlyEvenWithA721Key() throws Exception {
            // Before version 4 a "721" key is just an additional property, not a wrapper
            MapPlutusData properties = metadata("Direct");
            properties.put(BytesPlutusData.of("721"), BytesPlutusData.of("x"));

            assertThat(cip68FTDatumParser.parse(datumHex(properties, BigInteger.valueOf(3)), REFERENCE_NFT))
                    .hasValueSatisfying(m -> assertThat(m.name()).isEqualTo("Direct"));
        }
    }

    /**
     * Datums with a Plutus constructor as an additional property. In yaci-store these were dropped with a
     * NullPointerException (bloxbean/yaci-store#1159, #1209). This parser reads only the typed FT fields, so
     * it never hit that bug; these guard that it keeps reading such datums.
     */
    @Nested
    class ConstructorPropertyValues {

        /** Preprod NFT from bloxbean/yaci-store#1159: {@code contractData} is a Plutus constructor. */
        private static final String NFT_WITH_CONSTRUCTOR_PROPERTY =
                "d87982a3446e616d65464e465420233145696d616765404c636f6e747261637444617461d879860181581cdc9acfee35"
                + "243d123e8f10bc58692a6bc5aa3135c7eafc2aac9daafcd87a80581c9abc17656a6d1c24688292777c18c1ce599845a5"
                + "88f4d893c1884da2d87a80d87a8001";

        /** Preprod fungible token (Wrapped pUSDC): {@code seed} is a constructor, {@code oracles} are key hashes. */
        private static final String FT_WITH_CONSTRUCTOR_PROPERTY =
                "d8799fae46737570706c791a1a449c8b44747970654c5772617070656441737365744576656e75654845746865726575"
                + "6d46706f6c696379582a3078413062383639393163363231386233366331643139443461326539456230634533363036"
                + "65423438476163636f756e74582a30783732423530393631423237343734624433363241436346393638616630316633"
                + "6338323863336432467469636b6572457055534443446e616d654d577261707065642070555344434b64657363726970"
                + "74696f6e582057726170706564207055534443206f70657261746564206279205042472e696f48646563696d616c7306"
                + "4375726c4e68747470733a2f2f7062672e696f446c6f676f582868747470733a2f2f70726570726f642e706267746f6b"
                + "656e2e696f2f7277612d6c6f676f2e706e674671756f72756d02476f7261636c65739f581c80edfa909a3d40a54fca4c"
                + "3ee852c7ba2a79391738911dc363580dc2581cab25d3b9476a3e3343a2f353b08b40913c573de7d286ef37ac4013e058"
                + "1c7bd1ebc8230f961193fb772204542e85425af4f7a8f36acb5543da08ff4473656564d8799fd8799f582042f5390b27"
                + "9a4b49d56fe594b2d5eaf02e8e387fa1612f87bafd2feed7c836afff02ff01d87980ff";

        /** Mainnet "PBG Token Voucher" datums whose {@code owner} is a constructor, keyed by hex with the expected name. */
        private static final Map<String, String> MAINNET_VOUCHERS_WITH_CONSTRUCTOR_OWNER = Map.of(
                "d8799fa9456f776e6572d8799fd8799f581c3e8242c26c999d22ecfc760cdc7a34508637c4351b3187ee20eb77ebffd8"
                + "7a80ff45646174756d0046746f6b656e731a001cf2c046706572696f64034570726963659f1b0000023fff1df4ed1b00"
                + "000005242abee5ff446e616d655450424720546f6b656e20566f75636865722038394b6465736372697074696f6e5821"
                + "5375636365737320666565207265696d62757273656d656e7420766f75636865724375726c5768747470733a2f2f7062"
                + "672e696f2f766f75636865727345696d616765582568747470733a2f2f746f6b656e2e7062672e696f2f766f75636865"
                + "722d6c6f676f2e706e6701d87980ff",
                "PBG Token Voucher 89",
                "d8799fa9456f776e6572d8799fd8799f581c90c98fa2c409dcaf2cec7770e8a614df2322fb08c545ddae0b285209ffd8"
                + "799fd8799fd8799f581cebffd86359482e97a1d7755cdae86739ffcafa6033713b735c4f4031ffffffff45646174756d"
                + "0046746f6b656e731b00000001ea0659e846706572696f64014570726963659f1b00000001c09103241a04748ab2ff44"
                + "6e616d655350424720546f6b656e20566f756368657220394b6465736372697074696f6e582153756363657373206665"
                + "65207265696d62757273656d656e7420766f75636865724375726c5768747470733a2f2f7062672e696f2f766f756368"
                + "65727345696d616765582568747470733a2f2f746f6b656e2e7062672e696f2f766f75636865722d6c6f676f2e706e67"
                + "01d87980ff",
                "PBG Token Voucher 9",
                "d8799fa9456f776e6572d8799fd8799f581ce29e15bf7d0deaabdb963a1f9ffc9adc661dbf6889ae73a17ae39843ffd8"
                + "799fd8799fd8799f581c8b4e2900167bf48e98803bb0ed0cd7c2f6b699a08716e8a520b507f9ffffffff45646174756d"
                + "0046746f6b656e731a00b1505246706572696f64044570726963659f1b0000029ff9d4b0d31b00000005d2138378ff44"
                + "6e616d655550424720546f6b656e20566f7563686572203132384b6465736372697074696f6e58215375636365737320"
                + "666565207265696d62757273656d656e7420766f75636865724375726c5768747470733a2f2f7062672e696f2f766f75"
                + "636865727345696d616765582568747470733a2f2f746f6b656e2e7062672e696f2f766f75636865722d6c6f676f2e70"
                + "6e6701d87980ff",
                "PBG Token Voucher 128");

        @Test
        void shouldKeepMetadataWhenAPropertyIsAConstructor() {
            assertThat(cip68FTDatumParser.parse(NFT_WITH_CONSTRUCTOR_PROPERTY))
                    .hasValueSatisfying(m -> assertThat(m.name()).isEqualTo("NFT #1"));
        }

        @Test
        void shouldKeepFungibleTokenMetadataWhenAPropertyIsAConstructor() {
            assertThat(cip68FTDatumParser.parse(FT_WITH_CONSTRUCTOR_PROPERTY)).hasValueSatisfying(m -> {
                assertThat(m.name()).isEqualTo("Wrapped pUSDC");
                assertThat(m.description()).isEqualTo("Wrapped pUSDC operated by PBG.io");
                assertThat(m.ticker()).isEqualTo("pUSDC");
                assertThat(m.decimals()).isEqualTo(6L);
                assertThat(m.url()).isEqualTo("https://pbg.io");
                assertThat(m.logo()).isEqualTo("https://preprod.pbgtoken.io/rwa-logo.png");
            });
        }

        @Test
        void shouldKeepMetadataOfMainnetDatumsWithAConstructorOwner() {
            MAINNET_VOUCHERS_WITH_CONSTRUCTOR_OWNER.forEach((datum, name) ->
                    assertThat(cip68FTDatumParser.parse(datum)).as(name).hasValueSatisfying(m -> {
                        assertThat(m.name()).isEqualTo(name);
                        assertThat(m.description()).isEqualTo("Success fee reimbursement voucher");
                        assertThat(m.url()).isEqualTo("https://pbg.io/vouchers");
                        assertThat(m.version()).isEqualTo(1L);
                    }));
        }
    }

    /**
     * CIP-68 defines versions 1 to 4. A datum with another version is not indexed, and one warning names the token and
     * the version. Within the range, the layout (flat or nested) is decided by the {@code "721"} key, as the CIP's
     * retrieval steps say, not by the version.
     */
    @Nested
    class DatumLayoutAndVersion {

        private static final String POLICY = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
        private static final String BASE = HexUtil.encodeHexString("Token".getBytes());
        private static final AssetType REFERENCE_NFT = new AssetType(POLICY, "000643b0" + BASE);

        /** Mainnet: Greenland Reserve Coin, a live fungible token whose datum declares version 0. */
        private static final String GNRC_POLICY = "67cee89d59ab5354ee22c8af0638224126aecc6210f9372a61f13a64";
        private static final String GNRC_REFERENCE_NFT = "000643b0474e5243";
        private static final String GNRC_VERSION_0_DATUM =
                "d8799fa6446e616d6556477265656e6c616e64205265736572766520436f696e4b6465736372697074696f6e583c4173"
                + "736574206261636b656420746f6b656e207365637572656420627920477265656e6c616e642072756269657320616e64"
                + "20736170706869726573467469636b657244474e52434375726c582368747470733a2f2f7777772e7468652d6d696e74"
                + "2e636f6d2f636f6d706c69616e6365446c6f676f4048646563696d616c730600d866821a951b3c2b9f81581cc0bb241d"
                + "37ffbdfdbb07d3d34bff54671c00935128da06966bc033810103ffff";

        /** Mainnet: HOSKY 10K NFT 0002, first datum, version 100 (later replaced by a version 1 datum). */
        private static final String HOSKY_POLICY = "df9337b73a041b1c45015e4b08ee1fed9a8e2a5b2a25d73c6fc2a35b";
        private static final String HOSKY_REFERENCE_NFT = "000643b0484f534b592054656e4b2030303032";
        private static final String HOSKY_VERSION_100_DATUM =
                "d8799fa4446e616d6552484f534b592031304b204e4654203030303245696d6167655835697066733a2f2f516d665276"
                + "58375a41334673436a6f58575966425847534846573641525961617578364754335774714c455836674b646573637269"
                + "7074696f6e5825546869732069732061207265666572656e636520746f6b656e20666f72204349502d36382e46747261"
                + "697473a14474797065497265666572656e6365186480ff";

        /** {"721": {policy: {base name: metadata}}} for REFERENCE_NFT. */
        private static MapPlutusData nested(MapPlutusData metadata) {
            MapPlutusData byAsset = new MapPlutusData();
            byAsset.put(BytesPlutusData.of(HexUtil.decodeHexString(BASE)), metadata);
            MapPlutusData byPolicy = new MapPlutusData();
            byPolicy.put(BytesPlutusData.of(HexUtil.decodeHexString(POLICY)), byAsset);
            MapPlutusData root = new MapPlutusData();
            root.put(BytesPlutusData.of("721"), byPolicy);
            return root;
        }

        @Test
        void readsEveryDefinedVersionWithoutAWarning() throws Exception {
            for (long version = 1; version <= 4; version++) {
                assertThat(cip68FTDatumParser.parse(datumHex(metadata("Token"), BigInteger.valueOf(version)), REFERENCE_NFT))
                        .as("version %d", version)
                        .hasValueSatisfying(m -> assertThat(m.name()).isEqualTo("Token"));
            }
            assertThat(warnings()).isEmpty();
        }

        @Test
        void rejectsAnUndefinedVersionAndWarnsOnceNamingTheToken() throws Exception {
            for (long version : new long[]{0, 5, 100, -1, Long.MAX_VALUE}) {
                logs.list.clear();

                assertThat(cip68FTDatumParser.parse(datumHex(metadata("Token"), BigInteger.valueOf(version)), REFERENCE_NFT))
                        .as("version %d", version).isEmpty();
                assertThat(warnings()).as("version %d", version).singleElement().satisfies(w -> assertThat(w)
                        .contains("Skipping").contains("version " + version).contains(POLICY)
                        .contains(REFERENCE_NFT.assetName()).contains("(1 to 4)"));
            }
        }

        @Test
        void warnsWithoutATokenWhenThereIsNoReferenceNft() throws Exception {
            assertThat(cip68FTDatumParser.parse(datumHex(metadata("Token"), BigInteger.valueOf(7)))).isEmpty();

            assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("Skipping").contains("version 7"));
        }

        @Test
        void rejectsTheRealMainnetVersion0FungibleToken() {
            AssetType referenceNft = new AssetType(GNRC_POLICY, GNRC_REFERENCE_NFT);

            assertThat(cip68FTDatumParser.parse(GNRC_VERSION_0_DATUM, referenceNft)).isEmpty();
            assertThat(warnings()).singleElement().satisfies(w -> assertThat(w)
                    .contains("version 0").contains(GNRC_POLICY).contains(GNRC_REFERENCE_NFT));
        }

        @Test
        void rejectsTheRealMainnetVersion100Datum() {
            AssetType referenceNft = new AssetType(HOSKY_POLICY, HOSKY_REFERENCE_NFT);

            assertThat(cip68FTDatumParser.parse(HOSKY_VERSION_100_DATUM, referenceNft)).isEmpty();
            assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("version 100").contains(HOSKY_POLICY));
        }

        @Test
        void readsANestedDatumAsNestedWhateverItsDefinedVersion() throws Exception {
            for (long version = 1; version <= 4; version++) {
                String datum = datumHex(nested(metadata("Nested")), BigInteger.valueOf(version));

                assertThat(cip68FTDatumParser.hasNestedMetadata(datum)).as("version %d", version).isTrue();
                assertThat(cip68FTDatumParser.parse(datum, REFERENCE_NFT)).as("version %d", version)
                        .hasValueSatisfying(m -> assertThat(m.name()).isEqualTo("Nested"));
            }
        }

        @Test
        void readsADatumWithoutTheNestedKeyAsFlatWhateverItsVersion() throws Exception {
            for (long version : new long[]{3, 4}) {
                String datum = datumHex(metadata("Flat"), BigInteger.valueOf(version));

                assertThat(cip68FTDatumParser.hasNestedMetadata(datum)).as("version %d", version).isFalse();
                assertThat(cip68FTDatumParser.parse(datum, REFERENCE_NFT)).as("version %d", version)
                        .hasValueSatisfying(m -> assertThat(m.name()).isEqualTo("Flat"));
            }
        }

        @Test
        void isNotNestedForAnythingThatIsNotADecodableDatum() {
            assertThat(cip68FTDatumParser.hasNestedMetadata(null)).isFalse();
            assertThat(cip68FTDatumParser.hasNestedMetadata(" ")).isFalse();
            assertThat(cip68FTDatumParser.hasNestedMetadata("not-hex")).isFalse();
            assertThat(cip68FTDatumParser.hasNestedMetadata("81".repeat(16_000) + "00")).isFalse();
        }
    }

    /**
     * CIP-68 text is UTF-8. Bytes that are not valid UTF-8 are stored as hex instead of being decoded with replacement
     * characters, which would lose the original bytes; chunks of a {@code logo} are joined as bytes before decoding.
     */
    @Nested
    class TextEncoding {

        private final byte[] invalidUtf8 = {(byte) 0xff, (byte) 0xfe, 0x41};

        @Test
        void storesBytesThatAreNotUtf8AsHexInEveryTextField() throws Exception {
            MapPlutusData properties = new MapPlutusData();
            for (String key : List.of("name", "description", "ticker", "url", "logo")) {
                properties.put(BytesPlutusData.of(key), BytesPlutusData.of(invalidUtf8));
            }

            assertThat(cip68FTDatumParser.parse(datumHex(properties, BigInteger.ONE))).hasValue(
                    new FungibleTokenMetadata(null, "fffe41", "fffe41", "fffe41", "fffe41", "fffe41", 1L));
        }

        @Test
        void keepsValidUtf8AsTextAndStripsNullCharacters() throws Exception {
            MapPlutusData properties = metadata("Caf\u00e9\u0000 \u20ac");

            assertThat(cip68FTDatumParser.parse(datumHex(properties, BigInteger.ONE)))
                    .hasValueSatisfying(m -> assertThat(m.name()).isEqualTo("Caf\u00e9 \u20ac"));
        }

        @Test
        void joinsLogoChunksAsBytesSoACharacterSplitAcrossChunksSurvives() throws Exception {
            // "\u20ac" is three bytes (e2 82 ac); the first chunk ends after its first byte
            byte[] text = "logo-\u20ac.png".getBytes(StandardCharsets.UTF_8);
            byte[] first = java.util.Arrays.copyOfRange(text, 0, 6);
            byte[] second = java.util.Arrays.copyOfRange(text, 6, text.length);
            MapPlutusData properties = metadata("Token");
            properties.put(BytesPlutusData.of("logo"), ListPlutusData.of(BytesPlutusData.of(first), BytesPlutusData.of(second)));

            assertThat(cip68FTDatumParser.parse(datumHex(properties, BigInteger.ONE)))
                    .hasValueSatisfying(m -> assertThat(m.logo()).isEqualTo("logo-\u20ac.png"));
        }

        @Test
        void keepsAnEmptyLogoByteStringAsEmpty() throws Exception {
            MapPlutusData properties = metadata("Token");
            properties.put(BytesPlutusData.of("logo"), BytesPlutusData.of(new byte[0]));

            assertThat(cip68FTDatumParser.parse(datumHex(properties, BigInteger.ONE)))
                    .hasValueSatisfying(m -> assertThat(m.logo()).isEmpty());
        }
    }

    /** {@code logo} is capped at 64 KiB, the CIP-26 logo limit, measured on the joined bytes. */
    @Nested
    class LogoSizeCap {

        private static final int MAX = 64 * 1024;

        private Optional<FungibleTokenMetadata> parseWithLogo(PlutusData logo) throws Exception {
            MapPlutusData properties = metadata("Token");
            properties.put(BytesPlutusData.of("logo"), logo);
            return cip68FTDatumParser.parse(datumHex(properties, BigInteger.ONE));
        }

        private ListPlutusData chunks(int totalBytes) {
            ListPlutusData list = new ListPlutusData();
            for (int remaining = totalBytes; remaining > 0; remaining -= 64) {
                list.add(BytesPlutusData.of("a".repeat(Math.min(64, remaining)).getBytes(StandardCharsets.US_ASCII)));
            }
            return list;
        }

        @Test
        void keepsALogoAtTheLimit() throws Exception {
            assertThat(parseWithLogo(chunks(MAX))).hasValueSatisfying(m -> assertThat(m.logo()).hasSize(MAX));
            assertThat(warnings()).isEmpty();
        }

        @Test
        void dropsALogoOverTheLimitButKeepsTheMetadata() throws Exception {
            assertThat(parseWithLogo(chunks(MAX + 1))).hasValueSatisfying(m -> {
                assertThat(m.logo()).isNull();
                assertThat(m.name()).isEqualTo("Token");
            });
            assertThat(warnings()).singleElement().satisfies(w -> assertThat(w).contains("logo").contains(String.valueOf(MAX + 1)));
        }

        @Test
        void appliesTheLimitToASingleByteStringToo() throws Exception {
            assertThat(parseWithLogo(BytesPlutusData.of(new byte[MAX + 1])))
                    .hasValueSatisfying(m -> assertThat(m.logo()).isNull());
        }
    }

}
