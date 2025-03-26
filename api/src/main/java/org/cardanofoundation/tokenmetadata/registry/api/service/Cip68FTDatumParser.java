package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.client.exception.CborDeserializationException;
import com.bloxbean.cardano.client.plutus.spec.*;
import com.bloxbean.cardano.client.util.HexUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
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
    private static final String WEBSITE = "website";
    private static final String URL = "url";

    public Optional<FungibleTokenMetadata> parse(String inlineDatum) {
        try {

            if (inlineDatum == null || inlineDatum.trim().isEmpty()) {
                return Optional.empty();
            }

            var cip68Data = PlutusData.deserialize(HexUtil.decodeHexString(inlineDatum));

            if (cip68Data instanceof ConstrPlutusData) {

                var data = ((ConstrPlutusData) cip68Data).getData().getPlutusDataList();

                if (data.size() < 2 || !(data.getFirst() instanceof MapPlutusData properties)) {
                    return Optional.empty();
                }

                var decimals = getNumericProperty(DECIMALS, properties);
                var description = getStringProperty(DESCRIPTION, properties);
                var logo = getStringProperty(LOGO, properties);
                var name = getStringProperty(NAME, properties);
                var ticker = getStringProperty(TICKER, properties);
                var website = getStringProperty(WEBSITE, properties);
                var url = getStringProperty(URL, properties);

                if (!(data.get(1) instanceof BigIntPlutusData version)) {
                    return Optional.empty();
                }

                return Optional.of(new FungibleTokenMetadata(decimals, description, logo, name, ticker, url, website, version.getValue().longValue()));

            }

            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error while processing {}", inlineDatum, e);
            try {
                log.warn("Datum: {}", OBJECT_MAPPER.writeValueAsString(PlutusData.deserialize(HexUtil.decodeHexString(inlineDatum))));
            } catch (JsonProcessingException | CborDeserializationException ex) {
                // do nothing
            }
            return Optional.empty();
        }

    }

    private String getStringProperty(String propertyName, MapPlutusData mapPlutusData) {
        var property = mapPlutusData.getMap().get(BytesPlutusData.of(propertyName));
        if (property instanceof BytesPlutusData) {
            return new String(((BytesPlutusData) property).getValue());
        } else {
            return null;
        }
    }

    private Long getNumericProperty(String propertyName, MapPlutusData mapPlutusData) {
        var property = mapPlutusData.getMap().get(BytesPlutusData.of(propertyName));
        if (property instanceof BigIntPlutusData) {
            return ((BigIntPlutusData) property).getValue().longValue();
        } else {
            return null;
        }
    }


}
