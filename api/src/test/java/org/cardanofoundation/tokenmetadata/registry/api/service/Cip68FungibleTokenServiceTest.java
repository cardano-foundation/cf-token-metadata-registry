package org.cardanofoundation.tokenmetadata.registry.api.service;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.util.AssetType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
class Cip68FungibleTokenServiceTest {


    @Test
    public void convertAssetNameInReferenceNft() {
        var fltdUnit = "577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e0014df10464c4454";

        var assetType = AssetType.fromUnit(fltdUnit);
        var refNftAssetName = String.format("000643b0%s", assetType.assetName().substring(8));

//        000643b0
//        000643b0464c4454
//        0014df10464c4454

        var actualRefNftAssetType = new AssetType(assetType.policyId(), refNftAssetName);
        var fldtReferenceNftUnit = "577f0b1342f8f8f4aed3388b80a8535812950c7a892495c0ecdf0f1e000643b0464c4454";

        var expected = AssetType.fromUnit(fldtReferenceNftUnit);


        Assertions.assertEquals(actualRefNftAssetType, expected);
    }

}