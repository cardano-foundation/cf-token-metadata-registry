package org.cardanofoundation.tokenmetadata.registry.api.service;

import com.bloxbean.cardano.client.plutus.spec.PlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.model.onchain.Cip68Datum;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class Cip68DatumParser {

    private final ObjectMapper objectMapper;

    public Optional<Cip68Datum> parse(String inlineDatum) {
        try {

            PlutusData oracleUpdate = PlutusData.deserialize(HexUtil.decodeHexString(inlineDatum));
            var json = objectMapper.writeValueAsString(oracleUpdate);
            JsonNode jsonNode = objectMapper.readTree(json);


            return Optional.empty();
        } catch (Exception e) {
            log.warn("Error", e);
            return Optional.empty();
        }

    }


}
