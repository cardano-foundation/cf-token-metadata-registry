package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.client.api.exception.ApiException;
import com.bloxbean.cardano.client.backend.blockfrost.common.Constants;
import com.bloxbean.cardano.client.backend.blockfrost.service.BFBackendService;
import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.util.HexUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

@Slf4j
public class Cip68DatumParserTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String DECIMALS = "decimals";
    private static final String DESCRIPTION = "description";
    private static final String LOGO = "logo";
    private static final String NAME = "name";
    private static final String TICKER = "ticker";
    private static final String WEBSITE = "website";

    @Test
    public void deserialiseDatum() throws CborDeserializationException, JsonProcessingException {

        var cip68Data = PlutusData.deserialize(HexUtil.decodeHexString("d8799fa648646563696d616c73064b6465736372697074696f6e5f5840546865206f6666696369616c20746f6b656e206f6620466c756964546f6b656e732c2061206c656164696e6720446546692065636f73797374656d206675656c5827656420627920696e6e6f766174696f6e20616e6420636f6d6d756e697479206261636b696e672eff446c6f676f582068747470733a2f2f666c756964746f6b656e732e636f6d2f666c64742e706e67446e616d6544464c4454467469636b657244464c44544777656273697465581868747470733a2f2f666c756964746f6b656e732e636f6d2f0101ff"));

        if (cip68Data instanceof ConstrPlutusData) {

            var data = ((ConstrPlutusData) cip68Data).getData().getPlutusDataList();
            var properties = ((MapPlutusData) data.get(0));

            var decimals = getNumericProperty(DECIMALS, properties);
            log.info("decimals: {}", decimals);

            var description = getStringProperty(DESCRIPTION, properties);
            log.info("description: {}", description);

            var logo = getStringProperty(LOGO, properties);
            log.info("logo: {}", logo);

            var name = getStringProperty(NAME, properties);
            log.info("name: {}", name);

            var ticker = getStringProperty(TICKER, properties);
            log.info("ticker: {}", ticker);

            var website = getStringProperty(WEBSITE, properties);
            log.info("website: {}", website);

        }

        var json = OBJECT_MAPPER.writeValueAsString(cip68Data);

        log.info("json: {}", json);


    }

    private String getStringProperty(String propertyName, MapPlutusData mapPlutusData) {
        var property = mapPlutusData.getMap().get(BytesPlutusData.of(propertyName));
        if (property != null) {
            return new String(((BytesPlutusData) property).getValue());
        } else {
            return null;
        }
    }

    private Long getNumericProperty(String propertyName, MapPlutusData mapPlutusData) {
        var property = mapPlutusData.getMap().get(BytesPlutusData.of(propertyName));
        if (property != null) {
            return ((BigIntPlutusData) property).getValue().longValue();
        } else {
            return null;
        }
    }


    @Test
    public void parseDatum() throws ApiException {

//        ac7bfd6e7d7639c4b282c9ad9faeb3a516a40245863c1e82d85f08707a1fa28d

        var bfBackendService = new BFBackendService(Constants.BLOCKFROST_MAINNET_URL, "mainnetKWaNkQcrF1erC3u3SZjaFxZiM2M20jFM");

        var utxoResult = bfBackendService.getUtxoService().getTxOutput("ac7bfd6e7d7639c4b282c9ad9faeb3a516a40245863c1e82d85f08707a1fa28d", 0).getValue();
        log.info("datum: {}", utxoResult.getInlineDatum());


    }


    @Test
    public void foo() throws JsonProcessingException {
        var data = ConstrPlutusData.of(0, BigIntPlutusData.of(BigInteger.ONE));
        var json = OBJECT_MAPPER.writeValueAsString(data);
        log.info("json: {}", json);
    }


}
