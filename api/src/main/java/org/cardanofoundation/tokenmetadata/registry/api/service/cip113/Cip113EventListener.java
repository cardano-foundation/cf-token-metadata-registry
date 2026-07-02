package org.cardanofoundation.tokenmetadata.registry.api.service.cip113;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.events.RollbackEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.AddressUtxoEvent;
import com.bloxbean.cardano.yaci.store.utxo.domain.TxInputOutput;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.config.Cip113Configuration;
import org.cardanofoundation.tokenmetadata.registry.entity.Cip113RegistryNode;
import org.cardanofoundation.tokenmetadata.registry.repository.Cip113RegistryNodeRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class Cip113EventListener {

    private final Cip113Configuration cip113Configuration;
    private final Cip113RegistryNodeParser registryNodeParser;
    private final Cip113RegistryNodeRepository cip113RegistryNodeRepository;
    private final Cip113RegistryService cip113RegistryService;

    @EventListener
    @Transactional
    public void handleRollback(RollbackEvent rollbackEvent) {
        long rollbackSlot = rollbackEvent.getRollbackTo().getSlot();
        int count = cip113RegistryNodeRepository.deleteBySlotGreaterThan(rollbackSlot);
        log.info("CIP-113 rollback to slot {}: deleted {} registry node records", rollbackSlot, count);
    }

    @Transactional
    @EventListener
    public void processTransaction(AddressUtxoEvent addressUtxoEvent) {
        if (!cip113Configuration.isEnabled()) {
            return;
        }

        Long slot = addressUtxoEvent.getMetadata().getSlot();

        List<Cip113RegistryNode> entities = new ArrayList<>();

        for (TxInputOutput txInputOutput : addressUtxoEvent.getTxInputOutputs()) {
            for (AddressUtxo utxo : txInputOutput.getOutputs()) {
                if (utxo.getInlineDatum() != null && cip113RegistryService.containsRegistryNode(utxo)) {
                    parseEntity(utxo, slot).ifPresent(entities::add);
                }
            }
        }

        if (!entities.isEmpty()) {
            cip113RegistryNodeRepository.saveAll(entities);
            entities.forEach(entity -> log.info("Indexed CIP-113 registry node: key={}, slot={}, txHash={}",
                    entity.getKey(), entity.getSlot(), entity.getTxHash()));
        }
    }

    private Optional<Cip113RegistryNode> parseEntity(AddressUtxo utxo, Long slot) {
        return registryNodeParser.parse(utxo.getInlineDatum())
                .map(parsedRegistryNode -> Cip113RegistryNode.builder()
                        .key(parsedRegistryNode.key())
                        .slot(slot)
                        .txHash(utxo.getTxHash())
                        .transferLogicScript(parsedRegistryNode.transferLogicScript())
                        .thirdPartyTransferLogicScript(parsedRegistryNode.thirdPartyTransferLogicScript())
                        .globalStatePolicyId(parsedRegistryNode.globalStatePolicyId())
                        .next(parsedRegistryNode.next())
                        .datum(utxo.getInlineDatum())
                        .build());
    }

}
