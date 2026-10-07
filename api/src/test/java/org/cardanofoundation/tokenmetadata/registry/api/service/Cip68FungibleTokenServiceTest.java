package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.cardanofoundation.tokenmetadata.registry.repository.MetadataReferenceNftRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

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

}
