package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.ParsedCip68Datum;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.repository.MetadataReferenceNftRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigInteger;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

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
                new Cip68FungibleTokenService(mock(MetadataReferenceNftRepository.class));

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
    class InvalidReason {

        private final Cip68FungibleTokenService service =
                new Cip68FungibleTokenService(mock(MetadataReferenceNftRepository.class));

        private static ParsedCip68Datum fungibleToken(String name, String description, String logo) {
            return new ParsedCip68Datum(6L, description, logo, name, "TKN", null, 1L, null, null, false);
        }

        private static ParsedCip68Datum nft(String name, String description, String image) {
            return new ParsedCip68Datum(null, description, null, name, null, null, 1L, image, null, false);
        }

        @ParameterizedTest
        @ValueSource(ints = {222, 333, 444})
        void rejectsADatumWithoutANameForEveryLabel(int label) {
            assertThat(service.invalidReason(fungibleToken(null, "Desc", null), label)).hasValue("it has no name");
        }

        @Test
        void acceptsAFungibleTokenWithNameAndDescriptionAndNoLogo() {
            assertThat(service.invalidReason(fungibleToken("Token", "Desc", null), 333)).isEmpty();
        }

        @Test
        void rejectsAFungibleTokenWithoutDescription() {
            assertThat(service.invalidReason(fungibleToken("Token", null, null), 333))
                    .hasValueSatisfying(reason -> assertThat(reason).contains("no description"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"QmXyz", "iagon://abc", "logo.png"})
        void doesNotRejectAFungibleTokenForItsLogo(String logo) {
            // the logo is optional: a bad one is left out (see invalidLogoReason), the datum stays valid
            assertThat(service.invalidReason(fungibleToken("Token", "Desc", logo), 333)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(ints = {222, 444})
        void acceptsAnNftOrRftWithAnImageAndNoDescription(int label) {
            assertThat(service.invalidReason(nft("Token", null, "ipfs://bafy"), label)).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(ints = {222, 444})
        void rejectsAnNftOrRftWithoutImage(int label) {
            assertThat(service.invalidReason(nft("Token", "Desc", null), label))
                    .hasValueSatisfying(reason -> assertThat(reason).contains("no image"));
            assertThat(service.invalidReason(nft("Token", "Desc", ""), label))
                    .hasValueSatisfying(reason -> assertThat(reason).contains("no image"));
        }

        @ParameterizedTest
        @ValueSource(ints = {222, 444})
        void rejectsAnNftOrRftImageWithAnotherScheme(int label) {
            assertThat(service.invalidReason(nft("Token", "Desc", "iagon://abc"), label))
                    .hasValueSatisfying(reason -> assertThat(reason).contains("not a URI"));
        }

        @Test
        void abbreviatesALongImageInTheReason() {
            assertThat(service.invalidReason(nft("Token", "Desc", "x".repeat(500)), 222))
                    .hasValueSatisfying(reason -> assertThat(reason)
                            .contains("x".repeat(60) + "...").doesNotContain("x".repeat(61)));
        }
    }

    /** The optional {@code logo} of a fungible token: left out when it is not a URI with an allowed scheme. */
    @Nested
    class InvalidLogoReason {

        private final Cip68FungibleTokenService service =
                new Cip68FungibleTokenService(mock(MetadataReferenceNftRepository.class));

        private static ParsedCip68Datum withLogo(String logo) {
            return new ParsedCip68Datum(6L, "Desc", logo, "Token", "TKN", null, 1L, null, null, false);
        }

        @ParameterizedTest
        @ValueSource(strings = {"https://example.com/logo.png", "ipfs://bafkrei", "ar://abc",
                "data:image/png;base64,iVBORw0KGgo=", "IPFS://Qm123", "", " "})
        void keepsALogoWithAnAllowedSchemeOrEmpty(String logo) {
            assertThat(service.invalidLogoReason(withLogo(logo))).isEmpty();
        }

        @Test
        void keepsAMissingLogo() {
            assertThat(service.invalidLogoReason(withLogo(null))).isEmpty();
        }

        @ParameterizedTest
        @ValueSource(strings = {"QmXyz", "bafkreidrvp5q37eh", "iagon://abc", "http://example.com/logo.png",
                "ipfs:", "logo.png"})
        void leavesOutALogoWithAnotherForm(String logo) {
            assertThat(service.invalidLogoReason(withLogo(logo)))
                    .hasValueSatisfying(reason -> assertThat(reason).contains("not a URI").contains(logo));
        }

        @Test
        void abbreviatesALongLogoInTheReason() {
            assertThat(service.invalidLogoReason(withLogo("x".repeat(500))))
                    .hasValueSatisfying(reason -> assertThat(reason)
                            .contains("x".repeat(60) + "...").doesNotContain("x".repeat(61)));
        }
    }

}
