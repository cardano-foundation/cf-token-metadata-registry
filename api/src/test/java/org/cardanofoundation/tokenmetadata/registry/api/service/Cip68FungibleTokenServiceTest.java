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

    /** A subject gets the errors of its own label and those that do not depend on a label (ADR-019). */
    @Nested
    class ErrorsForLabel {

        private final ValidationError version = ValidationError.mandatory("CIP-68", null, "version", "undefined_version", "v0");
        private final ValidationError nftImage = ValidationError.mandatory("CIP-68", "222", "image", "missing_value", "no image");
        private final ValidationError ftDescription = ValidationError.mandatory("CIP-68", "333", "description", "missing_value", "no description");

        @Test
        void keepsTheErrorsOfTheLabelAndThoseOfNoLabel() {
            List<ValidationError> errors = List.of(version, nftImage, ftDescription);

            assertThat(Cip68FungibleTokenService.errorsForLabel(errors, 333)).containsExactly(version, ftDescription);
            assertThat(Cip68FungibleTokenService.errorsForLabel(errors, 444)).containsExactly(version);
        }

        @Test
        void leavesARowThatWasNeverCheckedUnchecked() {
            assertThat(Cip68FungibleTokenService.errorsForLabel(null, 333)).isNull();
        }

        @Test
        void servesAFungibleTokenPairedWithAnNftWithoutTheNftRequirement() throws Exception {
            MetadataReferenceNft row = MetadataReferenceNft.builder().policyId("aa".repeat(28)).assetName("000643b041").slot(1L)
                    .name("LP").description("Pool share").version(1L).datum("d87980")
                    .validationErrors(List.of(nftImage)).build();
            MetadataReferenceNftRepository repository = mock(MetadataReferenceNftRepository.class);
            when(repository.findFirstByPolicyIdAndAssetNameOrderBySlotDesc("aa".repeat(28), "000643b041")).thenReturn(Optional.of(row));

            Optional<FungibleTokenMetadata> served = new Cip68FungibleTokenService(repository, new Cip68FTDatumParser())
                    .findSubject("aa".repeat(28), "000643b041", List.of(), false);

            assertThat(served).hasValueSatisfying(metadata -> assertThat(metadata.validationErrors()).isEmpty());
        }
    }

}
