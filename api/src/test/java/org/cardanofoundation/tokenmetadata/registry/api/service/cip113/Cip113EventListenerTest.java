package org.cardanofoundation.tokenmetadata.registry.api.service.cip113;

import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.plutus.spec.BytesPlutusData;
import com.bloxbean.cardano.client.plutus.spec.ConstrPlutusData;
import com.bloxbean.cardano.client.util.HexUtil;
import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.Amt;
import com.bloxbean.cardano.yaci.store.events.EventMetadata;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import org.cardanofoundation.tokenmetadata.registry.api.config.Cip113Configuration;
import org.cardanofoundation.tokenmetadata.registry.entity.Cip113RegistryNode;
import org.cardanofoundation.tokenmetadata.registry.repository.Cip113RegistryNodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class Cip113EventListenerTest {

    private static final String REGISTRY_NFT_POLICY_ID = "aabbccdd11223344aabbccdd11223344aabbccdd11223344aabbccdd";
    private static final String REGISTERED_POLICY_ID = "deadbeefcafebabedeadbeefcafebabedeadbeefcafebabedeadbeef";
    private static final String TRANSFER_LOGIC = "1111111111111111111111111111111111111111111111111111111111";
    private static final String THIRD_PARTY_LOGIC = "2222222222222222222222222222222222222222222222222222222222";
    private static final String TX_HASH = "abcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";

    @Mock
    private Cip113RegistryNodeRepository repository;

    private Cip113Configuration config;
    private Cip113RegistryNodeParser parser;
    private Cip113RegistryService registryService;
    private Cip113EventListener listener;

    @BeforeEach
    void setUp() {
        config = new Cip113Configuration();
        config.setEnabled(true);
        config.setRegistryNftPolicyIds(List.of(REGISTRY_NFT_POLICY_ID));
        config.init();

        parser = new Cip113RegistryNodeParser();
        registryService = new Cip113RegistryService(repository, config);
        listener = new Cip113EventListener(config, parser, repository, registryService);
    }

    @Test
    void processesValidRegistryNodeUtxo() throws Exception {
        String datum = buildRegistryNodeDatum(REGISTERED_POLICY_ID, "ffffffffffff",
                TRANSFER_LOGIC, THIRD_PARTY_LOGIC, "");

        AddressUtxoEvent event = buildEvent(100L, REGISTRY_NFT_POLICY_ID, REGISTERED_POLICY_ID, datum, TX_HASH);

        listener.processTransaction(event);

        ArgumentCaptor<Cip113RegistryNode> captor = ArgumentCaptor.forClass(Cip113RegistryNode.class);
        verify(repository).save(captor.capture());

        Cip113RegistryNode saved = captor.getValue();
        assertEquals(REGISTERED_POLICY_ID, saved.getPolicyId());
        assertEquals(100L, saved.getSlot());
        assertEquals(TX_HASH, saved.getTxHash());
        assertEquals(TRANSFER_LOGIC, saved.getTransferLogicScript());
        assertEquals(THIRD_PARTY_LOGIC, saved.getThirdPartyTransferLogicScript());
    }

    @Test
    void skipsWhenDisabled() {
        config.setEnabled(false);

        AddressUtxoEvent event = buildEvent(100L, REGISTRY_NFT_POLICY_ID, REGISTERED_POLICY_ID, "d8799f40ff", TX_HASH);

        listener.processTransaction(event);

        verifyNoInteractions(repository);
    }

    @Test
    void skipsWhenNoPolicyIdsConfigured() {
        config.setRegistryNftPolicyIds(List.of());
        config.init();

        AddressUtxoEvent event = buildEvent(100L, REGISTRY_NFT_POLICY_ID, REGISTERED_POLICY_ID, "d8799f40ff", TX_HASH);

        listener.processTransaction(event);

        verifyNoInteractions(repository);
    }

    @Test
    void skipsUtxoWithNonMatchingPolicyId() throws Exception {
        String datum = buildRegistryNodeDatum(REGISTERED_POLICY_ID, "ffffffffffff",
                TRANSFER_LOGIC, THIRD_PARTY_LOGIC, "");
        String otherPolicyId = "9999999999999999999999999999999999999999999999999999999999";

        AddressUtxoEvent event = buildEvent(100L, otherPolicyId, REGISTERED_POLICY_ID, datum, TX_HASH);

        listener.processTransaction(event);

        verifyNoInteractions(repository);
    }

    @Test
    void skipsUtxoWithoutInlineDatum() {
        AddressUtxo utxo = AddressUtxo.builder()
                .txHash(TX_HASH)
                .inlineDatum(null)
                .amounts(List.of(Amt.builder()
                        .unit(REGISTRY_NFT_POLICY_ID + REGISTERED_POLICY_ID)
                        .quantity(BigInteger.ONE)
                        .build()))
                .build();

        AddressUtxoEvent event = AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(100L).build())
                .txInputOutputs(List.of(TxInputOutput.builder().outputs(List.of(utxo)).build()))
                .build();

        listener.processTransaction(event);

        verifyNoInteractions(repository);
    }

    @Test
    void skipsUtxoWithInvalidDatum() {
        AddressUtxoEvent event = buildEvent(100L, REGISTRY_NFT_POLICY_ID, REGISTERED_POLICY_ID,
                "deadbeef", TX_HASH);

        listener.processTransaction(event);

        verifyNoInteractions(repository);
    }

    @Test
    void skipsNftWithQuantityGreaterThanOne() throws Exception {
        String datum = buildRegistryNodeDatum(REGISTERED_POLICY_ID, "ffffffffffff",
                TRANSFER_LOGIC, THIRD_PARTY_LOGIC, "");

        AddressUtxo utxo = AddressUtxo.builder()
                .txHash(TX_HASH)
                .inlineDatum(datum)
                .amounts(List.of(Amt.builder()
                        .unit(REGISTRY_NFT_POLICY_ID + REGISTERED_POLICY_ID)
                        .quantity(BigInteger.TWO)
                        .build()))
                .build();

        AddressUtxoEvent event = AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(100L).build())
                .txInputOutputs(List.of(TxInputOutput.builder().outputs(List.of(utxo)).build()))
                .build();

        listener.processTransaction(event);

        verifyNoInteractions(repository);
    }

    private AddressUtxoEvent buildEvent(long slot, String nftPolicyId, String nftAssetName,
                                         String inlineDatum, String txHash) {
        AddressUtxo utxo = AddressUtxo.builder()
                .txHash(txHash)
                .inlineDatum(inlineDatum)
                .amounts(List.of(Amt.builder()
                        .unit(nftPolicyId + nftAssetName)
                        .quantity(BigInteger.ONE)
                        .build()))
                .build();

        return AddressUtxoEvent.builder()
                .metadata(EventMetadata.builder().slot(slot).build())
                .txInputOutputs(List.of(TxInputOutput.builder().outputs(List.of(utxo)).build()))
                .build();
    }

    private static String buildRegistryNodeDatum(String key, String next,
                                                  String transferLogic, String thirdPartyLogic,
                                                  String globalState) throws Exception {
        ConstrPlutusData registryNode = ConstrPlutusData.of(0,
                BytesPlutusData.of(HexUtil.decodeHexString(key)),
                BytesPlutusData.of(HexUtil.decodeHexString(next)),
                ConstrPlutusData.of(0, BytesPlutusData.of(HexUtil.decodeHexString(transferLogic))),
                ConstrPlutusData.of(0, BytesPlutusData.of(HexUtil.decodeHexString(thirdPartyLogic))),
                BytesPlutusData.of(globalState.isEmpty() ? new byte[0] : HexUtil.decodeHexString(globalState))
        );
        return HexUtil.encodeHexString(CborSerializationUtil.serialize(registryNode.serialize()));
    }

}
