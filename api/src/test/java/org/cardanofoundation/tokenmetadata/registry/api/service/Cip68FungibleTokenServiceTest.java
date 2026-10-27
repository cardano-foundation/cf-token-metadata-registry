package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.BigIntPlutusData;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.plutus.spec.MapPlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.cardanofoundation.tokenmetadata.registry.validation.FieldCategory;
import org.cardanofoundation.tokenmetadata.registry.validation.ValidationError;
import org.cardanofoundation.tokenmetadata.registry.entity.MetadataReferenceNft;
import org.cardanofoundation.tokenmetadata.registry.repository.MetadataReferenceNftRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Slf4j
class Cip68FungibleTokenServiceTest {


    @Test
    void convertAssetNameInReferenceNft() {
        String fltdUnit = "577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e0014df10464c4454";

        AssetType assetType = AssetType.fromUnit(fltdUnit);
        String refNftAssetName = String.format("000643b0%s", assetType.assetName().substring(8));

//        000643b0
//        000643b0464c4454
//        0014df10464c4454

        AssetType actualRefNftAssetType = new AssetType(assetType.policyId(), refNftAssetName);
        String fldtReferenceNftUnit = "577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e000643b0464c4454";

        AssetType expected = AssetType.fromUnit(fldtReferenceNftUnit);


        Assertions.assertEquals(actualRefNftAssetType, expected);
    }

    @Nested
    class ExtractReferenceNfts {

        private static final String POLICY = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";

        private final Cip68FungibleTokenService service =
                new Cip68FungibleTokenService(mock(MetadataReferenceNftRepository.class), new Cip68FTDatumParser());

        private static Amt amount(String unit, long quantity) {
            return Amt.builder().unit(unit).quantity(BigInteger.valueOf(quantity)).build();
        }

        @Test
        void returnsEveryReferenceNftOfTheOutputInOrder() {
            AddressUtxo output = AddressUtxo.builder().amounts(List.of(
                    amount("lovelace", 2_000_000),
                    amount(POLICY + "000643b041", 1),
                    amount(POLICY + "0014df1041", 1_000),
                    amount(POLICY + "000643b042", 1))).build();

            assertThat(service.extractReferenceNfts(output)).extracting(Amt::getUnit)
                    .containsExactly(POLICY + "000643b041", POLICY + "000643b042");
        }

        @Test
        void returnsNothingWhenTheOutputHasNoReferenceNft() {
            AddressUtxo output = AddressUtxo.builder().amounts(List.of(
                    amount("lovelace", 2_000_000), amount(POLICY + "000643b041", 2))).build();

            assertThat(service.extractReferenceNfts(output)).isEmpty();
        }
    }


    /** What CIP-68 requires of a datum for each user-token label. */
    @Nested
    class ReferenceNftSubject {

        private static final String POLICY = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
        private final Cip68FungibleTokenService service =
                new Cip68FungibleTokenService(mock(MetadataReferenceNftRepository.class), new Cip68FTDatumParser());

        @ParameterizedTest
        @ValueSource(strings = {"0014df10", "001bc280"})
        void mapsAFungibleOrRichFungibleTokenToItsReferenceNft(String label) {
            assertThat(service.getReferenceNftSubject(POLICY + label + "41"))
                    .contains(new AssetType(POLICY, "000643b041"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"000de14041", "000643b041", "0014df10", "001bc280", "41"})
        void doesNotMapAnNftAReferenceNftOrAnAssetWithoutBaseName(String assetName) {
            assertThat(service.getReferenceNftSubject(POLICY + assetName)).isEmpty();
        }

        @Test
        void tellsARichFungibleTokenApart() {
            assertThat(service.isRichFungibleToken(POLICY + "001bc28041")).isTrue();
            assertThat(service.isRichFungibleToken(POLICY + "0014df1041")).isFalse();
            assertThat(service.isRichFungibleToken(POLICY + "001bc280")).isFalse();
        }
    }

    /**
     * A rich fungible token (444) has no logo in CIP-68; its required image is served as the logo, read from the stored
     * datum.
     */
    @Nested
    class RichFungibleTokenMetadata {

        private static final String POLICY = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
        private static final String REFERENCE_NFT = "000643b041";
        private final MetadataReferenceNftRepository repository = mock(MetadataReferenceNftRepository.class);
        private final Cip68FungibleTokenService service = new Cip68FungibleTokenService(repository, new Cip68FTDatumParser());

        @Test
        void servesTheImageAsTheLogoOfARichFungibleToken() throws Exception {
            stored(row("ipfs://bafyimage"));

            assertThat(service.findSubject(POLICY, REFERENCE_NFT, List.of(), true)).hasValueSatisfying(metadata -> {
                assertThat(metadata.logo()).isEqualTo("ipfs://bafyimage");
                assertThat(metadata.name()).isEqualTo("Token");
                assertThat(metadata.description()).isNull();
            });
        }

        @Test
        void servesTheStoredLogoOfAFungibleToken() throws Exception {
            MetadataReferenceNft row = row("ipfs://bafyimage");
            row.setLogo("ipfs://bafylogo");
            stored(row);

            assertThat(service.findSubject(POLICY, REFERENCE_NFT, List.of(), false))
                    .hasValueSatisfying(metadata -> assertThat(metadata.logo()).isEqualTo("ipfs://bafylogo"));
        }

        @Test
        void servesAnImageWithAnotherSchemeAsWritten() throws Exception {
            // the image is served as written; the validation errors say it breaks CIP-68 (ADR-019)
            stored(row("QmBareCid"));

            assertThat(service.findSubject(POLICY, REFERENCE_NFT, List.of(), true))
                    .hasValueSatisfying(metadata -> assertThat(metadata.logo()).isEqualTo("QmBareCid"));
        }

        @Test
        void prefersTheStoredImageToTheDatum() throws Exception {
            MetadataReferenceNft row = row("ipfs://bafyfromdatum");
            row.setImage("ipfs://bafystored");
            stored(row);

            assertThat(service.findSubject(POLICY, REFERENCE_NFT, List.of(), true))
                    .hasValueSatisfying(metadata -> assertThat(metadata.logo()).isEqualTo("ipfs://bafystored"));
        }

        @Test
        void servesTheImageFromThePrefetchedRowsOfABatch() throws Exception {
            MetadataReferenceNft row = row("ipfs://bafyimage");

            assertThat(service.findSubject(POLICY, REFERENCE_NFT, List.of(), Map.of(POLICY + ":" + REFERENCE_NFT, row), true))
                    .hasValueSatisfying(metadata -> assertThat(metadata.logo()).isEqualTo("ipfs://bafyimage"));
        }

        private void stored(MetadataReferenceNft row) {
            when(repository.findFirstByPolicyIdAndAssetNameOrderBySlotDesc(POLICY, REFERENCE_NFT)).thenReturn(Optional.of(row));
        }

        private static MetadataReferenceNft row(String image) throws Exception {
            MapPlutusData metadata = new MapPlutusData();
            metadata.put(BytesPlutusData.of("name"), BytesPlutusData.of("Token"));
            metadata.put(BytesPlutusData.of("image"), BytesPlutusData.of(image));
            ConstrPlutusData datum = ConstrPlutusData.of(0, metadata, BigIntPlutusData.of(1));
            return MetadataReferenceNft.builder()
                    .policyId(POLICY)
                    .assetName(REFERENCE_NFT)
                    .slot(100L)
                    .name("Token")
                    .version(1L)
                    .datum(HexUtil.encodeHexString(CborSerializationUtil.serialize(datum.serialize())))
                    .build();
        }
    }

    /**
     * A subject is served with what is wrong with the datum itself, stored when it was indexed, and what its own label
     * requires, checked when it is served: the label is often guessed when a datum is indexed without its user token,
     * and one reference NFT can serve a 333 and a 444 token (ADR-019).
     */
    @Nested
    class ErrorsOfTheSubjectsLabel {

        private static final String POLICY = "aa".repeat(28);
        private static final String REFERENCE_NFT = "000643b041";
        private final MetadataReferenceNftRepository repository = mock(MetadataReferenceNftRepository.class);
        private final Cip68FungibleTokenService service = new Cip68FungibleTokenService(repository, new Cip68FTDatumParser());

        private static MetadataReferenceNft.MetadataReferenceNftBuilder row() {
            return MetadataReferenceNft.builder().policyId(POLICY).assetName(REFERENCE_NFT).slot(1L).version(1L).datum("d87980");
        }

        private void stored(MetadataReferenceNft row) {
            when(repository.findFirstByPolicyIdAndAssetNameOrderBySlotDesc(POLICY, REFERENCE_NFT)).thenReturn(Optional.of(row));
        }

        @Test
        void checksAFungibleTokenUpdatedWithoutItsUserTokenAsAFungibleToken() {
            // name and image, no description, ticker, logo or decimals: an update without the user token was guessed as
            // an NFT, and the description a 333 requires was never checked
            stored(row().name("Coin").image("ipfs://QmCoin").validationErrors(List.of()).build());

            assertThat(service.findSubject(POLICY, REFERENCE_NFT, List.of(), false)).hasValueSatisfying(metadata -> {
                assertThat(metadata.logo()).isNull();
                assertThat(metadata.validationErrors())
                        .extracting(ValidationError::subStandard, ValidationError::fieldName, ValidationError::fieldCategory,
                                ValidationError::code)
                        .containsExactly(tuple("333", "description", FieldCategory.MANDATORY, "missing_value"));
            });
        }

        @Test
        void checksTheFungibleAndTheRichFungibleTokenOfOneReferenceNftEachForItsLabel() {
            MetadataReferenceNft both = row().name("Both").image("ipfs://QmBoth").validationErrors(List.of()).build();
            Map<String, MetadataReferenceNft> prefetched = Map.of(POLICY + ":" + REFERENCE_NFT, both);

            assertThat(service.findSubject(POLICY, REFERENCE_NFT, List.of(), prefetched, false)).hasValueSatisfying(metadata ->
                    assertThat(metadata.validationErrors()).extracting(ValidationError::subStandard).containsExactly("333"));
            assertThat(service.findSubject(POLICY, REFERENCE_NFT, List.of(), prefetched, true)).hasValueSatisfying(metadata -> {
                assertThat(metadata.logo()).isEqualTo("ipfs://QmBoth");
                assertThat(metadata.validationErrors()).isEmpty();
            });
        }

        @Test
        void servesTheStoredErrorsToEveryLabelWithTheErrorsOfTheLabelAfter() {
            ValidationError version = ValidationError.mandatory("CIP-68", null, "version", "invalid_version", "version 0");
            stored(row().name("Token").validationErrors(List.of(version)).build());

            assertThat(service.findSubject(POLICY, REFERENCE_NFT, List.of(), false)).hasValueSatisfying(metadata ->
                    assertThat(metadata.validationErrors()).extracting(ValidationError::subStandard, ValidationError::fieldName)
                            .containsExactly(tuple(null, "version"), tuple("333", "description")));
            assertThat(service.findSubject(POLICY, REFERENCE_NFT, List.of(), true)).hasValueSatisfying(metadata ->
                    assertThat(metadata.validationErrors()).extracting(ValidationError::subStandard, ValidationError::fieldName)
                            .containsExactly(tuple(null, "version"), tuple("444", "image")));
        }

        @Test
        void servesAFungibleTokenPairedWithAnNftWithoutTheNftRequirement() {
            stored(row().name("LP").description("Pool share").validationErrors(List.of()).build());

            assertThat(service.findSubject(POLICY, REFERENCE_NFT, List.of(), false))
                    .hasValueSatisfying(metadata -> assertThat(metadata.validationErrors()).isEmpty());
        }

        @Test
        void leavesARowThatWasNeverCheckedUnchecked() {
            stored(row().name("Old").build());

            assertThat(service.findSubject(POLICY, REFERENCE_NFT, List.of(), false))
                    .hasValueSatisfying(metadata -> assertThat(metadata.validationErrors()).isNull());
        }
    }

}
