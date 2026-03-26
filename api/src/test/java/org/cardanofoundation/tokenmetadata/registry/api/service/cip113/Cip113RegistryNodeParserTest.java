package org.cardanofoundation.tokenmetadata.registry.api.service.cip113;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class Cip113RegistryNodeParserTest {

    private final Cip113RegistryNodeParser parser = new Cip113RegistryNodeParser();

    @Test
    void parseRegistryNode() throws Exception {
        String inlineDatum = buildRegistryNodeDatum(
                "0befd1269cf3b5b41cce136c92c64b45dde93e4bfe11875839b713d1",
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "aaa513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126102",
                "def513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126103",
                "1234567890abcdef1234567890abcdef1234567890abcdef12345678"
        );

        Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(inlineDatum);

        assertTrue(result.isPresent());
        Cip113RegistryNodeParser.ParsedRegistryNode node = result.get();

        assertEquals("0befd1269cf3b5b41cce136c92c64b45dde93e4bfe11875839b713d1", node.key());
        assertEquals("ffffffffffffffffffffffffffffffffffffffffffffffffffffff", node.next());
        assertEquals("aaa513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126102", node.transferLogicScript());
        assertEquals("def513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126103", node.thirdPartyTransferLogicScript());
        assertEquals("1234567890abcdef1234567890abcdef1234567890abcdef12345678", node.globalStatePolicyId());
    }

    @Test
    void parseSentinelNode() throws Exception {
        String inlineDatum = buildRegistryNodeDatum(
                "",
                "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff",
                "aaa513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126102",
                "def513b0fcc01d635f8535d49f38acc33d4d6b62ee8732ca6e126103",
                ""
        );

        Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(inlineDatum);

        assertTrue(result.isPresent());
        Cip113RegistryNodeParser.ParsedRegistryNode node = result.get();

        assertEquals("", node.key());
        assertEquals("ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff", node.next());
    }

    @Test
    void parseNodeWithoutGlobalState() throws Exception {
        // 4 fields only, no globalStatePolicyId
        ConstrPlutusData registryNode = ConstrPlutusData.of(0,
                BytesPlutusData.of(HexUtil.decodeHexString("deadbeef")),
                BytesPlutusData.of(HexUtil.decodeHexString("cafebabe")),
                ConstrPlutusData.of(0, BytesPlutusData.of(HexUtil.decodeHexString("aabbccdd"))),
                ConstrPlutusData.of(0, BytesPlutusData.of(HexUtil.decodeHexString("11223344")))
        );
        String inlineDatum = HexUtil.encodeHexString(CborSerializationUtil.serialize(registryNode.serialize()));

        Optional<Cip113RegistryNodeParser.ParsedRegistryNode> result = parser.parse(inlineDatum);

        assertTrue(result.isPresent());
        assertEquals("deadbeef", result.get().key());
        assertEquals("cafebabe", result.get().next());
        assertEquals("aabbccdd", result.get().transferLogicScript());
        assertEquals("11223344", result.get().thirdPartyTransferLogicScript());
        assertNull(result.get().globalStatePolicyId());
    }

    @Test
    void parseInvalidDatumReturnsEmpty() {
        assertTrue(parser.parse("invalid_hex").isEmpty());
    }

    @Test
    void parseNullDatumReturnsEmpty() {
        assertTrue(parser.parse(null).isEmpty());
    }

    @Test
    void parseBlankDatumReturnsEmpty() {
        assertTrue(parser.parse("  ").isEmpty());
    }

    private static String buildRegistryNodeDatum(String key, String next,
                                                  String transferLogic, String thirdPartyLogic,
                                                  String globalState) throws Exception {
        ConstrPlutusData transferLogicConstr = ConstrPlutusData.of(0,
                BytesPlutusData.of(HexUtil.decodeHexString(transferLogic)));
        ConstrPlutusData thirdPartyConstr = ConstrPlutusData.of(0,
                BytesPlutusData.of(HexUtil.decodeHexString(thirdPartyLogic)));

        ConstrPlutusData registryNode = ConstrPlutusData.of(0,
                BytesPlutusData.of(key.isEmpty() ? new byte[0] : HexUtil.decodeHexString(key)),
                BytesPlutusData.of(HexUtil.decodeHexString(next)),
                transferLogicConstr,
                thirdPartyConstr,
                BytesPlutusData.of(globalState.isEmpty() ? new byte[0] : HexUtil.decodeHexString(globalState))
        );

        return HexUtil.encodeHexString(CborSerializationUtil.serialize(registryNode.serialize()));
    }

}
