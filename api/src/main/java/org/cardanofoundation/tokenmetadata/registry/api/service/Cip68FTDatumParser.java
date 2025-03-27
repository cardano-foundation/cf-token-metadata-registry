package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.util.HexUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.cip68.FungibleTokenMetadata;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class Cip68FTDatumParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String DECIMALS = "decimals";
    private static final String DESCRIPTION = "description";
    private static final String LOGO = "logo";
    private static final String NAME = "name";
    private static final String TICKER = "ticker";
    private static final String URL = "url";

    /**
     * Manually parses Cip68 Fungible Token Datum
     *
     * @param inlineDatum the hex encoded datum
     * @return the Cip68 Fungible Token Metadata
     */
    public Optional<FungibleTokenMetadata> parse(String inlineDatum) {

        if (inlineDatum == null || inlineDatum.trim().isEmpty()) {
            return Optional.empty();
        }

        try {

            var plutusData = PlutusData.deserialize(HexUtil.decodeHexString(inlineDatum));

            if (plutusData instanceof ConstrPlutusData cip68Data) {

                var dataList = cip68Data.getData().getPlutusDataList();

                if (dataList.size() < 2 || !(dataList.getFirst() instanceof MapPlutusData properties)) {
                    return Optional.empty();
                }

                var decimals = getNumericProperty(DECIMALS, properties);
                var description = getStringProperty(DESCRIPTION, properties);
                var logo = getStringProperty(LOGO, properties);
                var name = getStringProperty(NAME, properties);
                var ticker = getStringProperty(TICKER, properties);
                var url = getStringProperty(URL, properties);

                if (!(dataList.getFirst() instanceof BigIntPlutusData version)) {
                    return Optional.empty();
                }

                return Optional.of(new FungibleTokenMetadata(decimals, description, logo, name, ticker, url, version.getValue().longValue()));

            }

            return Optional.empty();
        } catch (Exception e) {
            log.warn("Unexpected error while parsing CIP FT Datum: {}", inlineDatum, e);
            return Optional.empty();
        }

    }

    private String getStringProperty(String propertyName, MapPlutusData mapPlutusData) {
        var property = mapPlutusData.getMap().get(BytesPlutusData.of(propertyName));
        if (property instanceof BytesPlutusData bytes) {
            return new String(bytes.getValue());
        } else {
            return null;
        }
    }

    private Long getNumericProperty(String propertyName, MapPlutusData mapPlutusData) {
        var property = mapPlutusData.getMap().get(BytesPlutusData.of(propertyName));
        if (property instanceof BigIntPlutusData bigInteger) {
            return bigInteger.getValue().longValue();
        } else {
            return null;
        }
    }


}
