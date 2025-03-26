package org.cardanofoundation.tokenmetadata.registry.api.service;

import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
class Cip68FTDatumParserTest {

    private final Cip68FTDatumParser cip68FTDatumParser = new Cip68FTDatumParser();

    @Test
    public void parseFLDTDatumTest() {

        var fldtCip68Datum = "d8799fa648646563696d616c73064b6465736372697074696f6e5f5840546865206f6666696369616c20746f6b656e206f6620466c756964546f6b656e732c2061206c656164696e6720446546692065636f73797374656d206675656c5827656420627920696e6e6f766174696f6e20616e6420636f6d6d756e697479206261636b696e672eff446c6f676f582068747470733a2f2f666c756964746f6b656e732e636f6d2f666c64742e706e67446e616d6544464c4454467469636b657244464c44544777656273697465581868747470733a2f2f666c756964746f6b656e732e636f6d2f0101ff";

        var tokenMetadataOpt = cip68FTDatumParser.parse(fldtCip68Datum);

        if (tokenMetadataOpt.isEmpty()) {
            Assertions.fail();
        }

        var tokenMetadata = tokenMetadataOpt.get();

        Assertions.assertEquals(new FungibleTokenMetadata(6L,
                        "The official token of FluidTokens, a leading DeFi ecosystem fueled by innovation and community backing.",
                        "https://fluidtokens.com/fldt.png",
                        "FLDT",
                        "FLDT",
                        null,
                        "https://fluidtokens.com/",
                        1L),
                tokenMetadata);


    }

    @Test
    public void parseUSDMDatumTest() {

        var fldtCip68Datum = "d8799fa7446e616d65445553444d4b6465736372697074696f6e5837466961742d6261636b656420737461626c65636f696e206e617469766520746f207468652043617264616e6f20626c6f636b636861696e467469636b6572445553444d4375726c5168747470733a2f2f6d6568656e2e696f2f446c6f676f5835697066733a2f2f516d5078596570454648747533474252754b3652684c35774b72536d7867596a624575384341644677344467687148646563696d616c7306456c6567616c582868747470733a2f2f6d6568656e2e696f2f6d6568656e5f7465726d735f6f665f736572766963652f01ff";

        var tokenMetadataOpt = cip68FTDatumParser.parse(fldtCip68Datum);

        if (tokenMetadataOpt.isEmpty()) {
            Assertions.fail();
        }

        var tokenMetadata = tokenMetadataOpt.get();

        Assertions.assertEquals(new FungibleTokenMetadata(6L,
                        "Fiat-backed stablecoin native to the Cardano blockchain",
                        "ipfs://QmPxYepEFHtu3GBRuK6RhL5wKrSmxgYjbEu8CAdFw4Dghq",
                        "USDM",
                        "USDM",
                        "https://mehen.io/",
                        null,
                        1L),
                tokenMetadata);


    }


}